package nl.knaw.dans.dataverse.migration;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.dans.dataverse.migration.rulebasedrole.AuthenticatedUser;
import nl.knaw.dans.dataverse.migration.rulebasedrole.AuthenticatedUserLookup;
import nl.knaw.dans.dataverse.migration.rulebasedrole.DataverseServiceLocal;
import nl.knaw.dans.dataverse.migration.rulebasedrole.Roleassignment;
import nl.knaw.dans.dataverse.migration.rulebasedrole.RuleExecutionSet;
import nl.knaw.dans.dataverse.migration.rulebasedrole.RuleServiceLocal;
import nl.knaw.dans.dataverse.migration.rulebasedrole.ShibAttributesHandlerException;


/**
 * Created by Eko Indarto on 08/11/16.
 */
public class ShibAttributesHandler extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ShibAttributesHandler.class);
    private static final long serialVersionUID = 1L;
    
    @EJB
    RuleServiceLocal ruleService;

    @EJB
    DataverseServiceLocal dataverseService;
    
    public static final String SEND_MAIL_TO = System.getProperty("shibattributeshandler.sendmail.to");
    public static final String NEW_USER_DEFAULT_PASSWORD = System.getProperty("new.user.default.password");
    public static final String BUILTIN_USER_KEY = System.getProperty("BuiltinUsers.KEY");

    /**
     * Default constructor.
     */
    public ShibAttributesHandler() {}

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	LOG.info("\n\n--ShibAttributesHandler - GET --\n");
        ShibAttributes sa;
		try {
			sa = new ShibAttributes(request);
			LOG.info("Attribute - eppn: " + sa.getEppn() + "\tAttribute - givenName: " + sa.getGivenName() + "\tAttribute -entitlement: " + sa.getEntitlement() + "\tAttribute - mail: " + sa.getMail());
			eppnChecker(sa);
			response.sendRedirect("https://" + request.getServerName() + "/shib.xhtml");
		} catch (ShibAttributesHandlerException e) {
			LOG.error(e.getMessage());
			boolean sendOk = sendSystemEmail(SEND_MAIL_TO, "ERROR on eppnChecker" , e.getMessage());
			if (!sendOk)
                LOG.error("Send email is failed, see the log file.");
			response.sendRedirect("/error.html");
		}
    }

    private void eppnChecker(ShibAttributes sa) throws ShibAttributesHandlerException {
    	LOG.info("Entering eppnChecker...");
        //Check Whether the ePPN is registered as persistentid in the authenticateduserlookup table.
        AuthenticatedUserLookup al = dataverseService.findAuthenticatedUserLookupByEppn(sa.getEppn());
        
        if (al != null) {
        	//Existing users with correct ePPN
        	//do nothing.
        	LOG.info("DO NOTHING. " + sa.getSingleMail() + " is an existing user. ePPN (" + sa.getEppn() + ") "
        			+ "is used correctly. persistentid: " +al.getPersistentuserId() +". authenticateduser_id: " + al.getAuthenticateduserId());
        } else {
        	LOG.info("Shib mail value is not the same as ePPN value. ePPN: " + sa.getEppn() + "\tmail: " + sa.getMail());
        	//New user of existing user where email is used as persitentidentifier.
        	//Find user by email addres
        	AuthenticatedUser au = dataverseService.findUserIdByEmail( sa.getSingleMail());
        	if (au == null) {
        		LOG.info("Email is not found in the database. " + sa.getMail() + " is a new user.");
        		//New user
        		//this means, no registered user by the given email.
   	 			//case 1. From non-VU: do nothing (just redirect)
   	 			//case 2. From VU: Apply rules based on shibEntitlement
        		//For new users that come from VU, they will be automatically assigned their role based on defined rule(s).
        		LOG.info("Apply rules");
        		List<Roleassignment> ras = setUserRoleBasedOnEntitlement(sa);
        		if (ras  != null) {
        			String msg = "Org: " + sa.getOrgName() + "\nmail: " + sa.getMail() + "\nlastname: " + sa.getLastname() ;
        			for (Roleassignment r : ras) {
        				msg += "\nassigneeidentifierr: " + r.getAssigneeIdentifier() + "\tdefinitionpoint_id(dataverseid)" 
        						+ r.getDefinitionpointId() + "\trole_id" + r.getRoleId();
        			}
        			sendSystemEmail(SEND_MAIL_TO, "ShibAttributesHandler - ADD ROLE SUCCESS - " + sa.getEntitlement(), msg);
        		} else 
        			LOG.info("No rule match for entitlement: " + sa.getEntitlement() + "\tePPN: " + sa.getEppn());
        	} else {
        		//User is registered by email. Old users use email as persistentidentifier, some institutions have email address as ePPN, some not.
        		//fix it.
        		 LOG.info( sa.getSingleMail() + "(shibMail) is an existing user that has eppn: " + sa.getEppn() + " and email: " + au.getEmail() + ". UPDATE IT.");
        		 boolean updateOk = dataverseService.updateAuthenticatedUserLookupByUserId(sa.getEppn(), au.getId(), false);
        		 String msg = "Update authenticateduserlookup. ePPN: " + sa.getEppn() + "\tauthenticateduser_id: " + au.getId();
 				 LOG.info(msg);
 				 if (updateOk)
 					sendSystemEmail(SEND_MAIL_TO, "ShibAttributesHandler - UPDATE SUCCESS - " + sa.getEppn(), msg);    		 
        	}
        	
        } 
    }

    private List<Roleassignment> setUserRoleBasedOnEntitlement(ShibAttributes sa) throws ShibAttributesHandlerException  {
        if (sa.getEntitlement() != null && !sa.getEntitlement().isEmpty()) {
        	LOG.info("entering Rule execution set");
            RuleExecutionSet rs = new RuleExecutionSet(ruleService, dataverseService, sa);
            return rs.setUserRole();
          
        } else {
        	LOG.info("No entitlement is provided for eppn: " + sa.getEppn());
        	return null;
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    @Resource(name = "mail/notifyMailSession")
    private Session session;

    private boolean sendSystemEmail(String to, String subject, String messageText) {
        boolean sent = false;
        try {
            Message msg = new MimeMessage(session);

            InternetAddress systemAddress = new InternetAddress("info@dataverse.nl");

            msg.setFrom(systemAddress);
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            msg.setSubject(subject);
            msg.setText(messageText);
            Transport.send(msg);
            sent = true;

        } catch (AddressException ae) {
            LOG.warn(ae.getMessage());
        } catch (MessagingException me) {
            LOG.warn(me.getMessage());
        }
        return sent;
    }




}
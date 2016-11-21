package nl.knaw.dans.dataverse.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Optional;


/**
 * Created by Eko Indarto on 08/11/16.
 */
public class ShibAttributesHandler extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ShibAttributesHandler.class);
    private static final long serialVersionUID = 1L;
    private Connection conn;

    /**
     * Default constructor.
     */
    public ShibAttributesHandler() {
        try {
            InitialContext ctx = new javax.naming.InitialContext();
            DataSource ds = (javax.sql.DataSource) ctx.lookup("jdbc/VDCNetDS");
            conn = ds.getConnection();
        } catch (NamingException e) {
            LOG.error(e.getMessage());
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        }

    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Statement stmt = null;
        String shibEppn = (String) request.getAttribute("eppn");
        String givenName = (String) request.getAttribute("givenName");
        String shibEntitlement = (String) request.getAttribute("entitlement");
        String shibMail = ((String) request.getAttribute("mail")).split(";")[0];
        LOG.info("Attribute - eppn: " + shibEppn + "\tAttribute - givenName: " + givenName + "\tAttribute -entitlement: " + shibEntitlement + "\tAttribute - mail: " + shibMail);

        if (shibEppn != null && !shibEppn.isEmpty() && shibMail != null && !shibMail.isEmpty()) {
            eppnChecker(stmt, shibEppn, shibMail);
        } else {
            LOG.error("FATAL ERROR, eppn and email are required!");
            response.sendRedirect("/error.html");
        }

        response.sendRedirect("https://" + request.getServerName() + "/shib.xhtml");
    }

    private void eppnChecker(Statement stmt, String shibEppn, String shibMail) {
        try {
            stmt = conn.createStatement();
            String authenticatedUserIdQuery = "select persistentuserid, authenticateduser_id from authenticateduserlookup where authenticationproviderid='shib' and persistentuserid ='https://engine.surfconext.nl/authentication/idp/metadata|"
                    + shibEppn + "'";

            LOG.info("Query to check whether the eppn is correctly used: " + authenticatedUserIdQuery);
            boolean eppnIsCorrectlyUsed = isEppnCorrectlyUsed(stmt, authenticatedUserIdQuery);

            if (eppnIsCorrectlyUsed) {
                // Do nothing since the eppn is already correctly used.
                LOG.info(shibMail + " is an existing user. EPPN (" + shibEppn + ") is used correctly. DO NOTHING");
            } else {
                // eppn isn't in the authenticateduserlookup table
                // There are 2 possibilities:
                // 1. an new user or
                // 2. the eppn is not the same as mail
                String sql = "select id, email, useridentifier from authenticateduser where email in ('"
                        + shibMail + "', '" + shibMail.toLowerCase() + "')";

                int id = 0;
                String email = "";
                boolean existingUser = false;

                LOG.info("Check the authenticateduser table. Query: " + sql);
                ResultSet resultSetOfAuthenticateduserQuery = stmt.executeQuery(sql);
                while (resultSetOfAuthenticateduserQuery.next()) {
                    id = resultSetOfAuthenticateduserQuery.getInt("id");
                    email = resultSetOfAuthenticateduserQuery.getString("email");
                    String useridentifier = resultSetOfAuthenticateduserQuery.getString("useridentifier");
                    LOG.info("Query result. id: " + id + "\temail: " + email + "\tuseridentifier: " + useridentifier);
                    existingUser = true;
                }

                if (!existingUser) {
                    // A user with the shibMail is not found, so that is a new
                    // user. Do nothing.
                    LOG.info(shibMail + " is a new user that has eppn: " + shibEppn + ". DO NOTHING");
                } else {
                    updateEppnOfOldUser(stmt, shibEppn, shibMail, authenticatedUserIdQuery, id, email);
                }

            }

        } catch (SQLException ex) {
            LOG.error(ex.getMessage());
        } finally {
            if (stmt != null)
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOG.error(e.getMessage());
                }

        }
    }

    private void updateEppnOfOldUser(Statement stmt, String shibEppn, String shibMail, String authenticatedUserIdQuery, int id, String email) throws SQLException {
        String fixAuthenticateduserlookupWithEppn = "";
        //first check whether the user is already in authenticateduserlookup table
        boolean userExistInAuthencticatedlookup = findUserInAuthenticateduserlookupById(stmt, id);

        if (userExistInAuthencticatedlookup) {
            //Update it
            LOG.info(shibMail + " is an existing user that has eppn: " + shibEppn + " and email: " + email + ". UPDATE IT.");
            fixAuthenticateduserlookupWithEppn = "UPDATE authenticateduserlookup SET persistentuserid ='https://engine.surfconext.nl/authentication/idp/metadata|"
                    + shibEppn + "' WHERE authenticateduser_id =" + id;

        } else {
            //Insert it.
            LOG.info(shibMail + " is an existing user that has eppn: " + shibEppn + " and email: " + email + ". INSERT IT.");
            fixAuthenticateduserlookupWithEppn = "INSERT INTO authenticateduserlookup (persistentuserid, authenticateduser_id) values='https://engine.surfconext.nl/authentication/idp/metadata|"
                    + shibEppn + "'," + id + ")";
        }
        LOG.info("updateEppnOfOldUser query: " + fixAuthenticateduserlookupWithEppn);
        stmt.execute(fixAuthenticateduserlookupWithEppn);

        //Now, check whether the updated is successfully (and it also to get time to the dataverse ? )
        ResultSet resultSetOfAuthenticateduserlookupCheckQuery = stmt.executeQuery(authenticatedUserIdQuery);
        String mailMsg = "shib mail: " + shibMail + "\nshibEppn: " + shibEppn + "\nauthenticateduser -email: " + email + "\nauthenticateduser -id: " + id;
        int numberOfRow = 0;
        while (resultSetOfAuthenticateduserlookupCheckQuery.next()) {
            numberOfRow++;
        }
        if (numberOfRow == 0) {
            //Updated is not success.
            LOG.error("UPDATE ERROR -  authenticateduserlookup is NOT UPDATED.");
            mailMsg = "\nQuery (authenticatedUserIdQuery): " + authenticatedUserIdQuery + "\nQuery (authenticatedUserIdQuery): " + authenticatedUserIdQuery + "\n" + mailMsg;
            boolean sendOk = sendSystemEmail("eko.indarto@dans.knaw.nl", "ShibAttributesHandler -ERROR --  NO UPDATED ROW. ", mailMsg);
            if (!sendOk)
                LOG.error("Send email is failed, see the log file.");
        } else if (numberOfRow == 1) {
            LOG.info("UPDATE SUCCESSFUL for shib mail: " + shibMail + "\tshibEppn: " + shibEppn);
            boolean sendOk = sendSystemEmail("eko.indarto@dans.knaw.nl", "ShibAttributesHandler - " + shibEppn + " - Successfully Updated", mailMsg);
            if (!sendOk)
                LOG.error("Send email is failed, see the log file.");
        } else {
            LOG.error("UPDATE ERROR - The number of updated row MUST BE 1 but it is " + numberOfRow);
            mailMsg = "\nQuery (authenticatedUserIdQuery): " + authenticatedUserIdQuery + "\nQuery (authenticatedUserIdQuery): " + authenticatedUserIdQuery + "\n" + mailMsg;
            boolean sendOk = sendSystemEmail("eko.indarto@dans.knaw.nl", "ShibAttributesHandler -ERROR -- The number of updated row: " + numberOfRow, mailMsg);
            if (!sendOk)
                LOG.error("Send email is failed, see the log file.");
        }
    }

    private boolean findUserInAuthenticateduserlookupById(Statement stmt, int id) throws SQLException {
        String findAuthenticateduserlookupByIdQuery = "Select id from authenticateduserlookup where authenticateduser_id = " + id;
        LOG.info("Find user authenticatedlookup by Id, query: " + findAuthenticateduserlookupByIdQuery);
        ResultSet resultSetOfAuthenticateduserlookupCheckByid = stmt.executeQuery(findAuthenticateduserlookupByIdQuery);
        while (resultSetOfAuthenticateduserlookupCheckByid.next()) {
            return true;
        }
        return false;
    }

    private boolean isEppnCorrectlyUsed(Statement stmt, String authenticatedUserIdQuery) throws SQLException {
        ResultSet resultSetOfAuthenticateduserlookupQuery = stmt.executeQuery(authenticatedUserIdQuery);
        while (resultSetOfAuthenticateduserlookupQuery.next()) {
            String persistentuserid = resultSetOfAuthenticateduserlookupQuery.getString("persistentuserid");
            int authenticateduser_id = resultSetOfAuthenticateduserlookupQuery.getInt("authenticateduser_id");
            LOG.info("persistentuserid: " + persistentuserid + "\tauthenticateduser_id" + authenticateduser_id);
            return true;
        }
        return false;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        doGet(request, response);
    }


    @Resource(name = "mail/notifyMailSession")
    private Session session;

    public boolean sendSystemEmail(String to, String subject, String messageText) {
        boolean sent = false;
        try {
            Message msg = new MimeMessage(session);

            InternetAddress systemAddress = new InternetAddress("eko.indarto@dans.knaw.nl");

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
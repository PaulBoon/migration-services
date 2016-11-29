package nl.knaw.dans.dataverse.migration;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.dans.dataverse.migration.rulebasedrole.ShibAttributesHandlerException;

/**
 * Created by Eko Indarto on 22/11/16.
 */
public class ShibAttributes {
	private static final Logger LOG = LoggerFactory.getLogger(ShibAttributes.class);
    private  HttpServletRequest request;
    private String eppn;
    private String givenName;
    private String lastname;
    private String mail;
    private String singleMail;
    private String entitlement;
    private String schacHomeOrganization;

    public ShibAttributes(HttpServletRequest request) throws ShibAttributesHandlerException {
        this.request = request;
        this.eppn = getAttributeValue("eppn");
        this.mail = getAttributeValue("mail");
        if (!this.allRequiredFieldExists())
        	throw new ShibAttributesHandlerException("ePPN and mail are required fields. ePPN: " + this.eppn + "\tmail: " + this.mail);
        this.lastname = getAttributeValue("sn");
        this.givenName = getAttributeValue("givenName");
        this.singleMail = mail.split(";")[0];//In the old Dataverse, we took always the first email
        this.entitlement = getAttributeValue("entitlement");
        this.schacHomeOrganization = getAttributeValue("schacHomeOrganization");
    }

    public String getEppn() {
        return eppn;
    }

    public String getGivenName() {
    	if (givenName.isEmpty())
    		return lastname.substring(0,1);
    	
        return givenName;
    }
    
    public String getLastname() {
        return lastname;
    }

    public String getEntitlement() {
        return entitlement;
    }

    public String getMail() {
        return mail;
    }
    
    public String getSingleMail() {
    	return singleMail;
    }

    public String getOrgName() {
        return schacHomeOrganization;
    }

    public boolean allRequiredFieldExists() {

        return (eppn != null && !eppn.isEmpty() && mail !=null && !mail.isEmpty());
    }

    public String getAttributeValue(String attributeName) {
    	Object o = request.getAttribute(attributeName);
    	if (o != null && o instanceof String)
    		return (String)o;
    	
    	LOG.warn("'" + attributeName + "' is null or not a string. So, fill in with empty string.");
        return "";
    }
}


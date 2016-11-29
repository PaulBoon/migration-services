/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.dataverse.migration.rulebasedrole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.List;

/**
 *
 * RuleServiceBean.java
 * 
 * @author Eko Indarto
*/
@Stateless
public class DataverseServiceBean implements DataverseServiceLocal {

	/**
	 *
	 */
	private static final long serialVersionUID = -3041567495764689270L;
    private static final Logger LOG = LoggerFactory.getLogger(DataverseServiceBean.class);

	@PersistenceContext(unitName = "VDCNet-ejbPU")
	private EntityManager em;

	/**
	 * Creates a new instance of UserServiceBean
	 */
	public DataverseServiceBean() {
	}

	@Override
	public AuthenticatedUser findUserIdByEppn(String eppn) throws ShibAttributesHandlerException {
        String query = "SELECT object(au) FROM authenticateduser au INNER JOIN authenticateduserlookup al ON al.authenticateduserId=au.id " +
                " WHERE al.authenticationproviderId='shib' and al.persistentuserId=:persistentuserId";
		String persistentuserid = "https://engine.surfconext.nl/authentication/idp/metadata|" + eppn;
		Query q = em.createQuery(query, AuthenticatedUserLookup.class)
				.setParameter("persistentuserId", persistentuserid);

        LOG.info("Find a user id by ePPN. ePPN: " + eppn + "\tQuery: " + query);
		List<AuthenticatedUser> auList = q.getResultList();
        if (auList != null && auList.size() == 1)
            return auList.get(0);

        throw new ShibAttributesHandlerException("");

	}
	
	@Override
	public AuthenticatedUser findUserIdByEmail(String email) {
		LOG.info("Entering findUserIdByEmail...");
        String query = "SELECT object(au) FROM authenticateduser au WHERE au.email=:email or au.email=:emailLC ";
		Query q = em.createQuery(query, AuthenticatedUser.class)
				.setParameter("email", email)
				.setParameter("emailLC", email.toLowerCase());

        LOG.info("Find a user id by email. email: " + email + "\tQuery: " + query);
		List<AuthenticatedUser> auList = q.getResultList();
        if (auList != null && auList.size() == 1)
            return auList.get(0);
        //Since email is unique in datatabe, we will get either 1 or null.
        return null;

	}

    @Override
    public long findDataverseIdByAlias(String alias) throws ShibAttributesHandlerException {
        String query = "select object(o) from dataverse o where o.alias=:alias";
        Query q = em.createQuery(query, Dataverse.class)
                .setParameter("alias", alias);
        LOG.info("Find dataverse id by dataverse alias. Dataverse Alias: " + alias + "\tQuery: " + query);

        List<Dataverse> dvList = q.getResultList();
        if (dvList != null && dvList.size() == 1)
            return dvList.get(0).getId();

        throw new ShibAttributesHandlerException("");

    }

    @Override
    public Roleassignment setRoleassignment(String assigneeIdentifier, long definitionpointId, long roleId) throws ShibAttributesHandlerException{
        Roleassignment r = new Roleassignment();
        r.setAssigneeIdentifier("@" + assigneeIdentifier);
        r.setDefinitionpointId(definitionpointId);
        r.setRoleId(roleId);

        LOG.info("Insert record to roleassignment table. Value of assigneeidentifier:  " + assigneeIdentifier
                + "\tvalue of definitionpoint_id: " + definitionpointId + "\tvalue of role_id: " + roleId);
       // em.getTransaction().begin();
        try {
        	em.persist(r);
        	LOG.info("inserting is done!");
        } catch (EntityExistsException | IllegalArgumentException  e) {
        	String msg= "Inserting of assigneeIdentifier" + assigneeIdentifier + "\tdefinitionpointId: "
        				+ definitionpointId + "\troleId: " + roleId + " is FAILED.";
        	LOG.error(msg);
        	throw new ShibAttributesHandlerException(msg + ". Cause: " + e.getMessage());
        }
        return r;
    }

    @Override
    public AuthenticatedUserLookup findAuthenticatedUserLookupByEppn(String eppn) {
        String persistentuserid = "https://engine.surfconext.nl/authentication/idp/metadata|" + eppn;
        String query = "SELECT object(al) FROM authenticateduserlookup al " +
                " WHERE al.authenticationproviderId='shib' and al.persistentuserId=:persistentuserId";
        Query q = em.createQuery(query, AuthenticatedUserLookup.class)
                .setParameter("persistentuserId", persistentuserid);
        LOG.info("Find AuthenticatedUserLookup by ePPN. ePPN: " + eppn + "\tQuery: " + query);

        List<AuthenticatedUserLookup> auList = q.getResultList();
        if (auList != null && auList.size() == 1)
            return auList.get(0);
        //No exception is needed since in the database the persistentuserid is unique. We will get either one record or nothing
        else
            return null;
    }

	@Override
	public boolean updateAuthenticatedUserLookupByUserId(String eppn, long authenticateduserId, boolean uppdateAuthenticationproviderid) throws ShibAttributesHandlerException {
		String persistentuserid = "https://engine.surfconext.nl/authentication/idp/metadata|" + eppn;
		String query;
		Query q;
		if (uppdateAuthenticationproviderid) {
			query = "UPDATE authenticateduserlookup SET persistentuserid= ?, authenticationproviderid='shib'" 
					+ " WHERE authenticateduser_id= ?";
			q = em.createNativeQuery(query)
					.setParameter(1, persistentuserid)
					.setParameter(2, authenticateduserId);
		} else {
			query = "UPDATE authenticateduserlookup SET persistentuserid= ?" 
					+ " WHERE authenticateduser_id= ?";
			q = em.createNativeQuery(query)
					.setParameter(1, persistentuserid)
					.setParameter(2, authenticateduserId);
		}
		LOG.info("Update authenticateduserlookup. persistentuserId: " + persistentuserid
				+ "\tauthenticateduserId: " + authenticateduserId + "\tQuery: " + query);
		int i = q.executeUpdate();
		LOG.info("Update executed, i= " + i);
		if (i != 1) {
			String msg= "UPDATE is FAILED. Please look to the authenticateduserlookup table. i: " + i + "\teppn" 
					+ eppn + "\tauthenticateduserId: " + authenticateduserId + ".\tQuery: " + query;
			LOG.error(msg);
			throw new ShibAttributesHandlerException(msg);
		}
		return true;
	}

	@Override
	public void deleteBuiltinuserByUsername(String username)  throws ShibAttributesHandlerException {
		String query = "DELETE FROM builtinuser WHERE username = ?";
		Query q = em.createNativeQuery(query)
				.setParameter(1, username);
		
		LOG.info("Delete from builtinuser where username " + username);
		int i = q.executeUpdate();
		LOG.info("Update executed, i= " + i + ". Query: " + query);
		if (i != 1) {
			String msg= "DELETE is FAILED. Please look to the builtinuser table. i: " + i + "\tusername" + username;
			LOG.error(msg);
			throw new ShibAttributesHandlerException(msg +  ". Query: " + query);
		}
	}
}

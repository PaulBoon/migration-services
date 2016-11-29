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

import nl.knaw.dans.dataverse.migration.ShibAttributes;
import nl.knaw.dans.dataverse.migration.ShibAttributesHandler;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * RuleExecutionSet.java
 * 
 * @author Eko Indarto
 *
 */
public class RuleExecutionSet {
	private final static Logger LOG = LoggerFactory.getLogger(RuleExecutionSet.class.getName());

	//private static final String DATAVERSE_API_AUTHENTICATED_USERS = "http://localhost:8080/api/admin/authenticatedUsers";
	private static final String DATAVERSE_API_CREATE_BUILTIN_USER = "http://localhost:8080/api/builtin-users";// Unfortunately no native API for creating shib user
	private ArrayList<Roleassignment> roleassignments;
	
	private RuleServiceLocal rs;
	private DataverseServiceLocal dsl;
	private ShibAttributes sa;

	public RuleExecutionSet(RuleServiceLocal rs, DataverseServiceLocal dsl, ShibAttributes sa) {
		this.rs = rs;
		this.sa = sa;
		this.dsl = dsl;
	}

	public List<Roleassignment> setUserRole() throws ShibAttributesHandlerException {
		LOG.info("Rule Checks");

		// Example from VU:
		// shibProps ("schacHomeOrganization","vu.nl"),
		// ("entitlement","urn:x-surfnet:dans.knaw.nl:dataversenl:role:dataset-creator")
		// the "vu.nl" as Rule name and "entitlement" is the RuleCondition
		List<Rule> ruleList = rs.findRulesByOrgName(sa.getOrgName());
		if (ruleList == null || ruleList.isEmpty()) {
			LOG.info("No rule is implemented for " + sa.getOrgName());
		} else {
			LOG.info("Search rule conditon for " + sa.getOrgName());
			List<Rule> searchedRules = getMatchedRuleCondition(ruleList);
			if (!searchedRules.isEmpty()) {
				// NOTE: We have to create user first before execute assignRule
				// method.
				// Create user.
				Map<String, Object> newUser = new LinkedHashMap<String, Object>();
				newUser.put("email", sa.getSingleMail());
				newUser.put("firstName", sa.getGivenName());
				newUser.put("lastName", sa.getLastname());
				newUser.put("affiliation", sa.getOrgName());
				// To avoid duplicate user name, user name is composed from
				// firstname.lastname.orgname
				newUser.put("userName", sa.getGivenName() + "." + sa.getLastname() + "." + sa.getOrgName());
				// System.out.println(json);
				Gson gson = new Gson();
				JsonObject jsonObject = gson.toJsonTree(newUser).getAsJsonObject();
				Client client = ClientBuilder.newClient();
				// Unfortunately no native API for creating shib user
				Response postResponse = client.target(DATAVERSE_API_CREATE_BUILTIN_USER)
						.path( ShibAttributesHandler.NEW_USER_DEFAULT_PASSWORD + "/" + ShibAttributesHandler.BUILTIN_USER_KEY)
						.request(MediaType.APPLICATION_JSON)
						.buildPost(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON)).invoke();
				LOG.info("postResponse.getStatus(): " + postResponse.getStatus() + "tpostResponse.getStatusInfo(): " + postResponse.getStatusInfo());
				if (postResponse.getStatus() == HttpURLConnection.HTTP_OK){
					String jsonResponse = postResponse.readEntity(String.class);
					LOG.info("New BUILTIN user is created" + jsonResponse);
					//There is  API to convert a builtin user to a shibuser, see edu.harvard.iq.dataverse.Dataverse.Admin.java
					//line 309: @Path("authenticatedUsers/convert/builtin2shib")
					//Unfortunately, it will create a random user with the given email address.
					/*
					From Admin.java. line 354 - 363
					Map<String, String> randomUser = shibService.getRandomUser();
//			        String eppn = UUID.randomUUID().toString().substring(0, 8);
			        String eppn = randomUser.get("eppn");
			        String idPEntityId = randomUser.get("idp");
			        String notUsed = null;
			        String separator = "|";
			        UserIdentifier newUserIdentifierInLookupTable = new UserIdentifier(idPEntityId + separator + eppn, notUsed);
			        String overwriteFirstName = randomUser.get("firstName");
			        String overwriteLastName = randomUser.get("lastName");
			        String overwriteEmail = randomUser.get("email");*/
					
					//So, we have to change it to SHIB user using our own code.
					JsonParser parser = new JsonParser();
					 JsonObject jjo = parser.parse(jsonResponse).getAsJsonObject();
					 long userId = jjo.getAsJsonObject("data").get("authenticatedUser").getAsJsonObject().get("id").getAsLong();
					 LOG.debug("Update authenticateduserlookup where userId: " + userId);
					 dsl.updateAuthenticatedUserLookupByUserId(sa.getEppn(), userId, true);
						 
					 LOG.info("Trying to delete builtinuser");
					 String username = jjo.getAsJsonObject("data").get("user").getAsJsonObject().get("userName").getAsString();
					 dsl.deleteBuiltinuserByUsername(username);
					
				} else {
					String msg = "ERROR: cannot create user. Response code: " + postResponse.getStatusInfo()
					+ " new user: " + newUser.values();
					LOG.error(msg);
					
					throw new ShibAttributesHandlerException(msg);
				}
				
				roleassignments = new ArrayList<Roleassignment>();

				for (Rule searchedRule : searchedRules) {
					LOG.info("Assign searched rule: " + searchedRule.getDescription());
					// To insert a Dataverse role to a user, the following
					// tables
					// are needed to be updated:
					// - roleassignment
					// - usernotification
					// - actionlogrecord (ideally)
					ArrayList<Roleassignment> ras = assignRule(searchedRule);
					roleassignments.addAll(ras);

					// save(user, rule);
					// so the current user can use his role immediately (without
					// first logout and then login again)
					// setRolesToCurrentUser(user, rule);
				}
			}

		}
		return roleassignments;

	}

	private ArrayList<Roleassignment> assignRule(Rule searchedRule) throws ShibAttributesHandlerException {
		ArrayList<Roleassignment> roleassignments = new ArrayList<Roleassignment>();
		// We need userid that can be retrieved from authenticateduserlooku
		// based on ePPN.
		AuthenticatedUser au = dsl.findUserIdByEppn(sa.getEppn());
		Collection<RuleGoal> rgList = searchedRule.getRuleGoal();
		for (RuleGoal rg : rgList) {
			String dataverseAlias = rg.getDataverseAlias();
			long dataverseId = dsl.findDataverseIdByAlias(dataverseAlias);

			// save to DB
			Roleassignment r = dsl.setRoleassignment(au.getUserIdentifier(), dataverseId, rg.getDataverseroleId());
			roleassignments.add(r);
			LOG.info("'" + rg.getDataverseroleId() + "' role is assigned to user '" + au.getUserIdentifier()
					+ "' for dvn alias '" + dataverseAlias + "'. Save it to the DB.");
		}
		return roleassignments;
	}
	/*
	 * Suppose, the RULE table has the following data: id organization
	 * description 1 vu.nl 2 vu.nl 3 dans 4 vu.nl 5 vu.nl
	 * 
	 * The RULE_CONDITION table has the following records: id ATTRIBUTE PATTERN
	 * RULE_ID 1 entitlement abc 1 2 affiliation employee 1 3 entitlement abc 2
	 * 4 entitlement zzz 5
	 * 
	 * Suppose, the "schacHomeOrganization" properties contains vu.nl so from
	 * the "setUserRole" method, we have a rulelist that contains 2 element
	 * namely {(1,vu.nl) has rule condition (1, entitlement,abc) and (2,
	 * affilition,employee), (2,vu.nl) has rule condition (3, entitlement,abc),
	 * (4,vu.nl) has no rule condition, (5,vu.nl) has rule condition (4,
	 * entitlement,zzz)}
	 * 
	 * Case 1. The request contains rule condition where entitlement attribute
	 * with value 'abc' and affiliation with value 'xxx'
	 * 
	 * Case 2. The request contains rule condition where entitlement with value
	 * 'abc'
	 * 
	 * Case 3. The request contains no rule condition
	 * 
	 * Case 4. The request contains rule condition where only entitlement with
	 * value 'zzz"
	 * 
	 */

	public List<Rule> getMatchedRuleCondition(List<Rule> ruleList) {
		List<Rule> searchedRules = new ArrayList<Rule>();
		for (Rule rule : ruleList) {
			Collection<RuleCondition> rcList = rule.getRuleCondition();
			if (rcList == null || rcList.isEmpty()) {
				// This is the case where a rule has no addition rule condition
				// (Case 3)
				// return rule; //leave this method.
				searchedRules.add(rule);
			} else {
				boolean matchedcondition = true;
				for (RuleCondition rc : rcList) {
					// Ex: rc.getAttributename() = entitlement (from the DB,
					// column
					// attribute_name)
					// rc.getPattern() =
					// urn:x-surfnet:dans.knaw.nl:dataversenl:role:dataset-creator
					// (from the DB, column pattern)
					Pattern pattern = Pattern.compile(rc.getPattern());
					String attrValFromShib = sa.getAttributeValue(rc.getAttributename());

					if ((attrValFromShib != null && !attrValFromShib.trim().equals(""))) {
						if (rc.getAttributename().equals("mail"))// We need this
																	// since we
																	// can get
																	// multiples
																	// mails.
							matchedcondition = mailIsMatched(pattern, attrValFromShib);
						else
							matchedcondition = pattern.matcher(attrValFromShib).matches();
					} else {
						matchedcondition = false;
					}
					if (!matchedcondition)
						break;
				}

				if (matchedcondition) {
					searchedRules.add(rule);
				}
			}
		}
		return searchedRules;
	}

	private boolean mailIsMatched(Pattern pattern, String attrValFromShib) {
		// Multiple emails where delimited by ;
		String mails[] = attrValFromShib.split(";");
		for (String mail : mails) {
			if (pattern.matcher(mail).matches()) {
				return true;
			}

		}
		return false;
	}

	
}

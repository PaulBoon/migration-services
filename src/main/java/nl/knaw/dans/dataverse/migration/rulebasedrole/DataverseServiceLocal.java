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

import javax.ejb.Local;
import java.math.BigInteger;
import java.util.List;

/**
 * RuleServiceLocal.java
 *
 * @author Eko Indarto
 */
@Local
public interface DataverseServiceLocal extends java.io.Serializable {
	public AuthenticatedUser findUserIdByEppn(String eppn) throws ShibAttributesHandlerException;
	
	public AuthenticatedUser findUserIdByEmail(String eppn);

	public long findDataverseIdByAlias(String alias) throws ShibAttributesHandlerException;

	public Roleassignment setRoleassignment(String assigneeIdentifier, long definitionpointId, long roleId) throws ShibAttributesHandlerException;

	public AuthenticatedUserLookup findAuthenticatedUserLookupByEppn(String eppn);
	
	public boolean updateAuthenticatedUserLookupByUserId(String eppn, long authenticateduserId, boolean uppdateAuthenticationproviderid) throws ShibAttributesHandlerException;
	
	public void deleteBuiltinuserByUsername(String username) throws ShibAttributesHandlerException;
}

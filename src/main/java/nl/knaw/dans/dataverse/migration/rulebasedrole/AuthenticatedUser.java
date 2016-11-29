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

import javax.persistence.*;

/**
 * Rule.java
 *
 * @author Eko Indarto
 */
@Entity(name = "authenticateduser")
public class AuthenticatedUser implements java.io.Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -3345859427256966914L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "useridentifier")
	private String userIdentifier;
	
	@Column(name = "email")
	private String email;


	/** Creates a new instance of AuthenticatedUserLookup */
	public AuthenticatedUser() {
	}

	public Long getId() { return id; }

	public void setId(Long id) { this.id = id; }

	public String getUserIdentifier() {
		return userIdentifier;
	}

	public void setUserIdentifier(String userIdentifier) {
		this.userIdentifier = userIdentifier;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}

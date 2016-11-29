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
 * Roleassignment.java
 *
 * @author Eko Indarto
 */
@Entity(name = "roleassignment")
public class Roleassignment implements java.io.Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -3345859427256966917L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "assigneeidentifier")
	private String assigneeIdentifier;

	@Column(name = "definitionpoint_id")
	private long definitionpointId;

	@Column(name = "role_id")
	private long roleId;
	/** Creates a new instance of Roleassignment */
	public Roleassignment() {
	}

	public String getAssigneeIdentifier() {
		return assigneeIdentifier;
	}

	public void setAssigneeIdentifier(String assigneeIdentifier) {
		this.assigneeIdentifier = assigneeIdentifier;
	}

	public long getDefinitionpointId() {
		return definitionpointId;
	}

	public void setDefinitionpointId(long definitionpointId) {
		this.definitionpointId = definitionpointId;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public Long getId() {
		return id;
	}



}

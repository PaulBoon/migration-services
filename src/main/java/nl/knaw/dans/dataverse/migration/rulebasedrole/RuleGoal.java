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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * RuleGoal.java
 * 
 * @author Eko Indarto
 */
@Entity(name = "_m_rule_goal")
public class RuleGoal implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3345859427259666912L;

	/**
	 * Holds value of property rule.
	 */
	@ManyToOne
	@JoinColumn(nullable = false)
	private Rule rule;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "dataverse_alias")
	private String dataverseAlias;

	@Column(name = "dataverserole_id")
	private int dataverseroleId;


	/** Creates a new instance of RuleGoal */
	public RuleGoal() {
	}


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDataverseAlias() {
		return dataverseAlias;
	}


	public int getDataverseroleId() {
		return dataverseroleId;
	}

	public Rule getRule() {
		return rule;
	}

	public int hashCode() {
		int hash = 0;
		hash += (this.id != null ? this.id.hashCode() : 0);
		return hash;
	}

	public boolean equals(Object object) {
		if (!(object instanceof RuleGoal)) {
			return false;
		}
		RuleGoal other = (RuleGoal) object;
		if (this.id != other.id
				&& (this.id == null || !this.id.equals(other.id)))
			return false;
		return true;
	}
}

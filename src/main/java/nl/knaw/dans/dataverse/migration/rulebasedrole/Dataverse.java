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
import java.math.BigInteger;

/**
 * Dataverse.java
 *
 * @author Eko Indarto
 */
@Entity(name = "dataverse")
public class Dataverse implements java.io.Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -3345859427256966913L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "alias")
	private String alias;

	/** Creates a new instance of AuthenticatedUserLookup */
	public Dataverse() {
	}

	public Long getId() {
		return id;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

}

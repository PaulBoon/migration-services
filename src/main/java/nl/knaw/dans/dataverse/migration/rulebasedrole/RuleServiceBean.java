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

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * RuleServiceBean.java
 * 
 * @author Eko Indarto
*/
@Stateless
public class RuleServiceBean implements RuleServiceLocal {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3041567495764689270L;
	@PersistenceContext(unitName = "VDCNet-ejbPU")
	private EntityManager em;

	/**
	 * Creates a new instance of UserServiceBean
	 */
	public RuleServiceBean() {
	}

	@Override
	public List<Rule> findAll() {
		return em.createQuery("select object(o) from Rule as o", Rule.class)
				.getResultList();
	}

	@Override
	public Rule findRuleById(Long id) {
		return em.createQuery(
				"select object(o) from Rule as o where o.id=:id", Rule.class)
				.setParameter("id", id)
				.getSingleResult();
	}

	@Override
	public Rule findRuleByCondition(RuleCondition rc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Rule> findRulesByOrgName(String orgName) {
		return em.createQuery(
				"select object(o) from _m_rule as o where o.orgName=:orgName", Rule.class)
				.setParameter("orgName", orgName)
                .getResultList();
	}

}

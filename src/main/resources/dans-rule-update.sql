-- Table: rule

-- DROP TABLE rule;

CREATE TABLE _m_rule
(
  id serial NOT NULL,
  org_name character varying(255),
  description character varying(255),
  CONSTRAINT _m_rule_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE _m_rule
  OWNER TO "dvnapp";
-- Table: _m_rule_condition

-- DROP TABLE _m_rule_condition;

CREATE TABLE _m_rule_condition
(
  id serial NOT NULL,
  attribute_name character varying(255),
  pattern character varying(255),
  rule_id bigint NOT NULL,
  CONSTRAINT rule_condition_pkey PRIMARY KEY (id),
  CONSTRAINT fk__m_rule_condition__m_rule_id FOREIGN KEY (rule_id)
      REFERENCES _m_rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE _m_rule_condition
  OWNER TO "dvnapp";
  
  
-- Table: _m_rulegoal

-- DROP TABLE _m_rule_goal;

CREATE TABLE _m_rule_goal
(
  id serial NOT NULL,
  rule_id bigint NOT NULL,
  dataverse_alias character varying(255),
  dataverserole_id bigint NOT NULL,
  CONSTRAINT _m_rule_goal_pkey PRIMARY KEY (id),
  CONSTRAINT fk__m_rule_goal_dataverserole_id FOREIGN KEY (dataverserole_id)
      REFERENCES dataverserole (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk__m_rule_goal_rule_id FOREIGN KEY (rule_id)
      REFERENCES _m_rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE _m_rule_goal
  OWNER TO "dvnapp";


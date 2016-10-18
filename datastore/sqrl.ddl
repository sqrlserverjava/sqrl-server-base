CREATE TABLE sqrl_identity (id BIGINT NOT NULL, idk VARCHAR(255) NOT NULL, native_user_xref VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE sqrl_correlator (id BIGINT NOT NULL, authenticationStatus VARCHAR(255) NOT NULL, expiryTime DATETIME NOT NULL, value VARCHAR(255) NOT NULL, authenticated_identity BIGINT, PRIMARY KEY (id));
CREATE TABLE sqrl_identity_flag (id BIGINT NOT NULL, value TINYINT(1) default 0 NOT NULL, name VARCHAR(255) NOT NULL);
CREATE TABLE sqrl_identity_data (id BIGINT NOT NULL, value VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL);
CREATE TABLE sqrl_used_nut_token (id BIGINT NOT NULL, value VARCHAR(255) NOT NULL);
CREATE TABLE sqrl_transient_auth_data (id BIGINT NOT NULL, value VARCHAR(2000) NOT NULL, name VARCHAR(255) NOT NULL);
-- INDEXES
ALTER TABLE `sqrl_identity` ADD INDEX(`native_user_xref`);
ALTER TABLE `sqrl_identity` ADD UNIQUE INDEX(`idk`);
ALTER TABLE `sqrl_correlator` ADD UNIQUE INDEX(`value`);
ALTER TABLE `sqrl_identity_flag` ADD INDEX(`name`);
ALTER TABLE `sqrl_identity_data` ADD INDEX(`name`);
ALTER TABLE `sqrl_used_nut_token` ADD UNIQUE INDEX(`value`);
ALTER TABLE `sqrl_transient_auth_data` ADD INDEX(`name`);
-- FOREIGN KEY
ALTER TABLE sqrl_correlator ADD CONSTRAINT FK_sqrl_correlator_authenticated_identity FOREIGN KEY (authenticated_identity) REFERENCES sqrl_identity (id);
ALTER TABLE sqrl_identity_flag ADD CONSTRAINT FK_sqrl_identity_flag_id FOREIGN KEY (id) REFERENCES sqrl_identity (id);
ALTER TABLE sqrl_identity_data ADD CONSTRAINT FK_sqrl_identity_data_id FOREIGN KEY (id) REFERENCES sqrl_identity (id);
ALTER TABLE sqrl_used_nut_token ADD CONSTRAINT FK_sqrl_used_nut_token_id FOREIGN KEY (id) REFERENCES sqrl_correlator (id);
ALTER TABLE sqrl_transient_auth_data ADD CONSTRAINT FK_sqrl_transient_auth_data_id FOREIGN KEY (id) REFERENCES sqrl_correlator (id);
-- ID GENERATOR TABLE
CREATE TABLE sqrl_db_id_gen (name VARCHAR(50) NOT NULL, value DECIMAL(38) NOT NULL, PRIMARY KEY (name));
INSERT INTO sqrl_db_id_gen(name, value) values ('identity_gen', 0);
INSERT INTO sqrl_db_id_gen(name, value) values ('correlator_gen', 0);
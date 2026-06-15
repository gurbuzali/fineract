-- Runs once when the MySQL data volume is first created (docker-entrypoint-initdb.d).
-- Creates the two databases Fineract's integration harness expects.
--
-- Both the bootstrap datasource (context.xml) and the default tenant row (list_db V1) connect as
-- root/mysql, so no extra user is strictly required. We additionally create mifos/mysql for parity
-- with the `setBlankPassword` gradle task and the dev launcher, in case a future run points the
-- tenant connection at it.

CREATE DATABASE IF NOT EXISTS `mifosplatform-tenants` CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE DATABASE IF NOT EXISTS `mifostenant-default`   CHARACTER SET utf8 COLLATE utf8_general_ci;

-- drizzle-jdbc:thin predates caching_sha2_password — force native password on both accounts.
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'mysql';

CREATE USER IF NOT EXISTS 'mifos'@'%' IDENTIFIED WITH mysql_native_password BY 'mysql';
GRANT ALL PRIVILEGES ON *.* TO 'mifos'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;

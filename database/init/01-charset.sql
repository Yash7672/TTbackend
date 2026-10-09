-- FIXORA — MySQL container bootstrap
--
-- The MySQL entrypoint already creates the database named by MYSQL_DATABASE and
-- grants MYSQL_USER access to it. This script only pins the character set so the
-- seeded catalogue (which contains non-ASCII demo text) is stored correctly.
--
-- NOTE: Fixora does NOT create tables here. Tables are created by Hibernate
-- (spring.jpa.hibernate.ddl-auto=update) and populated by the idempotent Java
-- seeder (com.fixora.config.SeedDataInitializer) on backend startup.

CREATE DATABASE IF NOT EXISTS fixora_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

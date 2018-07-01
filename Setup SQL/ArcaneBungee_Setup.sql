-- Run this file through MariaDB server on corredt database for initial setup

CREATE TABLE `ab_players` (
  `id`         INT AUTO_INCREMENT NOT NULL,
  `uuid`       VARCHAR(36)        NOT NULL,
  `name`       VARCHAR(36)        NOT NULL,
  `firstseen`  TIMESTAMP          NOT NULL,
  `lastseen`   TIMESTAMP          NOT NULL,
  `greylist`   BOOL               NOT NULL,
  `discord`    BOOL               NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8;
CREATE INDEX `ab_players_uuid` ON `ab_players` (`uuid`);
CREATE INDEX `ab_players_name` ON `ab_players` (`name`);

-- Provisioning Discord implementation
--CREATE TABLE `ab_discord` (
--  `id`         INT AUTO_INCREMENT NOT NULL,
--  `uuid`       VARCHAR(36)        NOT NULL,
--  `name`       VARCHAR(36)        NOT NULL,
--  `value`      BOOL               NOT NULL,
--  `server`     VARCHAR(36)        NOT NULL,
--  `world`      VARCHAR(36)        NOT NULL,
--  `expiry`     INT(11)            NOT NULL,
--  `contexts`   VARCHAR(200)       NOT NULL,
--  PRIMARY KEY (`id`)
--) DEFAULT CHARSET = utf8;
--CREATE INDEX `group_permissions_name` ON `{prefix}group_permissions` (`name`);
--CREATE INDEX `{prefix}group_permissions_permission` ON `{prefix}group_permissions` (`permission`);

CREATE TABLE `ab_events` (
  `id`         INT AUTO_INCREMENT NOT NULL,
  `name`       VARCHAR(36)        NOT NULL,
  `server`     VARCHAR(36)        NOT NULL,
  `begintime`  TIMESTAMP          NOT NULL,
  `endtime`    TIMESTAMP          NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8;
CREATE INDEX `ab_events_name` ON `ab_events` (`name`);

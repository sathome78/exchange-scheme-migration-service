DROP TABLE IF EXISTS `NEW_TABLE`;

CREATE TABLE `NEW_TABLE` (
  `id`          int(11)      NOT NULL AUTO_INCREMENT,
  `name`        varchar(255) NOT NULL,
  `title`       varchar(255) NOT NULL,
  `description` text         NOT NULL,
  `created`     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = latin1;
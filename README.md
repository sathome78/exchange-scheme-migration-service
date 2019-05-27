# scheme-migration-service

**`Environments:`**

- `up-migrate (develop):`

Process of migration to locally deployed database. Each time migrations roll data into the database schema (without process of cleaning).

**Property file:** application-up-migrate.properties

- `up-reset-migrate (develop):`

Process of migration to locally deployed database. Each time the database schema is cleaned and the migrations are re-rolled.

**Property file:** application-up-reset-migrate.properties

- `remote-migrate (only for production):`

Process of migration to remotely deployed database. Each time migrations roll data into the database schema (without process of cleaning).

**Property file:** application-remote-migrate.properties

# Database configurations
Pay attention, before run application with chosen profile, you have to create database locally (or choose remote database) and add datasource configurations to application property file
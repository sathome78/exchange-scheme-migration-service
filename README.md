# scheme-migration-service

**`Environments:`**

- `up-migrate (develop):`

Process of migration to database deployed in docker container. Each time migrations roll data into the database schema (without process of cleaning).

**Property file:** application-up-migrate.properties

**Docker compose file:** docker-compose.yml

- `up-reset-migrate (develop):`

Process of migration to database deployed in docker container. Each time the database schema is cleaned and the migrations are re-rolled.

**Property file:** application-up-reset-migrate.properties

**Docker compose file:** docker-compose.yml

- `remote-migrate (production):`

Process of migration to remotely deployed database. Each time migrations roll data into the database schema (without process of cleaning).

**Property file:** application-remote-migrate.properties

# Process of up and down docker containers
From project root directory invoke command
- to up container with database
`ENVIRONMENT=YOUR_ENVIRONMENT docker-compose up -d`
- to down container with database
`docker-compose down`
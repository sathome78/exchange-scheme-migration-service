package me.exrates.schememigrationservice.configurations;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.log4j.Log4j2;
import me.exrates.SSMGetter;
import me.exrates.schememigrationservice.enums.ProfilesEnum;
import me.exrates.schememigrationservice.utils.ScriptRunner;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Log4j2
@Configuration
public class FlywayConfiguration {

    @Value("${datasource.driver-class-name}")
    private String driverClassName;
    @Value("${datasource.jdbc-url}")
    private String jdbcUrl;
    @Value("${datasource.username}")
    private String user;
    @Value("${ssm.password-path}")
    private String ssmPath;

    @Autowired
    private Environment environment;
    @Autowired
    private SSMGetter ssmGetter;

    @Bean(name = "customDataSource")
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(ssmGetter.lookup(ssmPath));
        hikariConfig.setConnectionTimeout(30 * 1000);
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public Flyway flyway(@Qualifier("customDataSource") DataSource dataSource) {
        ClassicConfiguration configuration = new ClassicConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setBaselineOnMigrate(true);

        if (!ProfilesEnum.REMOTE_MIGRATE.getName().equals(environment.getActiveProfiles()[0])) {
            configuration.setCallbacks(new AddSchemaBeforeMigrateCallback());
        }

        Flyway flyway = new Flyway(configuration);
        flyway.migrate();
        log.info("Flyway -> migrate");
        return flyway;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    class AddSchemaBeforeMigrateCallback implements Callback {

        @Override
        public boolean supports(Event event, Context context) {
            return Event.BEFORE_MIGRATE == event;
        }

        @Override
        public boolean canHandleInTransaction(Event event, Context context) {
            return false;
        }

        @Override
        public void handle(Event event, Context context) {
            // Creating object of ScriptRunner class
            ScriptRunner scriptRunner = new ScriptRunner(context.getConnection(), false, true);

            String removeSchemaScriptFile = "db/init/remove-schema.sql";
            String addSchemaScriptFile = "db/init/add-schema.sql";
            String emptySchemaScriptFile = "db/init/add-schema.sql";

            final String activeProfile = environment.getActiveProfiles()[0];
            // Executing SQL Script
            try {
                if (ProfilesEnum.EMPTY_SCHEMA.getName().equals(activeProfile)) {

                    scriptRunner.runScript(emptySchemaScriptFile);
                } else {
                    if (ProfilesEnum.UP_RESET_MIGRATE.getName().equals(activeProfile)) {
                        scriptRunner.runScript(removeSchemaScriptFile);
                    }
                    scriptRunner.runScript(addSchemaScriptFile);
                }
            } catch (SQLException ex) {
                log.error("SQL exception", ex);
            } catch (IOException ex) {
                log.error("I/O exception", ex);
            }
            log.info("Flyway -> initialize schema from sql script");
        }
    }
}

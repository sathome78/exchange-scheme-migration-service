package me.exrates.schememigrationservice;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
public class FlywayConfiguration {

    @Bean
    @Profile({"local", "up-reset-migrate"})
    public Flyway flywayLocal(HikariDataSource dataSource) {
        ClassicConfiguration configuration = new ClassicConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setLocationsAsStrings("classpath:db/migration", "classpath:db/init");
        configuration.setCleanDisabled(false);
        configuration.setOutOfOrder(true);

        Flyway flyway = new Flyway(configuration);
        flyway.clean();
        flyway.migrate();
        return flyway;
    }

    @Bean
    @Profile({"remote-migrate", "up-migrate"})
    public Flyway flywayRemoteMigrate(HikariDataSource dataSource) {
        ClassicConfiguration configuration = new ClassicConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setLocationsAsStrings("classpath:db/migration");
        configuration.setCleanDisabled(true);
        configuration.setBaselineOnMigrate(true);
        configuration.setOutOfOrder(true);

        Flyway flyway = new Flyway(configuration);
        flyway.repair();
        flyway.migrate();
        return flyway;
    }

    @Bean
    public Flyway flyway() {
        return Flyway.configure().load();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
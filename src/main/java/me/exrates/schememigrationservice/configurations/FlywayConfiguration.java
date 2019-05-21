package me.exrates.schememigrationservice.configurations;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.exrates.SSMGetter;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import javax.sql.DataSource;

@Configuration
public class FlywayConfiguration {

    @Value("${datasource.driver-class-name}")
    private String driverClassName;
    @Value("${datasource.jdbc-url}")
    private String jdbcUrl;
    @Value("${datasource.username}")
    private String user;
    @Value("${ssm.path}")
    private String ssmPath;

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
    @Profile({"up-reset-migrate"})
    public Flyway flywayUpAndReset(@Qualifier("customDataSource") DataSource dataSource) {
        ClassicConfiguration configuration = new ClassicConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setCleanDisabled(false);

        Flyway flyway = new Flyway(configuration);
        flyway.migrate();
        return flyway;
    }

    @Bean
    @Profile({"remote-migrate", "up-migrate"})
    public Flyway flywayUp(@Qualifier("customDataSource") DataSource dataSource) {
        ClassicConfiguration configuration = new ClassicConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setCleanDisabled(true);
        configuration.setBaselineOnMigrate(true);

        Flyway flyway = new Flyway(configuration);
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
package me.exrates.schememigrationservice.configurations;

import lombok.extern.log4j.Log4j2;
import me.exrates.SSMGetter;
import me.exrates.SSMGetterImpl;
import me.exrates.schememigrationservice.enums.ProfilesEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Log4j2
@Configuration
public class SSMConfiguration {

    @Autowired
    private Environment environment;

    @Bean
    public SSMGetter ssmGetter() {
        final String activeProfile = environment.getActiveProfiles()[0];

        if (!activeProfile.equals(ProfilesEnum.REMOTE_MIGRATE.name())) {
            return new MockSSM();
        }
        return new SSMGetterImpl();
    }

    private class MockSSM implements SSMGetter {
        MockSSM() {
            log.info("Using mock ssm lookup...");
        }

        @Override
        public String lookup(String s) {
            return "root";
        }
    }
}
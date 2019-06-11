package me.exrates.schememigrationservice.enums;

import lombok.Getter;

@Getter
public enum ProfilesEnum {

    UP_MIGRATE("up-migrate"),
    UP_RESET_MIGRATE("up-reset-migrate"),
    REMOTE_MIGRATE("remote-migrate"),
    UP_TEST_MIGRATE("up-test-migrate");

    private String name;

    ProfilesEnum(String name) {
        this.name = name;
    }
}
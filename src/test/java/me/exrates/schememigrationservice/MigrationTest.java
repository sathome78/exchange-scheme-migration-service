package me.exrates.schememigrationservice;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MigrationTest {

    private static final Path RESOURCE_FOLDER = Paths.get("src/main/resources/");
    private static final Path HEAD_MIGRATION_FILE = RESOURCE_FOLDER.resolve("head-migration-protection.txt");
    private static final Path MIGRATION_FOLDER = RESOURCE_FOLDER.resolve("db/migration/");
    private static final String FLYWAY_PREFIX_REG_EXP = "[VRU]";
    private static final String PREFIX_SEPARATOR = "__";

    @Before
    public void check() {
        if (!Files.exists(HEAD_MIGRATION_FILE)) {
            throw new RuntimeException("unable to find file " + HEAD_MIGRATION_FILE.getFileName()
                    + " by path " + RESOURCE_FOLDER.toString() + " required for migration validation");
        }
    }

    @Test
    public void checkMigrationFileContainsLatestVersion() throws IOException {
        LinkedList<FileName> resFileNames = readSqlMigrationsFileNames();

        String migrationFile = FileUtils.readFile(HEAD_MIGRATION_FILE, Charset.defaultCharset());

        Collections.sort(resFileNames);
        String expected = resFileNames.getLast().name;

        Assert.assertEquals(expected, migrationFile);
    }

    @Test
    public void testForVersionDuplicates() {
        List<FileName> migrationFileNames = readSqlMigrationsFileNames();

        Map<String, List<String>> duplicates = getDuplicates(migrationFileNames);

        if (duplicates.size() > 0) {
            Assert.fail(errorMessage(duplicates));
        }
    }

    private LinkedList<FileName> readSqlMigrationsFileNames() {
        File[] files = MIGRATION_FOLDER.toFile().listFiles();

        if (files == null || files.length == 0) {
            throw new RuntimeException("unable to find migrations in " + MIGRATION_FOLDER.toString());
        }

        return Stream.of(files)
                .map(f -> parseFileName(f.getName()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Map<String, List<String>> getDuplicates(List<FileName> list) {
        Map<String, List<String>> duplicates = new HashMap<>();

        for (FileName fileNameParser : list) {
            if (Collections.frequency(list, fileNameParser) > 1) {
                List<String> result = duplicates.computeIfAbsent(fileNameParser.prefixWithVersion, v -> new ArrayList<>());
                result.add(fileNameParser.name);
            }
        }

        return duplicates;
    }

    private String errorMessage(Map<String, List<String>> duplicates) {
        StringBuilder msg = new StringBuilder(MIGRATION_FOLDER.toString() + " contains duplicates: \n");
        for (String version : duplicates.keySet()) {
            msg.append(version).append(" version in files:\n");
            msg.append(String.join("; ", duplicates.get(version)));
        }

        return msg.toString();
    }

    private FileName parseFileName(String name) {
        if (name == null) {
            throw new RuntimeException("File name can`t be empty");
        }

        String prefixWithVersion = name.substring(0, name.indexOf(PREFIX_SEPARATOR));
        Double version = Double.valueOf(prefixWithVersion.replaceAll(FLYWAY_PREFIX_REG_EXP, ""));
        return new FileName(name, prefixWithVersion, version);
    }

    private class FileName implements Comparable<FileName> {

        private final String name;
        private final String prefixWithVersion;
        private final Double version;

        FileName(String name, String prefixWithVersion, Double version) {
            this.name = name;
            this.prefixWithVersion = prefixWithVersion;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileName fileNameParser = (FileName) o;
            return Objects.equals(version, fileNameParser.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version);
        }

        @Override
        public int compareTo(FileName o) {
            return version.compareTo(o.version);
        }
    }
}
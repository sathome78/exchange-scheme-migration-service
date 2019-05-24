package me.exrates.schememigrationservice.utils;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ScriptRunner {

    private static final String DEFAULT_DELIMITER = ";";
    private static final String PL_SQL_BLOCK_SPLIT_DELIMITER = "+";
    private static final String PL_SQL_BLOCK_END_DELIMITER = "#";

    private final boolean autoCommit, stopOnError;
    private final Connection connection;
    private String delimiter = ScriptRunner.DEFAULT_DELIMITER;
    private final PrintWriter out, err;

    /* To Store any 'SELECT' queries output */
    private List<Table> tableList;

    /* To Store any SQL Queries output except 'SELECT' SQL */
    private List<String> sqlOutput;

    public ScriptRunner(final Connection connection, final boolean autoCommit, final boolean stopOnError) {
        if (connection == null) {
            throw new RuntimeException("ScriptRunner requires an SQL Connection");
        }

        this.connection = connection;
        this.autoCommit = autoCommit;
        this.stopOnError = stopOnError;
        this.out = new PrintWriter(System.out);
        this.err = new PrintWriter(System.err);

        tableList = new ArrayList<>();
        sqlOutput = new ArrayList<>();
    }

    public void runScript(final String scriptFile) throws SQLException, IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scriptFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final boolean originalAutoCommit = this.connection.getAutoCommit();
        try {
            if (originalAutoCommit != this.autoCommit) {
                this.connection.setAutoCommit(this.autoCommit);
            }
            this.runScript(this.connection, reader);
        } finally {
            this.connection.setAutoCommit(originalAutoCommit);
        }
    }

    private void runScript(final Connection conn, final Reader reader) throws SQLException, IOException {
        StringBuffer command = null;

        Table table = null;
        try {
            final LineNumberReader lineReader = new LineNumberReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                if (command == null) {
                    command = new StringBuffer();
                }

                if (table == null) {
                    table = new Table();
                }

                String trimmedLine = line.trim();

                // Interpret SQL Comment & Some statement that are not executable
                if (trimmedLine.startsWith("--")
                        || trimmedLine.startsWith("//")
                        || trimmedLine.startsWith("#")
                        || trimmedLine.toLowerCase().startsWith("rem inserting into")
                        || trimmedLine.toLowerCase().startsWith("set define off")) {

                    // do nothing...
                } else if (trimmedLine.endsWith(this.delimiter) ||
                        trimmedLine.endsWith(PL_SQL_BLOCK_END_DELIMITER)) { // Line is end of statement

                    // Append
                    if (trimmedLine.endsWith(this.delimiter)) {
                        command.append(line, 0, line.lastIndexOf(this.delimiter));
                        command.append(" ");
                    } else if (trimmedLine.endsWith(PL_SQL_BLOCK_END_DELIMITER)) {
                        command.append(line, 0, line.lastIndexOf(PL_SQL_BLOCK_END_DELIMITER));
                        command.append(" ");
                    }

                    Statement stmt = null;
                    ResultSet rs = null;
                    try {
                        stmt = conn.createStatement();
                        boolean hasResults = false;
                        if (this.stopOnError) {
                            hasResults = stmt.execute(command.toString());
                        } else {
                            try {
                                stmt.execute(command.toString());
                            } catch (final SQLException e) {
                                e.fillInStackTrace();
                                err.println("Error executing SQL Command: \"" +
                                        command + "\"");
                                err.println(e);
                                err.flush();
                                throw e;
                            }
                        }

                        rs = stmt.getResultSet();
                        if (hasResults && rs != null) {

                            List<String> headerRow = new ArrayList<>();
                            List<List<String>> toupleList = new ArrayList<>();

                            // Print & Store result column names
                            final ResultSetMetaData md = rs.getMetaData();
                            final int cols = md.getColumnCount();
                            for (int i = 0; i < cols; i++) {
                                final String name = md.getColumnLabel(i + 1);
                                out.print(name + "\t");

                                headerRow.add(name);
                            }

                            table.setHeaderRow(headerRow);

                            out.println("");
                            out.println(StringUtils.repeat("---------", md.getColumnCount()));
                            out.flush();

                            // Print & Store result rows
                            while (rs.next()) {
                                List<String> touple = new ArrayList<>();
                                for (int i = 1; i <= cols; i++) {
                                    final String value = rs.getString(i);
                                    out.print(value + "\t");

                                    touple.add(value);
                                }
                                out.println("");

                                toupleList.add(touple);
                            }
                            out.flush();

                            table.setToupleList(toupleList);
                            this.tableList.add(table);
                            table = null;
                        } else {
                            sqlOutput.add(stmt.getUpdateCount() + " row(s) affected.");

                            out.println(stmt.getUpdateCount() + " row(s) affected.");
                            out.flush();
                        }
                        command = null;
                    } finally {
                        if (rs != null) {
                            try {
                                rs.close();
                            } catch (final Exception e) {
                                err.println("Failed to close result: " + e.getMessage());
                                err.flush();
                            }
                        }
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (final Exception e) {
                                err.println("Failed to close statement: " + e.getMessage());
                                err.flush();
                            }
                        }
                    }
                } else if (trimmedLine.endsWith(PL_SQL_BLOCK_SPLIT_DELIMITER)) {
                    command.append(line, 0, line.lastIndexOf(PL_SQL_BLOCK_SPLIT_DELIMITER));
                    command.append(" ");
                } else { // Line is middle of a statement

                    // Append
                    command.append(line);
                    command.append(" ");
                }
            }
            if (!this.autoCommit) {
                conn.commit();
            }
        } catch (final SQLException e) {
            conn.rollback();
            e.fillInStackTrace();
            err.println("Error executing SQL Command: \"" + command + "\"");
            err.println(e);
            err.flush();
            throw e;
        } catch (final IOException e) {
            e.fillInStackTrace();
            err.println("Error reading SQL Script.");
            err.println(e);
            err.flush();
            throw e;
        }
    }

    /**
     * @return the tableList
     */
    public List<Table> getTableList() {
        return tableList;
    }

    /**
     * @param tableList the tableList to set
     */
    public void setTableList(List<Table> tableList) {
        this.tableList = tableList;
    }

    /**
     * @return the sqlOutput
     */
    public List<String> getSqlOutput() {
        return sqlOutput;
    }

    /**
     * @param sqlOutput the sqlOutput to set
     */
    public void setSqlOutput(List<String> sqlOutput) {
        this.sqlOutput = sqlOutput;
    }

    @Data
    public class Table {

        private List<List<String>> toupleList;
        private List<String> headerRow;
    }
}
package com.github.igorsuhorukov.postgresql;

import java.io.File;
import java.io.IOException;

public interface IPostgresqlService extends AutoCloseable{

    void start() throws IOException;

    String getJdbcConnectionUrl();

    String getHost();

    int getPort();

    String getDatabaseName();

    String getUsername();

    String getPassword();

    void importFromFile(File file);
    void importFromFileWithArgs(File file, String... cliArgs);
    void restoreFromFile(File file, String... cliArgs);
    void exportToFile(File file);
    void exportSchemeToFile(File file);
    void exportDataToFile(File file);
}

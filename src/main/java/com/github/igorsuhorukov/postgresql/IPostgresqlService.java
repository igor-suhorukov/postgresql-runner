package com.github.igorsuhorukov.postgresql;

import java.io.IOException;

public interface IPostgresqlService extends AutoCloseable{

    void start() throws IOException;

    String getJdbcConnectionUrl();

    String getHost();

    int getPort();

    String getDatabaseName();

    String getUsername();

    String getPassword();
}

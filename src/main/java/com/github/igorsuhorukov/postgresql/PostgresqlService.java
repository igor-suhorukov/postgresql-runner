package com.github.igorsuhorukov.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.runtime.Network;
import ru.yandex.qatools.embed.postgresql.*;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.DownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.CachedArtifactStoreBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.PRODUCTION;

@Named("PostgreSQL")
@Singleton
public class PostgresqlService implements IPostgresqlService {

    List<String> dbParams= Arrays.asList( "-E", "UTF-8", "--locale=C", "--lc-collate=C", "--lc-ctype=C");
    String username = System.getProperty("db.user", "user");
    String password = System.getProperty("db.password", "password");
    String databaseName = System.getProperty("db.name", "database");
    String databaseStoragePath = System.getProperty("db.storage", "database_storage");
    String host = System.getProperty("db.host", "localhost");
    int port;
    {
        try {
            String dbPortProperty = System.getProperty("db.port");
            if(dbPortProperty==null) {
                port = Network.getFreeServerPort();
            } else {
                port = Integer.parseInt(dbPortProperty);
            }
        } catch (NumberFormatException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    protected PostgresProcess process;
    private PostgresConfig config;

    @PostConstruct
    public void start() throws IOException {
        IRuntimeConfig runtimeConfig = buildRuntimeConfig();

        PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(runtimeConfig);
        config = new PostgresConfig(PRODUCTION,
                new AbstractPostgresConfig.Net(host, port),
                new AbstractPostgresConfig.Storage(databaseName, databaseStoragePath),
                new AbstractPostgresConfig.Timeout(),
                new AbstractPostgresConfig.Credentials(username, password));
        config.getAdditionalInitDbParams().addAll(dbParams);

        PostgresExecutable exec = runtime.prepare(config);
        process = exec.start();
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        process.stop();
    }

    @Inject
    @Named("postgresUsername")
    public void setUsername(Optional<String> username) {
        if(username.isPresent()) {
            this.username = username.get();
        }
    }

    @Inject
    @Named("postgresPassword")
    public void setPassword(Optional<String> password) {
        if(password.isPresent()) {
            this.password = password.get();
        }
    }

    @Inject
    @Named("postgresDatabaseName")
    public void setDatabaseName(Optional<String> databaseName) {
        if(databaseName.isPresent()) {
            this.databaseName = databaseName.get();
        }
    }

    @Inject
    @Named("postgresDatabaseStoragePath")
    public void setDatabaseStoragePath(Optional<String> databaseStoragePath) {
        if(databaseStoragePath.isPresent()) {
            this.databaseStoragePath = databaseStoragePath.get();
        }
    }

    @Inject
    @Named("postgresHost")
    public void setHost(Optional<String> host) {
        if(host.isPresent()) {
            this.host = host.get();
        }
    }

    @Inject
    @Named("postgresPort")
    public void setPort(Optional<Integer> port) {
        if(port.isPresent()) {
            this.port = port.get();
        }
    }

    @Inject
    @Named("postgresDatabaseParameters")
    public void setDatabaseParameters(Optional<List<String>> dbParams) {
        if(dbParams.isPresent()) {
            this.dbParams = dbParams.get();
        }
    }

    public String getJdbcConnectionUrl(){
        return String.format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s",
                config.net().host(),
                config.net().port(),
                config.storage().dbName(),
                config.credentials().username(),
                config.credentials().password()
        );
    }

    public String getHost(){
        return config.net().host();
    }

    public int getPort(){
        return config.net().port();
    }

    public String getDatabaseName(){
        return config.storage().dbName();
    }

    public String getUsername(){
        return config.credentials().username();
    }

    public String getPassword(){
        return config.credentials().password();
    }

    protected IRuntimeConfig buildRuntimeConfig() {
        // turns off the default functionality of unzipping on every run.
        final String tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "pgembed").toFile().getPath();
        final Command cmd = Command.Postgres;
        final FixedPath cachedDir = new FixedPath(tmpDir);
        return new RuntimeConfigBuilder()
                .defaults(cmd)
                .artifactStore(new CachedArtifactStoreBuilder()
                        .defaults(cmd)
                        .tempDir(cachedDir)
                        .download(new DownloadConfigBuilder()
                                .defaultsForCommand(cmd)
                                .packageResolver(new PackagePaths(cmd, cachedDir))
                                .build()))
                .build();
    }
}

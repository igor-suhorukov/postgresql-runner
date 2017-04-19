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
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.PRODUCTION;

@Named("PostgreSQL")
@Singleton
public class PostgresqlService implements AutoCloseable{

    protected PostgresProcess process;
    private PostgresConfig config;

    @PostConstruct
    public void start() throws IOException {
        // turns off the default functionality of unzipping on every run.
        IRuntimeConfig runtimeConfig = buildRuntimeConfig();

        PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(runtimeConfig);
        config = new PostgresConfig(PRODUCTION,
                new AbstractPostgresConfig.Net("localhost", Network.getFreeServerPort()),
                new AbstractPostgresConfig.Storage(
                        System.getProperty("db.name","database"),
                        System.getProperty("db.storage","database_storage")),
                new AbstractPostgresConfig.Timeout(),
                new AbstractPostgresConfig.Credentials(
                        System.getProperty("db.user","user"), System.getProperty("db.password","password")));
        config.getAdditionalInitDbParams().addAll(Arrays.asList(
                "-E", "SQL_ASCII",
                "--locale=C",
                "--lc-collate=C",
                "--lc-ctype=C"
        ));

        PostgresExecutable exec = runtime.prepare(config);
        process = exec.start();
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        process.stop();
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

    public int getPort(){
        return config.net().port();
    }

    public String getHost(){
        return config.net().host();
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

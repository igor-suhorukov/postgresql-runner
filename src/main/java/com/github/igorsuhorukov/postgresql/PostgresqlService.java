package com.github.igorsuhorukov.postgresql;

import com.github.igorsuhorukov.postgresql.model.FileWithArgs;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.store.DownloadConfigBuilder;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ArtifactStoreBuilder;
import de.flapdoodle.embed.process.store.IDownloader;
import de.flapdoodle.embed.process.store.PostgresArtifactStoreBuilder;
import ru.yandex.qatools.embed.postgresql.*;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresDownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Named("PostgreSQL")
@Singleton
public class PostgresqlService implements IPostgresqlService {

    List<String> dbParams= Arrays.asList( "-E", "UTF-8", "--locale=C", "--lc-collate=C", "--lc-ctype=C");
    String username = System.getProperty("db.user", "user");
    String password = System.getProperty("db.password", "password");
    String databaseName = System.getProperty("db.name", "database");
    String databaseStoragePath = System.getProperty("db.storage", "database_storage");
    String host = System.getProperty("db.host", "localhost");
    String version = System.getProperty("db.version", Version.V9_6_5.asInDownloadPath());
    String downloadPath = System.getProperty("db.downloadPath");
    int port;
    protected PostgresProcess process;
    private PostgresConfig config;
    private IDownloader downloader;
    protected Optional<FileWithArgs> importFromFileWithArgs = Optional.empty();
    protected Optional<FileWithArgs> restoreFromFile = Optional.empty();
    protected Optional<File> exportToFile = Optional.empty();
    protected Optional<File> exportSchemeToFile = Optional.empty();
    protected Optional<File> exportDataToFile = Optional.empty();

    public PostgresqlService() {
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

    @PostConstruct
    public void start() throws IOException {
        IRuntimeConfig runtimeConfig = buildRuntimeConfig();

        PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(runtimeConfig);
        config = new PostgresConfig(new GenericVersion(version),
                new AbstractPostgresConfig.Net(host, port),
                new AbstractPostgresConfig.Storage(databaseName, databaseStoragePath),
                new AbstractPostgresConfig.Timeout(),
                new AbstractPostgresConfig.Credentials(username, password));
        config.getAdditionalInitDbParams().addAll(dbParams);

        PostgresExecutable exec = runtime.prepare(config);
        process = exec.start();
        importFromFileWithArgs.ifPresent(parameter -> importFromFileWithArgs(parameter.getFile(), parameter.getCliArgs()));
        restoreFromFile.ifPresent(parameter -> restoreFromFile(parameter.getFile(), parameter.getCliArgs()));
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        exportDataToFile.ifPresent(this::exportDataToFile);
        exportSchemeToFile.ifPresent(this::exportSchemeToFile);
        exportToFile.ifPresent(this::exportToFile);
        process.stop();
    }

    @Inject
    @Named("postgresUsername")
    public void setUsername(Optional<String> username) {
        username.ifPresent(parameter -> this.username = parameter);
    }

    @Inject
    @Named("postgresPassword")
    public void setPassword(Optional<String> password) {
        password.ifPresent(parameter -> this.password = parameter);
    }

    @Inject
    @Named("postgresDatabaseName")
    public void setDatabaseName(Optional<String> databaseName) {
        databaseName.ifPresent(parameter -> this.databaseName = parameter);
    }

    @Inject
    @Named("postgresDatabaseStoragePath")
    public void setDatabaseStoragePath(Optional<String> databaseStoragePath) {
        databaseStoragePath.ifPresent(parameter -> this.databaseStoragePath = parameter);
    }

    @Inject
    @Named("postgresHost")
    public void setHost(Optional<String> host) {
        host.ifPresent(parameter -> this.host = parameter);
    }

    @Inject
    @Named("postgresPort")
    public void setPort(Optional<Integer> port) {
        port.ifPresent(parameter -> this.port = parameter);
    }

    @Inject
    @Named("postgresDatabaseParameters")
    public void setDatabaseParameters(Optional<List<String>> dbParams) {
        dbParams.ifPresent(parameter -> this.dbParams = parameter);
    }

    @Inject
    @Named("postgresVersion")
    public void setVersion(Optional<String> version) {
        version.ifPresent(parameter -> this.version = parameter);
    }

    @Inject
    @Named("postgresDownloadPath")
    public void setDownloadPath(Optional<String> version) {
        version.ifPresent(parameter -> this.downloadPath = parameter);
    }

    @Inject
    public void setDownloader(Optional<IDownloader> downloader) {
        downloader.ifPresent(parameter -> this.downloader = parameter);
    }

    @Inject
    @Named("importFromFileWithArgs")
    public void setImportFromFileWithArgs(Optional<FileWithArgs> fileWithArgs){
        importFromFileWithArgs = fileWithArgs;
    }

    @Inject
    @Named("restoreFromFile")
    public void setRestoreFromFile(Optional<FileWithArgs> fileWithArgs){
        restoreFromFile = fileWithArgs;
    }

    @Inject
    @Named("exportToFile")
    public void setExportToFile(Optional<File> file){
        exportToFile = file;
    }

    @Inject
    @Named("exportSchemeToFile")
    public void setExportSchemeToFile(Optional<File> file){
        exportSchemeToFile = file;
    }

    @Inject
    @Named("exportDataToFile")
    public void setExportDataToFile(Optional<File> file){
        exportDataToFile = file;
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

    @Override
    public void importFromFile(File file) {
        process.importFromFile(file);
    }

    @Override
    public void importFromFileWithArgs(File file, String... cliArgs) {
        process.importFromFileWithArgs(file, cliArgs);
    }

    @Override
    public void restoreFromFile(File file, String... cliArgs) {
        process.restoreFromFile(file, cliArgs);
    }

    @Override
    public void exportToFile(File file) {
        process.exportToFile(file);
    }

    @Override
    public void exportSchemeToFile(File file) {
        process.exportSchemeToFile(file);
    }

    @Override
    public void exportDataToFile(File file) {
        process.exportDataToFile(file);
    }

    protected IRuntimeConfig buildRuntimeConfig() {
        // turns off the default functionality of unzipping on every run.
        final String tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "pgembed").toFile().getPath();
        final Command cmd = Command.Postgres;
        final FixedPath cachedDir = new FixedPath(tmpDir);
        DownloadConfigBuilder downloadConfigBuilder = new PostgresDownloadConfigBuilder()
                .defaultsForCommand(cmd)
                .packageResolver(new PackagePaths(cmd, cachedDir));
        if (downloadPath!=null){
            downloadConfigBuilder.downloadPath(downloadPath);
        }
        ArtifactStoreBuilder artifactStoreBuilder = new PostgresArtifactStoreBuilder()
                .defaults(cmd)
                .tempDir(cachedDir)
                .download(downloadConfigBuilder
                        .build());

        if(downloader!=null){
            artifactStoreBuilder.downloader(downloader);
        }

        return new RuntimeConfigBuilder().defaults(cmd).artifactStore(artifactStoreBuilder).build();
    }
}

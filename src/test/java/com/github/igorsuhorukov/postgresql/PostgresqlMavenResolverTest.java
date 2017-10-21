package com.github.igorsuhorukov.postgresql;

import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.PackagePaths;
import ru.yandex.qatools.embed.postgresql.config.PostgresDownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration(classes = PgMavenResolverContext.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PostgresqlMavenResolverTest {

    public static final String DOWNLOAD_PATH = "http://get.enterprisedb.com/postgresql/";
    private static Server server;

    @Autowired
    AbstractApplicationContext applicationContext;
    @Autowired
    PostgresqlService postgresqlService;


    @BeforeClass
    public static void startMavenRepoEmulation() throws Exception{

        server = new Server(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9684));

        PackagePaths packagePaths = new PackagePaths(null, null);
        Distribution distribution = Distribution.detectFor(new GenericVersion(Version.V9_6_3.asInDownloadPath()));
        String postgresFile = packagePaths.getPath(distribution);

        String groupArtifactVersion = MavenDownloader.getGroupArtifactVersion(
                "com.github.igor-suhorukov:postgresql", distribution, packagePaths);


        File cacheDir = new PostgresDownloadConfigBuilder().defaultsForCommand(Command.Postgres).build().getArtifactStorePath().asFile();
        if(cacheDir.exists()&&cacheDir.isDirectory()){
            Arrays.stream(cacheDir.listFiles()).forEach(File::delete);
        }

        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if(request.getRequestURI().endsWith(postgresFile.replace("-binaries",""))) {
                    File pgArchiveFile = new File(System.getProperty("java.io.tmpdir"), postgresFile);
                    try (OutputStream outputStream = response.getOutputStream()) {
                        FileUtils.copyURLToFile(new URL(DOWNLOAD_PATH + postgresFile), pgArchiveFile);
                        response.setStatus(200);
                        response.setContentType("application/octet-stream");
                        response.setContentLength((int) pgArchiveFile.length());
                        FileUtils.copyFile(pgArchiveFile, outputStream);
                    } finally {
                        pgArchiveFile.delete();
                    }
                }
            }
        };
        server.setHandler(handler);
        server.start();
    }

    @AfterClass
    public static void stopMavenRepoEmulation() throws Exception{
        server.stop();
    }

    @Test
    public void testResolveInMavenCustomRepo() throws Exception {
        assertNotNull(postgresqlService);
        assertNotNull(postgresqlService.getJdbcConnectionUrl());

        String username = applicationContext.getBean("postgresDownloadPath", String.class);
        assertEquals(username, postgresqlService.downloadPath);
    }
}

package com.github.igorsuhorukov.postgresql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PreDestroy;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration(classes = PgContextPropOverride.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class PostgresqlServiceCustomParamsTest {

    @Autowired
    AbstractApplicationContext applicationContext;

    @Autowired
    PostgresqlService postgresqlService;

    @PreDestroy
    private void releaseContext(){
        applicationContext.close();
    }

    @Test
    public void testSpringContextPgService() throws Exception {
        assertNotNull(postgresqlService);
        assertNotNull(postgresqlService.getJdbcConnectionUrl());

        String username = applicationContext.getBean("postgresUsername", String.class);
        String password = applicationContext.getBean("postgresPassword", String.class);
        String databaseName = applicationContext.getBean("postgresDatabaseName", String.class);
        String host = applicationContext.getBean("postgresHost", String.class);
        String databaseStoragePath = applicationContext.getBean("postgresDatabaseStoragePath", String.class);
        Integer port = applicationContext.getBean("postgresPort", Integer.class);
        List parameters = applicationContext.getBean("postgresDatabaseParameters", List.class);

        assertEquals(username, postgresqlService.username);
        assertEquals(password, postgresqlService.password);
        assertEquals(databaseName, postgresqlService.databaseName);
        assertEquals(host, postgresqlService.host);
        assertEquals(databaseStoragePath, postgresqlService.databaseStoragePath);
        assertEquals((long)port, (long)postgresqlService.port);
        assertEquals(parameters, postgresqlService.dbParams);
    }
}

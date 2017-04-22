package com.github.igorsuhorukov.postgresql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PreDestroy;
import java.sql.*;
import java.util.Date;

import static org.junit.Assert.*;

@ContextConfiguration(classes = PgContext.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class PostgresqlServiceTest {

    @Autowired
    AbstractApplicationContext applicationContext;

    @Autowired
    IPostgresqlService postgresqlService;

    @PreDestroy
    private void releaseContext(){
        applicationContext.close();
    }

    @Test
    public void testSpringContextPgService() throws Exception {
        assertNotNull(postgresqlService);
        assertNotNull(postgresqlService.getJdbcConnectionUrl());
        assertEquals("user", postgresqlService.getUsername());
        assertEquals("database", postgresqlService.getDatabaseName());
    }

    @Test
    public void testJdbcConnection() throws Exception{
        try (Connection connection = DriverManager.getConnection(postgresqlService.getJdbcConnectionUrl())){
            try (Statement statement = connection.createStatement()){
                try (ResultSet resultSet = statement.executeQuery("select now()")){
                    assertTrue(resultSet.next());
                    Timestamp timestamp = resultSet.getTimestamp(1);
                    assertNotNull(timestamp);
                    assertTrue(timestamp.before(new Date()));
                }
            }
        }
    }
}

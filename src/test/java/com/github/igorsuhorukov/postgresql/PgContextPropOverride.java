package com.github.igorsuhorukov.postgresql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import javax.inject.Singleton;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ComponentScan("com.github.igorsuhorukov.postgresql")
public class PgContextPropOverride {

    @Bean("postgresPassword")
    public String getPostgresPassword(){
        return "PWD";
    }

    @Bean("postgresUsername")
    public String getPostgresUsername(){
        return "SUPER_USER";
    }

    @Bean("postgresDatabaseName")
    public String getPostgresDatabaseName(){
        return "newDatabase";
    }

    @Bean("postgresHost")
    public String getPostgresHost(){
        return "ANOTHER_HOST";
    }

    @Bean("postgresPort")
    public Integer getPostgresPort(){
        return 9999;
    }

    @Bean("postgresDatabaseParameters")
    public List<String> getPostgresDatabaseParameters(){
        return Arrays.asList("-E", "UTF-8");
    }

    @Bean("postgresDatabaseStoragePath")
    @Singleton
    public String getPostgresDatabaseStoragePath(){
        return Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()).toFile().getPath();
    }
}

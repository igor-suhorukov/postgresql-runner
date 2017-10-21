package com.github.igorsuhorukov.postgresql;

import de.flapdoodle.embed.process.store.IDownloader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.github.igorsuhorukov.postgresql")
public class PgMavenResolverContext {

    @Bean("postgresDownloadPath")
    public String getPostgresDownloadPath(){
        return "com.github.igor-suhorukov:postgresql?http://127.0.0.1:9684";
    }

    @Bean
    public IDownloader getDownloader(){
        return new MavenDownloader();
    }

}

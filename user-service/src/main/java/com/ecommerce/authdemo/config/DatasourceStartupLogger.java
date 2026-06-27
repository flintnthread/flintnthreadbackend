package com.ecommerce.authdemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatasourceStartupLogger implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            log.info("[DB] connected url={} user={} catalog={} schema={}",
                    metaData.getURL(),
                    metaData.getUserName(),
                    connection.getCatalog(),
                    connection.getSchema());
        } catch (Exception e) {
            log.error("[DB] failed to read datasource metadata: {}", e.getMessage(), e);
        }
    }
}

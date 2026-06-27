package com.ecommerce.authdemo.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * When MySQL is configured but unreachable (typical local dev: wrong root password),
 * switches to an embedded MySQL-compatible H2 database so all app features work locally.
 * Production should set {@code APP_DB_REQUIRE_MYSQL=true} to fail fast instead.
 */
public class MysqlConnectivityFallbackListener
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    static final String LOCAL_H2_PROFILE = "local-h2";
    private static final String FALLBACK_PROPERTY_SOURCE = "mysqlFallbackH2";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();

        if (environment.acceptsProfiles(Profiles.of("dev", "local", LOCAL_H2_PROFILE, "prod"))) {
            return;
        }
        if (Boolean.parseBoolean(environment.getProperty("app.db.require-mysql", "false"))) {
            return;
        }
        if (!Boolean.parseBoolean(environment.getProperty("app.db.mysql-fallback", "true"))) {
            return;
        }

        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.toLowerCase().contains("jdbc:mysql")) {
            return;
        }

        String username = environment.getProperty("spring.datasource.username", "root");
        String password = environment.getProperty("spring.datasource.password", "");

        if (canConnect(url, username, password)) {
            return;
        }

        environment.addActiveProfile(LOCAL_H2_PROFILE);
        applyH2Overrides(environment);

        System.out.println(
                "[DB] MySQL connection failed for " + url
                        + " — using embedded H2 (MySQL-compatible, data in ./data/)."
                        + " To use local MySQL instead, run scripts/setup-local-db.ps1"
                        + " or set APP_DB_REQUIRE_MYSQL=true to fail fast."
        );
    }

    private void applyH2Overrides(ConfigurableEnvironment environment) {
        Map<String, Object> h2 = new LinkedHashMap<>();
        h2.put("spring.datasource.url",
                "jdbc:h2:file:./data/flintdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE;NON_KEYWORDS=USER,VALUE");
        h2.put("spring.datasource.driver-class-name", "org.h2.Driver");
        h2.put("spring.datasource.username", "sa");
        h2.put("spring.datasource.password", "");
        h2.put("spring.jpa.hibernate.ddl-auto", "update");
        h2.put("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");

        environment.getPropertySources().addFirst(new MapPropertySource(FALLBACK_PROPERTY_SOURCE, h2));
    }

    private boolean canConnect(String url, String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.setLoginTimeout(3);
            try (var connection = DriverManager.getConnection(url, username, password)) {
                return connection.isValid(2);
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}

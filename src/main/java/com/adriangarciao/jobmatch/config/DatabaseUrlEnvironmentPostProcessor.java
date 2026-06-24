package com.adriangarciao.jobmatch.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses Railway's DATABASE_URL (postgresql://user:pass@host:port/db)
 * and exposes PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD so that
 * application.properties can reference them with ${PGHOST} etc.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || !databaseUrl.startsWith("postgresql://")) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        Map<String, Object> props = new HashMap<>();
        props.put("PGHOST", uri.getHost());
        props.put("PGPORT", String.valueOf(uri.getPort() == -1 ? 5432 : uri.getPort()));
        props.put("PGDATABASE", uri.getPath().replaceFirst("^/", ""));

        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":", 2);
            props.put("PGUSER", parts[0]);
            if (parts.length > 1) {
                props.put("PGPASSWORD", parts[1]);
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseUrl", props));
    }
}

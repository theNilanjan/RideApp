package com.ridebooking.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfig {
    @Bean
    @Primary
    DataSource dataSource(Environment env) {
        String url = firstText(env.getProperty("SPRING_DATASOURCE_URL"), env.getProperty("DATABASE_URL"));
        String username = env.getProperty("SPRING_DATASOURCE_USERNAME");
        String password = env.getProperty("SPRING_DATASOURCE_PASSWORD");

        if (!StringUtils.hasText(url)) {
            String host = env.getProperty("PGHOST", "localhost");
            String port = env.getProperty("PGPORT", "5432");
            String database = env.getProperty("PGDATABASE", "ridedb");
            url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            username = firstText(username, env.getProperty("PGUSER", "ride"));
            password = firstText(password, env.getProperty("PGPASSWORD", "ride"));
        } else if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            DatabaseUrl databaseUrl = parseDatabaseUrl(url);
            url = databaseUrl.jdbcUrl();
            username = firstText(username, databaseUrl.username());
            password = firstText(password, databaseUrl.password());
        }

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        if (StringUtils.hasText(username)) {
            dataSource.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            dataSource.setPassword(password);
        }
        return dataSource;
    }

    private static DatabaseUrl parseDatabaseUrl(String rawUrl) {
        URI uri = URI.create(rawUrl);
        String userInfo = uri.getUserInfo();
        String username = null;
        String password = null;
        if (StringUtils.hasText(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbcUrl.append(":").append(uri.getPort());
        }
        jdbcUrl.append(uri.getPath());
        if (StringUtils.hasText(uri.getQuery())) {
            jdbcUrl.append("?").append(uri.getQuery());
        }

        return new DatabaseUrl(jdbcUrl.toString(), username, password);
    }

    private static String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record DatabaseUrl(String jdbcUrl, String username, String password) {
    }
}

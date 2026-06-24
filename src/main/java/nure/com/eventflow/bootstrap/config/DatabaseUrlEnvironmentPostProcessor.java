package nure.com.eventflow.bootstrap.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.util.UriUtils;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "eventflowDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasText(environment.getProperty("EVENTFLOW_DB_URL"))) {
            return;
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (!hasText(databaseUrl)) {
            return;
        }

        DatabaseConnectionProperties connection = toDatabaseConnectionProperties(databaseUrl);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("EVENTFLOW_DB_URL", connection.jdbcUrl());

        if (!hasText(environment.getProperty("EVENTFLOW_DB_USERNAME")) && hasText(connection.username())) {
            properties.put("EVENTFLOW_DB_USERNAME", connection.username());
        }
        if (!hasText(environment.getProperty("EVENTFLOW_DB_PASSWORD")) && hasText(connection.password())) {
            properties.put("EVENTFLOW_DB_PASSWORD", connection.password());
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    static DatabaseConnectionProperties toDatabaseConnectionProperties(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:postgresql:")) {
            return new DatabaseConnectionProperties(databaseUrl, null, null);
        }

        URI uri = URI.create(databaseUrl);
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalArgumentException("DATABASE_URL must use postgres, postgresql, or jdbc:postgresql scheme.");
        }

        String host = uri.getHost();
        String databaseName = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (!hasText(host) || !hasText(databaseName)) {
            throw new IllegalArgumentException("DATABASE_URL must include a host and database name.");
        }

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
        if (hasText(uri.getRawQuery())) {
            jdbcUrl = jdbcUrl + "?" + uri.getRawQuery();
        }

        String username = null;
        String password = null;
        if (hasText(uri.getRawUserInfo())) {
            String[] userInfo = uri.getRawUserInfo().split(":", 2);
            username = decode(userInfo[0]);
            if (userInfo.length > 1) {
                password = decode(userInfo[1]);
            }
        }

        return new DatabaseConnectionProperties(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return UriUtils.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record DatabaseConnectionProperties(String jdbcUrl, String username, String password) {
    }
}

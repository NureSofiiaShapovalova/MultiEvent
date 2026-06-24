package nure.com.eventflow.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nure.com.eventflow.EventflowApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void convertsRenderPostgresUrlToSpringDatasourceProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://eventflow:p%40ss@dpg-host.internal:5432/eventflow_db?sslmode=require");

        processor.postProcessEnvironment(environment, new SpringApplication(EventflowApplication.class));

        assertThat(environment.getProperty("EVENTFLOW_DB_URL"))
                .isEqualTo("jdbc:postgresql://dpg-host.internal:5432/eventflow_db?sslmode=require");
        assertThat(environment.getProperty("EVENTFLOW_DB_USERNAME")).isEqualTo("eventflow");
        assertThat(environment.getProperty("EVENTFLOW_DB_PASSWORD")).isEqualTo("p@ss");
    }

    @Test
    void keepsExplicitEventflowDatabaseUrl() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("EVENTFLOW_DB_URL", "jdbc:postgresql://localhost:5432/eventflow")
                .withProperty("DATABASE_URL", "postgresql://eventflow:secret@dpg-host.internal:5432/eventflow_db");

        processor.postProcessEnvironment(environment, new SpringApplication(EventflowApplication.class));

        assertThat(environment.getProperty("EVENTFLOW_DB_URL"))
                .isEqualTo("jdbc:postgresql://localhost:5432/eventflow");
        assertThat(environment.getProperty("EVENTFLOW_DB_USERNAME")).isNull();
    }

    @Test
    void rejectsUnsupportedDatabaseUrlScheme() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor
                .toDatabaseConnectionProperties("mysql://user:secret@localhost:3306/eventflow"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DATABASE_URL must use postgres");
    }
}

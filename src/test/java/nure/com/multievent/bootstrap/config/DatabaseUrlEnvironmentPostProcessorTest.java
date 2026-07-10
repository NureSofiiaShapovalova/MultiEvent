package nure.com.multievent.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nure.com.multievent.MultiEventApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void convertsRenderPostgresUrlToSpringDatasourceProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://multievent:p%40ss@dpg-host.internal:5432/multievent_db?sslmode=require");

        processor.postProcessEnvironment(environment, new SpringApplication(MultiEventApplication.class));

        assertThat(environment.getProperty("MULTIEVENT_DB_URL"))
                .isEqualTo("jdbc:postgresql://dpg-host.internal:5432/multievent_db?sslmode=require");
        assertThat(environment.getProperty("MULTIEVENT_DB_USERNAME")).isEqualTo("multievent");
        assertThat(environment.getProperty("MULTIEVENT_DB_PASSWORD")).isEqualTo("p@ss");
    }

    @Test
    void keepsExplicitMultiEventDatabaseUrl() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MULTIEVENT_DB_URL", "jdbc:postgresql://localhost:5432/multievent")
                .withProperty("DATABASE_URL", "postgresql://multievent:secret@dpg-host.internal:5432/multievent_db");

        processor.postProcessEnvironment(environment, new SpringApplication(MultiEventApplication.class));

        assertThat(environment.getProperty("MULTIEVENT_DB_URL"))
                .isEqualTo("jdbc:postgresql://localhost:5432/multievent");
        assertThat(environment.getProperty("MULTIEVENT_DB_USERNAME")).isNull();
    }

    @Test
    void rejectsUnsupportedDatabaseUrlScheme() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor
                .toDatabaseConnectionProperties("mysql://user:secret@localhost:3306/multievent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DATABASE_URL must use postgres");
    }
}

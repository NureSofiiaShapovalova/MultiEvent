package nure.com.eventflow.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    private static final Path MODULE_ROOT = Path.of("src/main/java/nure/com/eventflow");

    @Test
    void plannedFeatureModulesExist() throws IOException {
        Set<String> modules;
        try (Stream<Path> paths = Files.list(MODULE_ROOT)) {
            modules = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .collect(java.util.stream.Collectors.toSet());
        }

        assertThat(modules).contains(
                "iam",
                "catalog",
                "organizer",
                "ticketing",
                "checkout",
                "payment",
                "notifications",
                "admin",
                "reporting",
                "shared");
    }

    @Test
    void webLayerDoesNotImportRepositoriesDirectly() throws IOException {
        List<Path> offendingFiles;
        try (Stream<Path> paths = Files.walk(MODULE_ROOT)) {
            offendingFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("\\web\\") || path.toString().contains("/web/"))
                    .filter(this::importsRepository)
                    .toList();
        }

        assertThat(offendingFiles)
                .as("Controllers and web DTOs should call application services, not infra repositories.")
                .isEmpty();
    }

    private boolean importsRepository(Path path) {
        try {
            String source = Files.readString(path);
            return source.lines().anyMatch(line ->
                    line.startsWith("import nure.com.eventflow.")
                            && line.contains(".infra.")
                            && line.endsWith("Repository;"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect " + path, exception);
        }
    }
}

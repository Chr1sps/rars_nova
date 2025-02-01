package utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ProjectPaths {

    private ProjectPaths() {
    }

    private static @NotNull String getPathName(final @NotNull Path path) {
        return Optional.ofNullable(path.getFileName()).map(Object::toString).orElse("");
    }

    public static @NotNull Path getProjectRoot() {
        var directory = getProjectRootByClass();
        while (directory != null) {
            try {
                try (final var fileList = Files.list(directory)) {
                    final var children = fileList.map(ProjectPaths::getPathName).collect(Collectors.toSet());
                    if (children.contains("gradle") && children.contains("src")) {
                        return directory;
                    }
                }
            } catch (final IOException ignore) {
            }
            directory = directory.getParent();
        }
        throw new RuntimeException("Could not find project root");
    }

    private static @NotNull Path getProjectRootByClass() {
        final var replacedName = ProjectPaths.class.getName().replace(".", "/");
        final var url = ProjectPaths.class.getResource("/" + replacedName + ".class");
        if (url == null) {
            throw new RuntimeException("Could not find class file");
        }
        try {
            return switch (url.getProtocol()) {
                case "file" -> Paths.get(url.toURI());
                case "jar" -> {
                    final var pathElements = Paths.get(new URI(url.getFile())).toString().split("!");
                    if (pathElements.length == 0) {
                        throw new RuntimeException("Could not find jar file");
                    } else {
                        yield Paths.get(pathElements[0]);
                    }
                }
                default -> throw new RuntimeException("Unsupported protocol");
            };
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

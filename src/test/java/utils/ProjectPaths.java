package utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class ProjectPaths {
    private static final ProjectPaths INSTANCE = new ProjectPaths();

    private ProjectPaths() {
    }

    private static @NotNull String getPathName(final @NotNull Path path) {
        return Optional.ofNullable(path.getFileName()).map(Object::toString).orElse("");
    }

    public static @NotNull Path getProjectRoot() {
        return Objects.requireNonNull(INSTANCE._getProjectRoot());
    }

    private @Nullable Path _getProjectRoot() {
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
        return null;
    }

    private @Nullable Path getProjectRootByClass() {
        final var replacedName = this.getClass().getName().replace(".", "/");
        final var url = getClass().getResource("/" + replacedName + ".class");
        if (url == null) {
            return null;
        }
        try {
            return switch (url.getProtocol()) {
                case "file" -> Paths.get(url.toURI());
                case "jar" -> {
                    final var pathElements = Paths.get(new URI(url.getFile())).toString().split("!");
                    if (pathElements.length == 0) {
                        yield null;
                    } else {
                        yield Paths.get(pathElements[0]);
                    }
                }
                default -> null;
            };
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

package utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Path;
import java.util.Objects;

public abstract class RarsTestBase {
    private @Nullable TestInfo testInfo;

    protected static @NotNull Path getTestDataPath() {
        return ProjectPaths.getProjectRoot().resolve("src/test/resources/test-data").toAbsolutePath();
    }

    @BeforeEach
    protected void getTestInfo(final TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    protected @NotNull String getTestName() {
        return Objects.requireNonNull(testInfo).getDisplayName();
    }

}

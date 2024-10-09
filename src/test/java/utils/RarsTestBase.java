package utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Base class for RARS tests containing common utility methods.
 */
public abstract class RarsTestBase {
    /**
     * Inner field used for storing {@link TestInfo} fetched from the
     * {@link RarsTestBase} method.
     */
    private @Nullable TestInfo testInfo;

    /**
     * @return the absolute path to the test data directory.
     */
    protected static @NotNull Path getTestDataPath() {
        return ProjectPaths.getProjectRoot().resolve("src/test/resources/test-data").toAbsolutePath();
    }

    /**
     * Fetches the {@link TestInfo} object for the current test.
     */
    @BeforeEach
    protected void getTestInfo(final TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    /**
     * @return the name of the current test.
     */
    protected @NotNull String getTestName() {
        return Objects.requireNonNull(testInfo).getDisplayName();
    }

}

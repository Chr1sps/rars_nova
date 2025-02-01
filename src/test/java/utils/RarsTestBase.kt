package utils

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.nio.file.Path

/**
 * Base class for RARS tests containing common utility methods.
 */
abstract class RarsTestBase {
    /**
     * Inner field used for storing [TestInfo] fetched from the
     * [RarsTestBase] method.
     */
    private lateinit var testInfo: TestInfo

    /**
     * Fetches the [TestInfo] object for the current test.
     */
    @BeforeEach
    protected fun getTestInfo(testInfo: TestInfo) {
        this.testInfo = testInfo
    }

    /** The name of the current test */
    protected val testName: String
        get() = testInfo.displayName

    companion object {
        /** The absolute path to the test data directory. */
        val testDataPath: Path
            get() = ProjectPaths.projectRoot.resolve("src/test/resources/test-data").toAbsolutePath()
    }
}

import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.testkit.engine.EngineTestKit

class CustomEngineTest {
    @Test
    fun testOtusEngine() {
        EngineTestKit.engine("otus").selectors(DiscoverySelectors.selectFile("src/test/test.http")).execute().testEvents().assertStatistics {
            it.succeeded(1)
            it.finished(1)
        }
    }
}
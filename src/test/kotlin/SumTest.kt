import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.otus.customengine.OtusTest

class SumTest {
    @Test
    fun testSum() {
        val calculator = Calculator()
        assertEquals(4, calculator.sum(2,2))
    }

    @OtusTest
    fun testSum2() {
        val calculator = Calculator()
        assertEquals(6, calculator.sum(3,3))
    }

    @OtusTest
    fun testSumFail() {
        val calculator = Calculator()
        assertEquals(6, calculator.sum(3,4))
    }
}
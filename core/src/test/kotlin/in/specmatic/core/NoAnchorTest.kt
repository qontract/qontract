package `in`.specmatic.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class NoAnchorTest {
    @Test
    fun `should return the path given`() {
        val path = "/some/path"
        assertThat(NoAnchor.resolve(path).canonicalPath).isEqualTo(path)
    }
}
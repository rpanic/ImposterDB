package unit

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import sql.replaceWildCards

class SqlUtilsTest{

    @Test
    fun testReplaceWildcard(){

        assertThat(replaceWildCards("TEST ?", "wild")).isEqualTo("TEST 'wild'")
        assertThat(replaceWildCards("TEST ?", 'w')).isEqualTo("TEST 'w'")
        assertThat(replaceWildCards("TEST ?", 1)).isEqualTo("TEST 1")
        assertThat(replaceWildCards("TEST ?", 1.0)).isEqualTo("TEST 1.0")
        assertThat(replaceWildCards("TEST ?", 1.0f)).isEqualTo("TEST 1.0")

    }

}
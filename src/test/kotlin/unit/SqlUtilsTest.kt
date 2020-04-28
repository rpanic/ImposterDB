package unit

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import sql.replaceWildCards

class SqlUtilsTest{

    @Test
    fun testReplaceWildcard(){

        assertThat("TEST ?".replaceWildCards("wild")).isEqualTo("TEST 'wild'")
        assertThat("TEST ?".replaceWildCards('w')).isEqualTo("TEST 'w'")
        assertThat("TEST ?".replaceWildCards( 1)).isEqualTo("TEST 1")
        assertThat("TEST ?".replaceWildCards( 1.0)).isEqualTo("TEST 1.0")
        assertThat("TEST ?".replaceWildCards( 1.0f)).isEqualTo("TEST 1.0")
        assertThat("TEST ? AND ?".replaceWildCards("t", 1.0f)).isEqualTo("TEST 't' AND 1.0")
        assertThat("TEST ? AND ?".replaceWildCards(Long.MAX_VALUE, Double.MAX_VALUE)).isEqualTo("TEST ${Long.MAX_VALUE} AND ${Double.MAX_VALUE}")

        assertThat("TEST 'a?' AND ?".replaceWildCards("wild")).isEqualTo("TEST 'a?' AND 'wild'")

    }

}
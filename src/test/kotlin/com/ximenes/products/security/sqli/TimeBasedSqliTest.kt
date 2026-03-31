package com.ximenes.products.security.sqli

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.servlet.get
import java.util.stream.Stream

class TimeBasedSqliTest : SqliTestBase() {

    @ParameterizedTest
    @MethodSource("paginationPayloads")
    fun sqli_pagination_timeBased_shouldNotDelay(url: String) {
        val start = System.currentTimeMillis()

        mockMvc.get(url).andReturn()

        val elapsed = System.currentTimeMillis() - start
        
        kotlin.test.assertTrue(
            elapsed < 1000L,
            "URL: $url - should not delay by pg_sleep. Elapsed: ${elapsed}ms"
        )
    }

    companion object {
        @JvmStatic
        fun paginationPayloads(): Stream<String> = Stream.of(
            "/products?page=0&size='; SELECT pg_sleep(5)--",
            "/products?page=0 AND pg_sleep(3)=0&size=20",
            "/warehouses?page=0&size=20&active=true' AND pg_sleep(3)=0--",
            "/warehouses?size=20 UNION SELECT pg_sleep(5),NULL--"
        )
    }
}
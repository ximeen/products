package com.ximenes.products.security.sqli

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.util.stream.Stream

class ProductsCreateSqliTest : SqliTestBase() {

    @ParameterizedTest
    @MethodSource("bodyInjectionPayloads")
    fun sqli_createProduct_bodyFields_shouldSanitizeOrReject(name: String, category: String) {
        val body = """{"name":"$name","category":"$category","defaultPrice":10.0,"sku":"TEST-${System.currentTimeMillis()}"}"""

        val result = mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andReturn()

        val status = result.response.status

        kotlin.test.assertTrue(
            status in listOf(201, 400),
            "Payload: name=$name - Expected 201 or 400, got $status"
        )

        val responseBody = result.response.contentAsString
        kotlin.test.assertTrue(
            !checkForInternalErrors(responseBody),
            "Response should not expose internal errors"
        )
    }

    companion object {
        @JvmStatic
        fun bodyInjectionPayloads(): Stream<Arguments> = Stream.of(
            Arguments.of("produto'; DROP TABLE products;--", "test"),
            Arguments.of("produto' OR '1'='1", "test"),
            Arguments.of("normal", "' UNION SELECT username,password FROM users--"),
            Arguments.of("test'--", "electronics")
        )
    }
}
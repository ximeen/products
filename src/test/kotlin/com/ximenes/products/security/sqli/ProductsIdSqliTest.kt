package com.ximenes.products.security.sqli

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

class ProductsIdSqliTest : SqliTestBase() {

    @Test
    fun sqli_getProductById_pathVariable_shouldRejectInjection() {
        val maliciousPaths = listOf(
            "' OR '1'='1",
            "1 OR 1=1--",
            "1; DROP TABLE products--",
            "1 UNION SELECT username,password FROM users--"
        )

        for (path in maliciousPaths) {
            val result = mockMvc.get("/products/$path").andReturn()
            val status = result.response.status
            val body = result.response.contentAsString

            kotlin.test.assertTrue(
                status in listOf(400, 404),
                "Expected 400 or 404 for payload: $path, got $status"
            )

            kotlin.test.assertTrue(
                !checkForInternalErrors(body),
                "Body should not expose internal error details for payload: $path"
            )
        }
    }
}
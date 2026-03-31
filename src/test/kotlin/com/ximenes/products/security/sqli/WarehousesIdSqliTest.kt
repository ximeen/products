package com.ximenes.products.security.sqli

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.servlet.get

class WarehousesIdSqliTest : SqliTestBase() {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "' OR '1'='1",
            "1 OR 1=1--",
            "1 UNION SELECT username,password FROM users--",
            "1; DROP TABLE warehouses--"
        ]
    )
    fun sqli_getWarehouseById_shouldRejectInjection(payload: String) {
        val result = mockMvc.get("/warehouses/$payload").andReturn()

        val status = result.response.status
        val body = result.response.contentAsString

        kotlin.test.assertTrue(
            status in listOf(400, 404),
            "Expected 400 or 404 for payload: $payload, got $status"
        )

        kotlin.test.assertTrue(
            !checkForInternalErrors(body),
            "Body should not expose internal error details for payload: $payload"
        )
    }
}
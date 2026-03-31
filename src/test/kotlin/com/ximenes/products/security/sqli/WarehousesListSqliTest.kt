package com.ximenes.products.security.sqli

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.servlet.get

class WarehousesListSqliTest : SqliTestBase() {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "true' OR '1'='1'--",
            "1 UNION SELECT name,NULL FROM warehouses--",
            "' OR 1=1--",
            "true; DROP TABLE warehouses--",
            "not_a_boolean"
        ]
    )
    fun sqli_listWarehouses_activeParam_shouldValidateBoolean(payload: String) {
        val result = mockMvc.get("/warehouses") {
            param("active", payload)
        }.andReturn()

        kotlin.test.assertEquals(
            400, result.response.status,
            "Invalid type should be rejected as 400"
        )

        val body = result.response.contentAsString
        kotlin.test.assertTrue(
            !checkForInternalErrors(body),
            "Body should not expose internal error details. Got: $body"
        )
    }
}
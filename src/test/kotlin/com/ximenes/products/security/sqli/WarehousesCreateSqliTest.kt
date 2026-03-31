package com.ximenes.products.security.sqli

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.util.stream.Stream

class WarehousesCreateSqliTest : SqliTestBase() {

    @ParameterizedTest
    @MethodSource("warehouseBodyPayloads")
    fun sqli_createWarehouse_bodyFields_shouldSanitizeOrReject(name: String) {
        val body = """{"name":"$name","address":"São Paulo","active":true}"""

        val result = mockMvc.post("/warehouses") {
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
        fun warehouseBodyPayloads(): Stream<String> = Stream.of(
            "CD São Paulo'; DROP TABLE warehouses;--",
            "CD' OR '1'='1",
            "'; INSERT INTO warehouses (name) VALUES ('hacked');--",
            "CD' UNION SELECT username,password FROM users--"
        )
    }
}
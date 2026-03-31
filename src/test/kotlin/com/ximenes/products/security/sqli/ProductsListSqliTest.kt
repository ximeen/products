package com.ximenes.products.security.sqli

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.servlet.get

class ProductsListSqliTest : SqliTestBase() {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "' OR '1'='1",
            "electronics' OR '1'='1'--",
            "1 UNION SELECT table_name,NULL FROM information_schema.tables--",
            "1 AND SUBSTRING((SELECT version()),1,1)='P'--"
        ]
    )
    fun sqli_listProducts_categoryParam_shouldNotInject(payload: String) {
        val result = mockMvc.get("/products") {
            param("category", payload)
        }.andReturn()

        val status = result.response.status
        val body = result.response.contentAsString

        kotlin.test.assertTrue(
            !checkForInternalErrors(body),
            "Body should not expose internal error details for payload: $payload"
        )

        if (status == 200) {
            val hasProducts = body.contains("\"products\":[") || body.contains("\"products\": [")
            val isEmpty = body.contains("\"products\":[]") || body.contains("\"products\": []")
            kotlin.test.assertTrue(
                isEmpty || !hasProducts,
                "Payload should not return real data. Got: $body"
            )
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "ACTIVE' OR '1'='1'--",
            "' UNION SELECT username,password FROM users--",
            "ACTIVE; DROP TABLE products--",
            "INVALID_VALUE"
        ]
    )
    fun sqli_listProducts_statusParam_shouldValidateEnum(payload: String) {
        mockMvc.get("/products") {
            param("status", payload)
        }.andExpect { org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest }
    }

    @org.junit.jupiter.api.Test
    fun sqli_listProducts_combinedParams_shouldNotInject() {
        mockMvc.get("/products") {
            param("category", "electronics")
            param("status", "ACTIVE' OR '1'='1'--")
        }.andExpect { org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest }

        val result = mockMvc.get("/products") {
            param("category", "' OR 1=1--")
            param("status", "ACTIVE")
        }.andReturn()

        val body = result.response.contentAsString
        kotlin.test.assertTrue(
            !checkForInternalErrors(body),
            "Body should not expose internal error details"
        )
    }
}
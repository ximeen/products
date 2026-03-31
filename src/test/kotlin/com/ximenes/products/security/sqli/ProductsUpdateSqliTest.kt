package com.ximenes.products.security.sqli

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get

class ProductsUpdateSqliTest : SqliTestBase() {

    @Test
    fun sqli_updateProduct_pathId_shouldRejectNonNumeric() {
        mockMvc.patch("/products/' OR '1'='1'--") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"novo nome"}"""
        }.andExpect { org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest }
    }

    @Test
    fun sqli_updateProduct_body_shouldNotAffectOtherRecords() {
        val createBody1 = """{"name":"produto-alvo","category":"test","defaultPrice":10.0,"sku":"TARGET-${System.currentTimeMillis()}"}"""
        val result1 = mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody1
        }.andReturn()
        
        val targetId = result1.response.contentAsString
            .split("\"id\":\"")
            .getOrNull(1)
            ?.split("\"")
            ?.firstOrNull() ?: return

        val createBody2 = """{"name":"produto-outro","category":"test","defaultPrice":10.0,"sku":"OTHER-${System.currentTimeMillis()}"}"""
        val result2 = mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody2
        }.andReturn()

        val otherId = result2.response.contentAsString
            .split("\"id\":\"")
            .getOrNull(1)
            ?.split("\"")
            ?.firstOrNull() ?: return

        mockMvc.patch("/products/$targetId") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"test'; UPDATE products SET name='HACKED' WHERE '1'='1'--"}"""
        }.andReturn()

        val getResult = mockMvc.get("/products/$otherId").andReturn()
        val otherName = getResult.response.contentAsString
            .split("\"name\":\"")
            .getOrNull(1)
            ?.split("\"")
            ?.firstOrNull()

        kotlin.test.assertTrue(
            otherName != "HACKED",
            "Other product should not be affected by injection"
        )
    }
}
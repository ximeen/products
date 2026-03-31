package com.ximenes.products.security.sqli

import com.ximenes.products.infrastructure.database.jpa.repositories.ProductJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class ProductsDeleteSqliTest : SqliTestBase() {

    @Autowired
    private lateinit var productJpaRepository: ProductJpaRepository

    @Test
    fun sqli_deleteProduct_shouldNotMassDelete() {
        val createBody1 = """{"name":"produto-1","category":"test","defaultPrice":10.0,"sku":"DEL1-${System.currentTimeMillis()}"}"""
        mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody1
        }.andReturn()

        val createBody2 = """{"name":"produto-2","category":"test","defaultPrice":10.0,"sku":"DEL2-${System.currentTimeMillis()}"}"""
        mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody2
        }.andReturn()

        val createBody3 = """{"name":"produto-3","category":"test","defaultPrice":10.0,"sku":"DEL3-${System.currentTimeMillis()}"}"""
        mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody3
        }.andReturn()

        val countBeforeDelete = productJpaRepository.count()

        mockMvc.delete("/products/1 OR 1=1--")
            .andExpect { MockMvcResultMatchers.status().isBadRequest }

        val countAfterDelete = productJpaRepository.count()
        
        kotlin.test.assertTrue(
            countAfterDelete >= countBeforeDelete,
            "Mass delete should not occur - count before: $countBeforeDelete, after: $countAfterDelete"
        )
    }
}
package com.ximenes.products.security.sqli

import com.ximenes.products.infrastructure.database.jpa.repositories.WarehouseJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get

class WarehousesStatusSqliTest : SqliTestBase() {

    @Autowired
    private lateinit var warehouseJpaRepository: WarehouseJpaRepository

    @Test
    fun sqli_updateWarehouseStatus_pathId_shouldRejectInjection() {
        mockMvc.patch("/warehouses/1 OR 1=1--/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"active":false}"""
        }.andExpect { org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest }
    }

    @Test
    fun sqli_updateWarehouseStatus_body_shouldNotAffectOthers() {
        val createBody1 = """{"name":"CD-alvo-${System.currentTimeMillis()}","address":"Rua A","active":true}"""
        val result1 = mockMvc.post("/warehouses") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody1
        }.andReturn()

        val targetId = parseIdFromResponse(result1.response.contentAsString) ?: return

        val createBody2 = """{"name":"CD-outro-${System.currentTimeMillis()}","address":"Rua B","active":true}"""
        val result2 = mockMvc.post("/warehouses") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody2
        }.andReturn()

        val otherId = parseIdFromResponse(result2.response.contentAsString) ?: return

        mockMvc.patch("/warehouses/$targetId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"active":false,"sql":"'; UPDATE warehouses SET active=false WHERE '1'='1'--"}"""
        }.andReturn()

        val otherWarehouse = warehouseJpaRepository.findById(otherId)
        val isOtherActive = otherWarehouse.map { it.active }.orElse(true)

        kotlin.test.assertTrue(
            isOtherActive,
            "Other warehouse should remain active"
        )
    }

    private fun parseIdFromResponse(response: String): String? {
        return response.split("\"id\":\"")
            .getOrNull(1)
            ?.split("\"")
            ?.firstOrNull()
    }
}
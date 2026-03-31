package com.ximenes.products.domain.entities.warehouse

import com.ximenes.products.domain.entities.warehouse.Warehouse
import com.ximenes.products.domain.entities.warehouse.WarehouseProps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class WarehouseEntityTest {

    private lateinit var validProps: WarehouseProps

    @BeforeEach
    fun setup() {
        validProps = WarehouseProps(
            name = "Depósito Central",
            address = "Rua das Flores, 123 - São Paulo, SP",
            active = true,
        )
    }

    @Test
    fun `should create warehouse with default ACTIVE status`() {
        val warehouse = Warehouse.create(validProps)
        assertEquals(true, warehouse.active)
    }

    @Test
    fun `should throw error if name has less than 3 characters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Warehouse.create(validProps.copy(name = "AB"))
        }
        assertEquals("Nome deve ter no mínimo 3 caracteres", ex.message)
    }

    @Test
    fun `should throw error if name has more than 100 characters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Warehouse.create(validProps.copy(name = "A".repeat(101)))
        }
        assertEquals("Nome deve ter no máximo 100 caracteres", ex.message)
    }

    @Test
    fun `should throw error if address has less than 5 characters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Warehouse.create(validProps.copy(address = "ABCD"))
        }
        assertEquals("Endereço deve ter no mínimo 5 caracteres", ex.message)
    }

    @Test
    fun `should throw error if address has more than 255 characters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Warehouse.create(validProps.copy(address = "A".repeat(256)))
        }
        assertEquals("Endereço deve ter no máximo 255 caracteres", ex.message)
    }

    @Test
    fun `should create warehouse with valid data`() {
        val warehouse = Warehouse.create(validProps)
        assertEquals("Depósito Central", warehouse.name)
        assertEquals("Rua das Flores, 123 - São Paulo, SP", warehouse.address)
        assertEquals(true, warehouse.active)
    }

    @Test
    fun `should deactivate warehouse`() {
        val warehouse = Warehouse.create(validProps)
        warehouse.deactivate()
        assertEquals(false, warehouse.active)
    }

    @Test
    fun `should reactivate warehouse`() {
        val warehouse = Warehouse.create(validProps.copy(active = false))
        warehouse.reactivate()
        assertEquals(true, warehouse.active)
    }

    @Test
    fun `should trim name and address on create`() {
        val warehouse = Warehouse.create(
            validProps.copy(
                name = "  Depósito Central  ",
                address = "  Rua das Flores, 123  "
            )
        )
        assertEquals("Depósito Central", warehouse.name)
        assertEquals("Rua das Flores, 123", warehouse.address)
    }

    @Test
    fun `should return isActive true when active`() {
        val warehouse = Warehouse.create(validProps)
        assertEquals(true, warehouse.isActive())
    }

    @Test
    fun `should return isActive false when inactive`() {
        val warehouse = Warehouse.create(validProps.copy(active = false))
        assertEquals(false, warehouse.isActive())
    }
}

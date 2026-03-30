package com.ximenes.products.domain.entities.product.value_objects

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SkuTest {

    @Test
    fun `should create sku with valid format`() {
        val sku = Sku.create("CAN-001")
        assertEquals("CAN-001", sku.getValue())
    }

    @Test
    fun `should throw error if sku is empty`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Sku.create("")
        }
        assertEquals("SKU é obrigatório", ex.message)
    }

    @Test
    fun `should throw error if sku has invalid format - lowercase`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Sku.create("can-001")
        }
        assertEquals("SKU deve seguir o formato ABC-1234 (letras maiúsculas, hífen, números)", ex.message)
    }

    @Test
    fun `should throw error if sku has invalid format - no hyphen`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Sku.create("CAN001")
        }
        assertEquals("SKU deve seguir o formato ABC-1234 (letras maiúsculas, hífen, números)", ex.message)
    }

    @Test
    fun `should throw error if sku has less than 2 letters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Sku.create("A-001")
        }
        assertEquals("SKU deve seguir o formato ABC-1234 (letras maiúsculas, hífen, números)", ex.message)
    }

    @Test
    fun `should throw error if sku has more than 10 letters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Sku.create("CANTTEXTOOO-001")
        }
        assertEquals("SKU deve seguir o formato ABC-1234 (letras maiúsculas, hífen, números)", ex.message)
    }

    @Test
    fun `should throw error if sku has more than 6 numbers`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Sku.create("CAN-1234567")
        }
        assertEquals("SKU deve seguir o formato ABC-1234 (letras maiúsculas, hífen, números)", ex.message)
    }

}

package com.ximenes.products.domain.entities.product

import com.ximenes.products.domain.entities.product.value_objects.Sku
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertFailsWith

class ProductEntityTest {

    private lateinit var validProps: ProductProps

    @BeforeEach
    fun setup() {
        validProps = ProductProps(
            name = "Caneta Esferográfica Azul",
            description = "Caneta ponta média 1.0mm",
            sku = Sku.create("CAN-001"),
            category = "Papelaria",
            defaultPrice = BigDecimal("2.50"),
            status = ProductStatus.ACTIVE,
        )
    }

    @Test
    fun `should create product with default ACTIVE status`() {
        val product = Product.create(validProps)
        assertEquals(ProductStatus.ACTIVE, product.status)
    }

    @Test
    fun `should throw error if name is empty`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Product.create(validProps.copy(name = ""))
        }
        assertEquals("Nome é obrigatório", ex.message)
    }

    @Test
    fun `should throw error if name has less than 3 characters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Product.create(validProps.copy(name = "AB"))
        }
        assertEquals("Nome deve ter no mínimo 3 caracteres", ex.message)
    }

    @Test
    fun `should throw error if name has more than 100 characters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Product.create(validProps.copy(name = "A".repeat(101)))
        }
        assertEquals("Nome deve ter no máximo 100 caracteres", ex.message)
    }

    @Test
    fun `should throw error if description has more than 500 characters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Product.create(validProps.copy(description = "A".repeat(501)))
        }
        assertEquals("Descrição deve ter no máximo 500 caracteres", ex.message)
    }

    @Test
    fun `should throw error if category has more than 50 characters`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Product.create(validProps.copy(category = "A".repeat(51)))
        }
        assertEquals("Categoria deve ter no máximo 50 caracteres", ex.message)
    }

    @Test
    fun `should throw error if defaultPrice is zero`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Product.create(validProps.copy(defaultPrice = BigDecimal.ZERO))
        }
        assertEquals("Preço padrão deve ser maior que zero", ex.message)
    }

    @Test
    fun `should throw error if defaultPrice is negative`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Product.create(validProps.copy(defaultPrice = BigDecimal("-1.00")))
        }
        assertEquals("Preço padrão não pode ser negativo", ex.message)
    }

    @Test
    fun `should create product with valid data`() {
        val product = Product.create(validProps)
        assertEquals("Caneta Esferográfica Azul", product.name)
        assertEquals("CAN-001", product.sku.getValue())
        assertEquals(BigDecimal("2.50"), product.defaultPrice)
    }

    @Test
    fun `should inactivate product`() {
        val product = Product.create(validProps)
        val inactivated = product.inactivate()
        assertEquals(ProductStatus.INACTIVE, inactivated.status)
    }

    @Test
    fun `should activate product`() {
        val product = Product.create(validProps.copy(status = ProductStatus.INACTIVE))
        val activated = product.activate()
        assertEquals(ProductStatus.ACTIVE, activated.status)
    }
}

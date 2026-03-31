package com.ximenes.products.application.use_cases.product

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.product.Product
import com.ximenes.products.domain.entities.product.ProductStatus
import com.ximenes.products.domain.entities.product.value_objects.Price
import com.ximenes.products.domain.entities.product.value_objects.Sku
import com.ximenes.products.shared.errors.ConflictError
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertFailsWith

class CreateProductTest {

    private lateinit var productRepo: IProductRepository

    @BeforeEach
    fun setup() {
        productRepo = mockk()
    }

    @Test
    fun `should create product successfully`() {
        every { productRepo.findByName(any()) } returns null
        every { productRepo.findBySku(any()) } returns null
        every { productRepo.save(any()) } returns Unit

        val useCase = CreateProductUseCase(productRepo)
        val result = useCase.execute(
            CreateProductInput(
                name = "Caneta Esferográfica",
                description = "Caneta azul",
                sku = "CAN-001",
                category = "Papelaria",
                defaultPrice = BigDecimal("2.50")
            )
        )

        assertNotNull(result.id)
        assertEquals("Caneta Esferográfica", result.name)
        assertEquals("CAN-001", result.sku)
        verify(exactly = 1) { productRepo.save(any()) }
    }

    @Test
    fun `should throw ConflictError if name already exists`() {
        every { productRepo.findByName("Caneta Existente") } returns Product.create(
            com.ximenes.products.domain.entities.product.ProductProps(
                name = "Caneta Existente",
                description = "Caneta",
                sku = Sku.create("CAN-000"),
                category = "Papelaria",
                defaultPrice = Price.create(BigDecimal("1.00")),
                status = ProductStatus.ACTIVE
            )
        )
        every { productRepo.findBySku(any()) } returns null

        val useCase = CreateProductUseCase(productRepo)
        assertFailsWith<ConflictError> {
            useCase.execute(
                CreateProductInput(
                    name = "Caneta Existente",
                    description = "Caneta",
                    sku = "CAN-002",
                    category = "Papelaria",
                    defaultPrice = BigDecimal("2.50")
                )
            )
        }
    }

    @Test
    fun `should throw ConflictError if sku already exists`() {
        every { productRepo.findByName(any()) } returns null
        every { productRepo.findBySku("CAN-001") } returns Product.create(
            com.ximenes.products.domain.entities.product.ProductProps(
                name = "Caneta Existente",
                description = "Caneta",
                sku = Sku.create("CAN-001"),
                category = "Papelaria",
                defaultPrice = Price.create(BigDecimal("1.00")),
                status = ProductStatus.ACTIVE
            )
        )

        val useCase = CreateProductUseCase(productRepo)
        assertFailsWith<ConflictError> {
            useCase.execute(
                CreateProductInput(
                    name = "Caneta Nova",
                    description = "Caneta",
                    sku = "CAN-001",
                    category = "Papelaria",
                    defaultPrice = BigDecimal("2.50")
                )
            )
        }
    }
}

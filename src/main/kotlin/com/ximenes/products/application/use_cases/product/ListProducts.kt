package com.ximenes.products.application.use_cases.product

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.product.Product
import com.ximenes.products.domain.entities.product.ProductStatus
import com.ximenes.products.shared.errors.ValidationError
import java.math.BigDecimal

data class ListProductsInput(
    val category: String? = null,
    val status: ProductStatus? = ProductStatus.ACTIVE,
    val page: Int = 0,
    val size: Int = 20,
)

data class ListProductsOutput(
    val products: List<ProductSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class ProductSummary(
    val id: String,
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: BigDecimal,
    val status: ProductStatus,
)

class ListProductsUseCase(
    private val productRepo: IProductRepository
) {
    fun execute(input: ListProductsInput): ListProductsOutput {
        require(input.size <= 100) { "Tamanho máximo por página é 100" }

        val products = productRepo.findAll(
            category = input.category,
            status = input.status,
            page = input.page,
            size = input.size
        )

        val totalElements = productRepo.count(
            category = input.category,
            status = input.status
        )

        val totalPages = if (input.size > 0) {
            ((totalElements + input.size - 1) / input.size).toInt()
        } else {
            0
        }

        return ListProductsOutput(
            products = products.map { it.toSummary() },
            page = input.page,
            size = input.size,
            totalElements = totalElements,
            totalPages = totalPages,
        )
    }

    private fun Product.toSummary() = ProductSummary(
        id = this.id,
        name = this.name,
        description = this.description,
        sku = this.sku.getValue(),
        category = this.category,
        defaultPrice = this.defaultPrice,
        status = this.status,
    )
}

package com.ximenes.products.application.use_cases.product

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.product.Product
import com.ximenes.products.domain.entities.product.ProductStatus
import com.ximenes.products.shared.errors.NotFoundError
import java.math.BigDecimal

data class GetProductOutput(
    val id: String,
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: BigDecimal,
    val status: ProductStatus,
    val createdAt: String,
    val updatedAt: String,
)

class GetProductUseCase(
    private val productRepo: IProductRepository
) {
    fun execute(id: String): GetProductOutput {
        val product = productRepo.findById(id)
            ?: throw NotFoundError("Produto", id)

        return GetProductOutput(
            id = product.id,
            name = product.name,
            description = product.description,
            sku = product.sku.getValue(),
            category = product.category,
            defaultPrice = product.defaultPrice,
            status = product.status,
            createdAt = product.createdAt.toString(),
            updatedAt = product.updatedAt.toString(),
        )
    }
}

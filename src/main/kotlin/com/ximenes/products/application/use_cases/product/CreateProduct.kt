package com.ximenes.products.application.use_cases.product

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.product.Product
import com.ximenes.products.domain.entities.product.ProductProps
import com.ximenes.products.domain.entities.product.ProductStatus
import com.ximenes.products.domain.entities.product.value_objects.Price
import com.ximenes.products.domain.entities.product.value_objects.Sku
import com.ximenes.products.shared.errors.ConflictError
import java.math.BigDecimal

data class CreateProductInput(
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: BigDecimal,
)

data class CreateProductOutput(
    val id: String,
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: BigDecimal,
    val status: ProductStatus,
)

class CreateProductUseCase(
    private val productRepo: IProductRepository
) {
    fun execute(input: CreateProductInput): CreateProductOutput {
        val existingByName = productRepo.findByName(input.name)
        if (existingByName != null) {
            throw ConflictError("Já existe um produto com este nome")
        }

        val existingBySku = productRepo.findBySku(input.sku)
        if (existingBySku != null) {
            throw ConflictError("SKU já cadastrado no sistema")
        }

        val product = Product.create(
            ProductProps(
                name = input.name,
                description = input.description,
                sku = Sku.create(input.sku),
                category = input.category,
                defaultPrice = Price.create(input.defaultPrice),
                status = ProductStatus.ACTIVE,
            )
        )

        productRepo.save(product)

        return CreateProductOutput(
            id = product.id,
            name = product.name,
            description = product.description,
            sku = product.sku.getValue(),
            category = product.category,
            defaultPrice = product.defaultPrice.getValue(),
            status = product.status,
        )
    }
}

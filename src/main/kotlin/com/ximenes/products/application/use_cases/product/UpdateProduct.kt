package com.ximenes.products.application.use_cases.product

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.product.Product
import com.ximenes.products.domain.entities.product.ProductStatus
import com.ximenes.products.domain.entities.product.value_objects.Price
import com.ximenes.products.domain.entities.product.value_objects.Sku
import com.ximenes.products.shared.errors.ConflictError
import com.ximenes.products.shared.errors.NotFoundError
import com.ximenes.products.shared.errors.ValidationError
import java.math.BigDecimal

data class UpdateProductInput(
    val name: String? = null,
    val description: String? = null,
    val sku: String? = null,
    val category: String? = null,
    val defaultPrice: BigDecimal? = null,
    val status: ProductStatus? = null,
)

data class UpdateProductOutput(
    val id: String,
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: BigDecimal,
    val status: ProductStatus,
    val updatedAt: String,
)

class UpdateProductUseCase(
    private val productRepo: IProductRepository
) {
    fun execute(id: String, input: UpdateProductInput): UpdateProductOutput {
        if (input.name == null && input.description == null && 
            input.sku == null && input.category == null && 
            input.defaultPrice == null && input.status == null) {
            throw ValidationError("Nenhum campo informado para atualização")
        }

        val product = productRepo.findById(id)
            ?: throw NotFoundError("Produto", id)

        input.name?.let { newName ->
            val existingByName = productRepo.findByName(newName)
            if (existingByName != null && existingByName.id != id) {
                throw ConflictError("Já existe um produto com este nome")
            }
        }

        input.sku?.let { newSku ->
            val existingBySku = productRepo.findBySku(newSku)
            if (existingBySku != null && existingBySku.id != id) {
                throw ConflictError("SKU já cadastrado no sistema")
            }
        }

        val updatedProduct = product.updateWith(
            name = input.name,
            description = input.description,
            sku = input.sku?.let { Sku.create(it) },
            category = input.category,
            defaultPrice = input.defaultPrice?.let { Price.create(it) },
            status = input.status
        )

        productRepo.update(updatedProduct)

        return UpdateProductOutput(
            id = updatedProduct.id,
            name = updatedProduct.name,
            description = updatedProduct.description,
            sku = updatedProduct.sku.getValue(),
            category = updatedProduct.category,
            defaultPrice = updatedProduct.defaultPrice.getValue(),
            status = updatedProduct.status,
            updatedAt = updatedProduct.updatedAt.toString(),
        )
    }
}

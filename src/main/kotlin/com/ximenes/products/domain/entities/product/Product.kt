package com.ximenes.products.domain.entities.product

import com.ximenes.products.domain.entities.product.value_objects.Price
import com.ximenes.products.domain.entities.product.value_objects.Sku
import com.ximenes.products.domain.shared.Entity

data class ProductProps(
    val name: String,
    val description: String?,
    val sku: Sku,
    val category: String?,
    val defaultPrice: Price,
    val status: ProductStatus = ProductStatus.ACTIVE,
)

class Product private constructor(
    props: ProductProps,
    id: String? = null
) : Entity<ProductProps>(props, id) {

    val name: String get() = props.name
    val description: String? get() = props.description
    val sku: Sku get() = props.sku
    val category: String? get() = props.category
    val defaultPrice: Price get() = props.defaultPrice
    val status: ProductStatus get() = props.status

    fun updateWith(
        name: String? = null,
        description: String? = null,
        sku: Sku? = null,
        category: String? = null,
        defaultPrice: Price? = null,
        status: ProductStatus? = null,
        clearDescription: Boolean = false,
        clearCategory: Boolean = false
    ): Product {
        val newName = (name ?: this.name).trim()
        val newDescription = when {
            clearDescription -> null
            description != null -> description.trim()
            else -> this.description
        }
        val newSku = sku ?: this.sku
        val newCategory = when {
            clearCategory -> null
            category != null -> category.trim()
            else -> this.category
        }
        val newPrice = defaultPrice ?: this.defaultPrice
        val newStatus = status ?: this.status

        require(newName.isNotEmpty()) { "Nome é obrigatório" }
        require(newName.length >= 3) { "Nome deve ter no mínimo 3 caracteres" }
        require(newName.length <= 100) { "Nome deve ter no máximo 100 caracteres" }
        require(newDescription == null || newDescription.length <= 500) { "Descrição deve ter no máximo 500 caracteres" }
        require(newCategory == null || newCategory.length <= 50) { "Categoria deve ter no máximo 50 caracteres" }
        require(!newPrice.isZeroOrNegative()) { "Preço padrão deve ser maior que zero" }

        val newProps = ProductProps(
            name = newName,
            description = newDescription,
            sku = newSku,
            category = newCategory,
            defaultPrice = newPrice,
            status = newStatus
        )

        return Product(newProps, this.id).also {
            it.createdAt = this.createdAt
        }
    }

    fun inactivate(): Product = updateWith(status = ProductStatus.INACTIVE)
    fun activate(): Product = updateWith(status = ProductStatus.ACTIVE)
    fun isActive(): Boolean = props.status == ProductStatus.ACTIVE

    companion object {
        fun create(props: ProductProps, id: String? = null): Product {
            require(props.name.trim().isNotEmpty()) { "Nome é obrigatório" }
            require(props.name.trim().length >= 3) { "Nome deve ter no mínimo 3 caracteres" }
            require(props.name.trim().length <= 100) { "Nome deve ter no máximo 100 caracteres" }
            require(props.description == null || props.description.length <= 500) { "Descrição deve ter no máximo 500 caracteres" }
            require(props.category == null || props.category.length <= 50) { "Categoria deve ter no máximo 50 caracteres" }
            require(!props.defaultPrice.isZeroOrNegative()) { "Preço padrão deve ser maior que zero" }
            
            return Product(
                props = props.copy(
                    name = props.name.trim(),
                    description = props.description?.trim(),
                    category = props.category?.trim()
                ),
                id = id
            )
        }
    }
}

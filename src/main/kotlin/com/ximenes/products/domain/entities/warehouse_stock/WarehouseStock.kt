package com.ximenes.products.domain.entities.warehouse_stock

import com.ximenes.products.domain.shared.Entity

data class WarehouseStockProps(
    val productId: String,
    val warehouseId: String,
    val quantity: Int,
    val location: String,
)

class WarehouseStock private constructor(
    props: WarehouseStockProps,
    id: String? = null
) : Entity<WarehouseStockProps>(props, id) {

    val productId: String get() = props.productId
    val warehouseId: String get() = props.warehouseId
    val quantity: Int get() = props.quantity
    val location: String get() = props.location

    fun assignUpdatedAt(time: java.time.Instant) {
        this.updatedAt = time
    }

    fun updateQuantity(quantity: Int): WarehouseStock {
        require(quantity >= 0) { "Quantidade não pode ser negativa" }
        val newProps = props.copy(quantity = quantity)
        return WarehouseStock(newProps, this.id).also {
            it.createdAt = this.createdAt
        }
    }

    fun updateLocation(location: String): WarehouseStock {
        require(location.trim().isNotEmpty()) { "Localização é obrigatória" }
        val newProps = props.copy(location = location.trim())
        return WarehouseStock(newProps, this.id).also {
            it.createdAt = this.createdAt
        }
    }

    fun hasStock(): Boolean = props.quantity > 0

    companion object {
        fun create(props: WarehouseStockProps, id: String? = null): WarehouseStock {
            require(props.productId.isNotEmpty()) { "Product ID é obrigatório" }
            require(props.warehouseId.isNotEmpty()) { "Warehouse ID é obrigatório" }
            require(props.quantity >= 0) { "Quantidade não pode ser negativa" }
            require(props.location.trim().isNotEmpty()) { "Localização é obrigatória" }

            return WarehouseStock(
                props = props.copy(location = props.location.trim()),
                id = id
            )
        }
    }
}

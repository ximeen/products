package com.ximenes.products.infrastructure.database.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "warehouse_stocks")
data class WarehouseStockJpaEntity(
    @Id
    val id: String,

    @Column(name = "product_id")
    val productId: String,

    @Column(name = "warehouse_id")
    val warehouseId: String,

    val quantity: Int,

    val location: String,

    @Column(name = "updated_at")
    val updatedAt: Instant,
)

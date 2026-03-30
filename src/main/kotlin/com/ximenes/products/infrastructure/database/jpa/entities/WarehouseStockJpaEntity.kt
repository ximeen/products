package com.ximenes.products.infrastructure.database.jpa.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "warehouse_stocks")
data class WarehouseStockJpaEntity(
    @Id
    val id: String,

    val product_id: String,

    val warehouse_id: String,

    val quantity: Int,

    val location: String,

    val updated_at: Instant,
)

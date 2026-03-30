package com.ximenes.products.infrastructure.database.jpa.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "warehouses")
data class WarehouseJpaEntity(
    @Id
    val id: String,

    val name: String,

    val address: String,

    val active: Boolean,

    val created_at: Instant,

    val updated_at: Instant,
)

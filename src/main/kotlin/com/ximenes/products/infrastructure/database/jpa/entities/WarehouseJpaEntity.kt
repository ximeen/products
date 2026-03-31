package com.ximenes.products.infrastructure.database.jpa.entities

import jakarta.persistence.Column
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

    @Column(name = "created_at")
    val createdAt: Instant,

    @Column(name = "updated_at")
    val updatedAt: Instant,
)

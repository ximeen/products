package com.ximenes.products.infrastructure.database.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "products")
data class ProductJpaEntity(
    @Id
    val id: String,

    val name: String,

    val description: String?,

    val sku: String,

    val category: String?,

    val default_price: BigDecimal,

    @Column(name = "status")
    val productStatus: String,

    val created_at: Instant,

    val updated_at: Instant,
)

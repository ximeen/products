package com.ximenes.products.infrastructure.database.jpa.repositories

import com.ximenes.products.infrastructure.database.jpa.entities.WarehouseJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface WarehouseJpaRepository : JpaRepository<WarehouseJpaEntity, String> {
    fun findByName(name: String): WarehouseJpaEntity?
    fun findAllByActive(active: Boolean): List<WarehouseJpaEntity>
}

package com.ximenes.products.infrastructure.database.jpa.repositories

import com.ximenes.products.infrastructure.database.jpa.entities.WarehouseJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository

interface WarehouseJpaRepository : JpaRepository<WarehouseJpaEntity, String> {
    fun findByName(name: String): WarehouseJpaEntity?
    fun findAllByActive(active: Boolean): List<WarehouseJpaEntity>
    fun findAllByActive(active: Boolean, pageable: Pageable): Page<WarehouseJpaEntity>
}

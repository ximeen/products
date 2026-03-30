package com.ximenes.products.infrastructure.database.jpa.repositories

import com.ximenes.products.infrastructure.database.jpa.entities.WarehouseStockJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WarehouseStockJpaRepository : JpaRepository<WarehouseStockJpaEntity, String> {
    fun findByProductIdAndWarehouseId(productId: String, warehouseId: String): WarehouseStockJpaEntity?
    fun findByProductId(productId: String): List<WarehouseStockJpaEntity>
    fun findByWarehouseId(warehouseId: String): List<WarehouseStockJpaEntity>
    
    @Query("SELECT w FROM WarehouseStockJpaEntity w WHERE w.warehouseId = :warehouseId AND w.quantity > 0")
    fun findByWarehouseIdWithPositiveQuantity(@Param("warehouseId") warehouseId: String): List<WarehouseStockJpaEntity>
    
    @Query("SELECT w FROM WarehouseStockJpaEntity w WHERE w.productId = :productId AND w.quantity > 0")
    fun findByProductIdWithPositiveQuantity(@Param("productId") productId: String): List<WarehouseStockJpaEntity>
    
    fun existsByProductId(productId: String): Boolean
    
    fun existsByWarehouseId(warehouseId: String): Boolean
    
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WarehouseStockJpaEntity w WHERE w.productId = :productId AND w.quantity > 0")
    fun existsByProductIdWithPositiveQuantity(@Param("productId") productId: String): Boolean
    
    fun deleteByProductId(productId: String)
}

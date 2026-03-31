package com.ximenes.products.infrastructure.database.jpa.repositories

import com.ximenes.products.infrastructure.database.jpa.entities.ProductJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<ProductJpaEntity, String> {
    fun findByName(name: String): ProductJpaEntity?
    fun findBySku(sku: String): ProductJpaEntity?
    
    @Query("SELECT p FROM ProductJpaEntity p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:status IS NULL OR p.productStatus = :status) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    fun search(
        @Param("category") category: String?,
        @Param("status") status: String?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<ProductJpaEntity>
    
    @Query("SELECT COUNT(p) FROM ProductJpaEntity p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:status IS NULL OR p.productStatus = :status) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    fun countSearch(
        @Param("category") category: String?,
        @Param("status") status: String?,
        @Param("search") search: String?
    ): Long
}

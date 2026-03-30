package com.ximenes.products.infrastructure.database.repositories

import com.ximenes.products.domain.entities.warehouse_stock.IWarehouseStockRepository
import com.ximenes.products.domain.entities.warehouse_stock.WarehouseStock
import com.ximenes.products.domain.entities.warehouse_stock.WarehouseStockProps
import com.ximenes.products.infrastructure.database.jpa.entities.WarehouseStockJpaEntity
import com.ximenes.products.infrastructure.database.jpa.repositories.WarehouseStockJpaRepository
import org.springframework.stereotype.Component

@Component
class WarehouseStockRepositoryImpl(
    private val jpaRepository: WarehouseStockJpaRepository
) : IWarehouseStockRepository {

    override fun save(stock: WarehouseStock) {
        jpaRepository.save(stock.toJpaEntity())
    }

    override fun findById(id: String): WarehouseStock? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByProductIdAndWarehouseId(productId: String, warehouseId: String): WarehouseStock? =
        jpaRepository.findByProductIdAndWarehouseId(productId, warehouseId)?.toDomain()

    override fun findByProductId(productId: String): List<WarehouseStock> =
        jpaRepository.findByProductId(productId).map { it.toDomain() }

    override fun findByWarehouseId(warehouseId: String): List<WarehouseStock> =
        jpaRepository.findByWarehouseId(warehouseId).map { it.toDomain() }

    override fun findByWarehouseIdWithPositiveQuantity(warehouseId: String): List<WarehouseStock> =
        jpaRepository.findByWarehouseIdWithPositiveQuantity(warehouseId).map { it.toDomain() }

    override fun findByProductIdWithPositiveQuantity(productId: String): List<WarehouseStock> =
        jpaRepository.findByProductIdWithPositiveQuantity(productId).map { it.toDomain() }

    override fun update(stock: WarehouseStock) {
        jpaRepository.save(stock.toJpaEntity())
    }

    override fun delete(id: String) {
        jpaRepository.deleteById(id)
    }

    override fun deleteByProductId(productId: String) {
        jpaRepository.deleteByProductId(productId)
    }

    override fun existsByProductId(productId: String): Boolean =
        jpaRepository.existsByProductId(productId)

    override fun existsByWarehouseId(warehouseId: String): Boolean =
        jpaRepository.existsByWarehouseId(warehouseId)

    override fun existsByProductIdWithPositiveQuantity(productId: String): Boolean =
        jpaRepository.existsByProductIdWithPositiveQuantity(productId)

    private fun WarehouseStockJpaEntity.toDomain(): WarehouseStock =
        WarehouseStock.create(
            WarehouseStockProps(
                productId = this.product_id,
                warehouseId = this.warehouse_id,
                quantity = this.quantity,
                location = this.location,
            ),
            id = this.id
        ).also {
            it.assignUpdatedAt(this.updated_at)
        }

    private fun WarehouseStock.toJpaEntity(): WarehouseStockJpaEntity =
        WarehouseStockJpaEntity(
            id = this.id,
            product_id = this.productId,
            warehouse_id = this.warehouseId,
            quantity = this.quantity,
            location = this.location,
            updated_at = this.updatedAt,
        )
}

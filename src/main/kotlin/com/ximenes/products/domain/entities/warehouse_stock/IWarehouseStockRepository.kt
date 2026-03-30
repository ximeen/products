package com.ximenes.products.domain.entities.warehouse_stock

interface IWarehouseStockRepository {
    fun save(stock: WarehouseStock)
    fun findById(id: String): WarehouseStock?
    fun findByProductIdAndWarehouseId(productId: String, warehouseId: String): WarehouseStock?
    fun findByProductId(productId: String): List<WarehouseStock>
    fun findByWarehouseId(warehouseId: String): List<WarehouseStock>
    fun findByWarehouseIdWithPositiveQuantity(warehouseId: String): List<WarehouseStock>
    fun findByProductIdWithPositiveQuantity(productId: String): List<WarehouseStock>
    fun update(stock: WarehouseStock)
    fun delete(id: String)
    fun deleteByProductId(productId: String)
    fun existsByProductId(productId: String): Boolean
    fun existsByWarehouseId(warehouseId: String): Boolean
    fun existsByProductIdWithPositiveQuantity(productId: String): Boolean
}

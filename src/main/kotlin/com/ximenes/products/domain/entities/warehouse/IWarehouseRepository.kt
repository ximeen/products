package com.ximenes.products.domain.entities.warehouse

interface IWarehouseRepository {
    fun save(warehouse: Warehouse)
    fun findById(id: String): Warehouse?
    fun findByName(name: String): Warehouse?
    fun findAll(page: Int, size: Int, active: Boolean?): List<Warehouse>
    fun findAll(active: Boolean?): List<Warehouse>
    fun update(warehouse: Warehouse)
    fun delete(id: String)
    fun exists(id: String): Boolean
}

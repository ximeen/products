package com.ximenes.products.domain.entities.product

interface IProductRepository {
    fun save(product: Product)
    fun findById(id: String): Product?
    fun findByName(name: String): Product?
    fun findBySku(sku: String): Product?
    fun findAll(category: String?, status: ProductStatus?, page: Int, size: Int): List<Product>
    fun update(product: Product)
    fun delete(id: String)
    fun exists(id: String): Boolean
    fun count(category: String?, status: ProductStatus?): Long
}

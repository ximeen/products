package com.ximenes.products.infrastructure.database.repositories

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.product.Product
import com.ximenes.products.domain.entities.product.ProductProps
import com.ximenes.products.domain.entities.product.ProductStatus
import com.ximenes.products.domain.entities.product.value_objects.Price
import com.ximenes.products.domain.entities.product.value_objects.Sku
import com.ximenes.products.infrastructure.database.jpa.entities.ProductJpaEntity
import com.ximenes.products.infrastructure.database.jpa.repositories.ProductJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val jpaRepository: ProductJpaRepository
) : IProductRepository {

    override fun save(product: Product) {
        jpaRepository.save(product.toJpaEntity())
    }

    override fun findById(id: String): Product? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByName(name: String): Product? =
        jpaRepository.findByName(name)?.toDomain()

    override fun findBySku(sku: String): Product? =
        jpaRepository.findBySku(sku)?.toDomain()

    override fun findAll(
        category: String?,
        status: ProductStatus?,
        search: String?,
        page: Int,
        size: Int
    ): List<Product> =
        jpaRepository.search(
            category,
            status?.name,
            search,
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        ).content.map { it.toDomain() }

    override fun update(product: Product) {
        jpaRepository.save(product.toJpaEntity())
    }

    override fun delete(id: String) {
        jpaRepository.deleteById(id)
    }

    override fun exists(id: String): Boolean =
        jpaRepository.existsById(id)

    override fun count(category: String?, status: ProductStatus?, search: String?): Long =
        jpaRepository.countSearch(category, status?.name, search)

    private fun ProductJpaEntity.toDomain(): Product =
        Product.create(
            ProductProps(
                name = this.name,
                description = this.description,
                sku = Sku.create(this.sku),
                category = this.category,
                defaultPrice = Price.from(this.default_price),
                status = ProductStatus.valueOf(this.productStatus),
            ),
            id = this.id
        )

    private fun Product.toJpaEntity(): ProductJpaEntity =
        ProductJpaEntity(
            id = this.id,
            name = this.name,
            description = this.description,
            sku = this.sku.getValue(),
            category = this.category,
            default_price = this.defaultPrice.getValue(),
            productStatus = this.status.name,
            created_at = this.createdAt,
            updated_at = this.updatedAt,
        )
}

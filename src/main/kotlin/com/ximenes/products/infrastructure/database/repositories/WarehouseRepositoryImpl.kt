package com.ximenes.products.infrastructure.database.repositories

import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.domain.entities.warehouse.Warehouse
import com.ximenes.products.domain.entities.warehouse.WarehouseProps
import com.ximenes.products.infrastructure.database.jpa.entities.WarehouseJpaEntity
import com.ximenes.products.infrastructure.database.jpa.repositories.WarehouseJpaRepository
import org.springframework.stereotype.Component

@Component
class WarehouseRepositoryImpl(
    private val jpaRepository: WarehouseJpaRepository
) : IWarehouseRepository {

    override fun save(warehouse: Warehouse) {
        jpaRepository.save(warehouse.toJpaEntity())
    }

    override fun findById(id: String): Warehouse? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByName(name: String): Warehouse? =
        jpaRepository.findByName(name)?.toDomain()

    override fun findAll(active: Boolean?): List<Warehouse> =
        when (active) {
            null -> jpaRepository.findAll().map { it.toDomain() }
            else -> jpaRepository.findAllByActive(active).map { it.toDomain() }
        }

    override fun update(warehouse: Warehouse) {
        jpaRepository.save(warehouse.toJpaEntity())
    }

    override fun delete(id: String) {
        jpaRepository.deleteById(id)
    }

    override fun exists(id: String): Boolean =
        jpaRepository.existsById(id)

    private fun WarehouseJpaEntity.toDomain(): Warehouse =
        Warehouse.create(
            WarehouseProps(
                name = this.name,
                address = this.address,
                active = this.active,
            ),
            id = this.id
        ).also {
            it.assignCreatedAt(this.created_at)
            it.assignUpdatedAt(this.updated_at)
        }

    private fun Warehouse.toJpaEntity(): WarehouseJpaEntity =
        WarehouseJpaEntity(
            id = this.id,
            name = this.name,
            address = this.address,
            active = this.active,
            created_at = this.createdAt,
            updated_at = this.updatedAt,
        )
}

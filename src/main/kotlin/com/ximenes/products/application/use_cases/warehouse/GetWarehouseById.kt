package com.ximenes.products.application.use_cases.warehouse

import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.shared.errors.NotFoundError
import org.springframework.stereotype.Component

data class GetWarehouseByIdOutput(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant,
)

@Component
class GetWarehouseByIdUseCase(
    private val warehouseRepo: IWarehouseRepository
) {
    fun execute(id: String): GetWarehouseByIdOutput {
        val warehouse = warehouseRepo.findById(id)
            ?: throw NotFoundError("Depósito", id)

        return GetWarehouseByIdOutput(
            id = warehouse.id,
            name = warehouse.name,
            address = warehouse.address,
            active = warehouse.active,
            createdAt = warehouse.createdAt,
            updatedAt = warehouse.updatedAt,
        )
    }
}

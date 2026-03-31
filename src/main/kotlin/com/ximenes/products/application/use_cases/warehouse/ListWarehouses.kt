package com.ximenes.products.application.use_cases.warehouse

import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import org.springframework.stereotype.Component

data class ListWarehousesInput(
    val page: Int = 0,
    val size: Int = 20,
    val active: Boolean? = true,
)

data class ListWarehousesOutput(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
)

@Component
class ListWarehousesUseCase(
    private val warehouseRepo: IWarehouseRepository
) {
    fun execute(input: ListWarehousesInput): List<ListWarehousesOutput> {
        val warehouses = warehouseRepo.findAll(input.page, input.size, input.active)
        return warehouses.map { warehouse ->
            ListWarehousesOutput(
                id = warehouse.id,
                name = warehouse.name,
                address = warehouse.address,
                active = warehouse.active,
            )
        }
    }
}

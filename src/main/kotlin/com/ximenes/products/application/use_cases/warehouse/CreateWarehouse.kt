package com.ximenes.products.application.use_cases.warehouse

import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.domain.entities.warehouse.Warehouse
import com.ximenes.products.domain.entities.warehouse.WarehouseProps
import com.ximenes.products.shared.errors.ConflictError
import com.ximenes.products.shared.errors.ErrorCodes
import org.springframework.stereotype.Component

data class CreateWarehouseInput(
    val name: String,
    val address: String,
)

data class CreateWarehouseOutput(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
)

@Component
class CreateWarehouseUseCase(
    private val warehouseRepo: IWarehouseRepository
) {
    fun execute(input: CreateWarehouseInput): CreateWarehouseOutput {
        val existingByName = warehouseRepo.findByName(input.name.trim())
        if (existingByName != null) {
            throw ConflictError("Já existe um depósito com este nome", code = ErrorCodes.WAREHOUSE_NAME_ALREADY_EXISTS)
        }

        val warehouse = Warehouse.create(
            WarehouseProps(
                name = input.name.trim(),
                address = input.address.trim(),
                active = true,
            )
        )

        warehouseRepo.save(warehouse)

        return CreateWarehouseOutput(
            id = warehouse.id,
            name = warehouse.name,
            address = warehouse.address,
            active = warehouse.active,
        )
    }
}

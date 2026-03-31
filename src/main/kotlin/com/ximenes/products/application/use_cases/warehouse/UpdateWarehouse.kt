package com.ximenes.products.application.use_cases.warehouse

import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.domain.entities.warehouse.Warehouse
import com.ximenes.products.domain.entities.warehouse.WarehouseProps
import com.ximenes.products.shared.errors.NotFoundError
import com.ximenes.products.shared.errors.ValidationError
import org.springframework.stereotype.Component

data class UpdateWarehouseInput(
    val name: String? = null,
    val address: String? = null,
)

data class UpdateWarehouseOutput(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
)

@Component
class UpdateWarehouseUseCase(
    private val warehouseRepo: IWarehouseRepository
) {
    fun execute(id: String, input: UpdateWarehouseInput): UpdateWarehouseOutput {
        if (input.name == null && input.address == null) {
            throw ValidationError("Nenhum campo informado para atualização")
        }

        val warehouse = warehouseRepo.findById(id)
            ?: throw NotFoundError("Depósito", id)

        if (input.name != null) {
            val existingWithName = warehouseRepo.findByName(input.name.trim())
            if (existingWithName != null && existingWithName.id != id) {
                throw com.ximenes.products.shared.errors.ConflictError("Já existe um depósito com este nome")
            }
        }

        val newName = input.name?.trim() ?: warehouse.name
        val newAddress = input.address?.trim() ?: warehouse.address

        val updatedWarehouse = Warehouse.create(
            WarehouseProps(
                name = newName,
                address = newAddress,
                active = warehouse.active,
            ),
            id = warehouse.id
        ).also {
            it.assignCreatedAt(warehouse.createdAt)
        }

        warehouseRepo.update(updatedWarehouse)

        return UpdateWarehouseOutput(
            id = updatedWarehouse.id,
            name = updatedWarehouse.name,
            address = updatedWarehouse.address,
            active = updatedWarehouse.active,
        )
    }
}

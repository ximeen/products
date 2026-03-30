package com.ximenes.products.application.use_cases.warehouse

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.domain.entities.warehouse.Warehouse
import com.ximenes.products.domain.entities.warehouse_stock.IWarehouseStockRepository
import com.ximenes.products.domain.entities.warehouse_stock.WarehouseStock
import com.ximenes.products.shared.errors.ConflictError
import com.ximenes.products.shared.errors.NotFoundError
import com.ximenes.products.shared.errors.ValidationError

data class UpdateWarehouseInput(
    val name: String? = null,
    val address: String? = null,
    val active: Boolean? = null,
)

data class UpdateWarehouseOutput(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
)

data class ProductWithStock(
    val productId: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val location: String,
)

data class WarehouseConflictDetails(
    val warehouseId: String,
    val warehouseName: String,
    val productsWithStock: List<ProductWithStock>,
)

class UpdateWarehouseUseCase(
    private val warehouseRepo: IWarehouseRepository,
    private val stockRepo: IWarehouseStockRepository,
    private val productRepo: IProductRepository
) {
    fun execute(id: String, input: UpdateWarehouseInput): UpdateWarehouseOutput {
        if (input.name == null && input.address == null && input.active == null) {
            throw ValidationError("Nenhum campo informado para atualização")
        }

        val warehouse = warehouseRepo.findById(id)
            ?: throw NotFoundError("Depósito", id)

        if (input.active == false && warehouse.active) {
            val stocksWithQuantity = stockRepo.findByWarehouseIdWithPositiveQuantity(id)
            
            if (stocksWithQuantity.isNotEmpty()) {
                val productsWithStock = stocksWithQuantity.mapNotNull { stock ->
                    val product = productRepo.findById(stock.productId)
                    product?.let {
                        ProductWithStock(
                            productId = it.id,
                            productName = it.name,
                            sku = it.sku.getValue(),
                            quantity = stock.quantity,
                            location = stock.location,
                        )
                    }
                }

                throw ConflictError(
                    "Estoques não podem ser inativados quando há produtos nele, defina um lugar para esses produtos",
                    details = WarehouseConflictDetails(
                        warehouseId = warehouse.id,
                        warehouseName = warehouse.name,
                        productsWithStock = productsWithStock,
                    )
                )
            }
        }

        val updatedWarehouse = warehouse.updateWith(
            name = input.name,
            address = input.address,
            active = input.active
        )

        warehouseRepo.update(updatedWarehouse)

        return UpdateWarehouseOutput(
            id = updatedWarehouse.id,
            name = updatedWarehouse.name,
            address = updatedWarehouse.address,
            active = updatedWarehouse.active,
        )
    }
}

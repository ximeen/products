package com.ximenes.products.application.use_cases.warehouse

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.domain.entities.warehouse.Warehouse
import com.ximenes.products.domain.entities.warehouse_stock.IWarehouseStockRepository
import com.ximenes.products.shared.errors.ConflictError
import com.ximenes.products.shared.errors.NotFoundError
import org.springframework.stereotype.Component

data class ChangeWarehouseStatusInput(
    val active: Boolean,
)

data class ChangeWarehouseStatusOutput(
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

@Component
class ChangeWarehouseStatusUseCase(
    private val warehouseRepo: IWarehouseRepository,
    private val stockRepo: IWarehouseStockRepository,
    private val productRepo: IProductRepository
) {
    fun execute(id: String, input: ChangeWarehouseStatusInput): ChangeWarehouseStatusOutput {
        val warehouse = warehouseRepo.findById(id)
            ?: throw NotFoundError("Depósito", id)

        if (!input.active && warehouse.active) {
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

        if (input.active) {
            warehouse.reactivate()
        } else {
            warehouse.deactivate()
        }

        warehouseRepo.update(warehouse)

        return ChangeWarehouseStatusOutput(
            id = warehouse.id,
            name = warehouse.name,
            address = warehouse.address,
            active = warehouse.active,
        )
    }
}

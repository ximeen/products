package com.ximenes.products.infrastructure.http.spring.controllers

import com.ximenes.products.application.use_cases.warehouse.UpdateWarehouseInput
import com.ximenes.products.application.use_cases.warehouse.UpdateWarehouseOutput
import com.ximenes.products.application.use_cases.warehouse.UpdateWarehouseUseCase
import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.domain.entities.warehouse_stock.IWarehouseStockRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/warehouses")
class WarehouseController(
    private val updateWarehouseUseCase: UpdateWarehouseUseCase,
    private val warehouseRepo: IWarehouseRepository,
    private val stockRepo: IWarehouseStockRepository,
    private val productRepo: IProductRepository
) {

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody body: UpdateWarehouseRequest
    ): UpdateWarehouseResponse {
        val result = updateWarehouseUseCase.execute(id, body.toInput())
        return UpdateWarehouseResponse.fromOutput(result)
    }
}

data class UpdateWarehouseRequest(
    val name: String? = null,
    val address: String? = null,
    val active: Boolean? = null
) {
    fun toInput() = UpdateWarehouseInput(
        name = name,
        address = address,
        active = active
    )
}

data class UpdateWarehouseResponse(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean
) {
    companion object {
        fun fromOutput(output: UpdateWarehouseOutput) = UpdateWarehouseResponse(
            id = output.id,
            name = output.name,
            address = output.address,
            active = output.active
        )
    }
}

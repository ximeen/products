package com.ximenes.products.infrastructure.http.spring.controllers

import com.ximenes.products.application.use_cases.warehouse.*
import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.domain.entities.warehouse_stock.IWarehouseStockRepository
import com.ximenes.products.shared.errors.ValidationError
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/warehouses")
class WarehouseController(
    private val createWarehouseUseCase: CreateWarehouseUseCase,
    private val listWarehousesUseCase: ListWarehousesUseCase,
    private val getWarehouseByIdUseCase: GetWarehouseByIdUseCase,
    private val updateWarehouseUseCase: UpdateWarehouseUseCase,
    private val changeWarehouseStatusUseCase: ChangeWarehouseStatusUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @Valid body: CreateWarehouseRequest): CreateWarehouseResponse {
        val result = createWarehouseUseCase.execute(body.toInput())
        return CreateWarehouseResponse.fromOutput(result)
    }

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "true") active: Boolean?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ): List<ListWarehouseResponse> {
        if (size > 100) {
            throw ValidationError("Tamanho máximo por página é 100")
        }
        val result = listWarehousesUseCase.execute(ListWarehousesInput(page, size, active))
        return result.map { ListWarehouseResponse.fromOutput(it) }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): GetWarehouseByIdResponse {
        val result = getWarehouseByIdUseCase.execute(id)
        return GetWarehouseByIdResponse.fromOutput(result)
    }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody body: UpdateWarehouseRequest
    ): UpdateWarehouseResponse {
        val result = updateWarehouseUseCase.execute(id, body.toInput())
        return UpdateWarehouseResponse.fromOutput(result)
    }

    @PatchMapping("/{id}/status")
    fun changeStatus(
        @PathVariable id: String,
        @RequestBody body: ChangeWarehouseStatusRequest
    ): ChangeWarehouseStatusResponse {
        val result = changeWarehouseStatusUseCase.execute(id, body.toInput())
        return ChangeWarehouseStatusResponse.fromOutput(result)
    }
}

data class CreateWarehouseRequest(
    val name: String,
    val address: String,
) {
    fun toInput() = CreateWarehouseInput(name = name, address = address)
}

data class CreateWarehouseResponse(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
) {
    companion object {
        fun fromOutput(output: CreateWarehouseOutput) = CreateWarehouseResponse(
            id = output.id,
            name = output.name,
            address = output.address,
            active = output.active
        )
    }
}

data class ListWarehouseResponse(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
) {
    companion object {
        fun fromOutput(output: ListWarehousesOutput) = ListWarehouseResponse(
            id = output.id,
            name = output.name,
            address = output.address,
            active = output.active
        )
    }
}

data class GetWarehouseByIdResponse(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun fromOutput(output: GetWarehouseByIdOutput) = GetWarehouseByIdResponse(
            id = output.id,
            name = output.name,
            address = output.address,
            active = output.active,
            createdAt = output.createdAt.toString(),
            updatedAt = output.updatedAt.toString()
        )
    }
}

data class UpdateWarehouseRequest(
    val name: String? = null,
    val address: String? = null,
) {
    fun toInput() = UpdateWarehouseInput(name = name, address = address)
}

data class UpdateWarehouseResponse(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
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

data class ChangeWarehouseStatusRequest(
    val active: Boolean,
) {
    fun toInput() = ChangeWarehouseStatusInput(active = active)
}

data class ChangeWarehouseStatusResponse(
    val id: String,
    val name: String,
    val address: String,
    val active: Boolean,
) {
    companion object {
        fun fromOutput(output: ChangeWarehouseStatusOutput) = ChangeWarehouseStatusResponse(
            id = output.id,
            name = output.name,
            address = output.address,
            active = output.active
        )
    }
}

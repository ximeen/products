package com.ximenes.products.infrastructure.http.spring.controllers

import com.ximenes.products.application.use_cases.product.CreateProductInput
import com.ximenes.products.application.use_cases.product.CreateProductOutput
import com.ximenes.products.application.use_cases.product.CreateProductUseCase
import com.ximenes.products.application.use_cases.product.DeleteProductUseCase
import com.ximenes.products.application.use_cases.product.GetProductOutput
import com.ximenes.products.application.use_cases.product.GetProductUseCase
import com.ximenes.products.application.use_cases.product.ListProductsInput
import com.ximenes.products.application.use_cases.product.ListProductsOutput
import com.ximenes.products.application.use_cases.product.ListProductsUseCase
import com.ximenes.products.application.use_cases.product.ProductSummary
import com.ximenes.products.application.use_cases.product.UpdateProductInput
import com.ximenes.products.application.use_cases.product.UpdateProductOutput
import com.ximenes.products.application.use_cases.product.UpdateProductUseCase
import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.product.ProductStatus
import com.ximenes.products.domain.entities.warehouse_stock.IWarehouseStockRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/products")
class ProductController(
    private val createProductUseCase: CreateProductUseCase,
    private val getProductUseCase: GetProductUseCase,
    private val listProductsUseCase: ListProductsUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val productRepo: IProductRepository,
    private val stockRepo: IWarehouseStockRepository
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @Valid body: CreateProductRequest): CreateProductResponse {
        val result = createProductUseCase.execute(body.toInput())
        return CreateProductResponse.fromOutput(result)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): GetProductResponse {
        val result = getProductUseCase.execute(id)
        return GetProductResponse.fromOutput(result)
    }

    @GetMapping
    fun list(
        @RequestParam category: String? = null,
        @RequestParam status: String? = null,
        @RequestParam(defaultValue = "0") @Min(0) page: Int = 0,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int = 20
    ): ListProductsResponse {
        val input = ListProductsInput(
            category = category,
            status = status?.let { ProductStatus.valueOf(it) },
            page = page,
            size = size
        )
        val result = listProductsUseCase.execute(input)
        return ListProductsResponse.fromOutput(result)
    }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody body: UpdateProductRequest
    ): UpdateProductResponse {
        val result = updateProductUseCase.execute(id, body.toInput())
        return UpdateProductResponse.fromOutput(result)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String) {
        deleteProductUseCase.execute(id)
    }
}

data class CreateProductRequest(
    val name: String,
    val description: String? = null,
    val sku: String,
    val category: String? = null,
    val defaultPrice: Double
) {
    fun toInput() = CreateProductInput(
        name = name,
        description = description,
        sku = sku,
        category = category,
        defaultPrice = java.math.BigDecimal.valueOf(defaultPrice)
    )
}

data class CreateProductResponse(
    val id: String,
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: Double,
    val status: String
) {
    companion object {
        fun fromOutput(output: CreateProductOutput) = CreateProductResponse(
            id = output.id,
            name = output.name,
            description = output.description,
            sku = output.sku,
            category = output.category,
            defaultPrice = output.defaultPrice.toDouble(),
            status = output.status.name
        )
    }
}

data class GetProductResponse(
    val id: String,
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: Double,
    val status: String,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromOutput(output: GetProductOutput) = GetProductResponse(
            id = output.id,
            name = output.name,
            description = output.description,
            sku = output.sku,
            category = output.category,
            defaultPrice = output.defaultPrice.toDouble(),
            status = output.status.name,
            createdAt = output.createdAt,
            updatedAt = output.updatedAt
        )
    }
}

data class ListProductsResponse(
    val products: List<ProductSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun fromOutput(output: ListProductsOutput) = ListProductsResponse(
            products = output.products.map { ProductSummaryResponse.fromSummary(it) },
            page = output.page,
            size = output.size,
            totalElements = output.totalElements,
            totalPages = output.totalPages
        )
    }
}

data class ProductSummaryResponse(
    val id: String,
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: Double,
    val status: String
) {
    companion object {
        fun fromSummary(summary: ProductSummary) = ProductSummaryResponse(
            id = summary.id,
            name = summary.name,
            description = summary.description,
            sku = summary.sku,
            category = summary.category,
            defaultPrice = summary.defaultPrice.toDouble(),
            status = summary.status.name
        )
    }
}

data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val sku: String? = null,
    val category: String? = null,
    val defaultPrice: Double? = null,
    val status: String? = null
) {
    fun toInput() = UpdateProductInput(
        name = name,
        description = description,
        sku = sku,
        category = category,
        defaultPrice = defaultPrice?.let { java.math.BigDecimal.valueOf(it) },
        status = status?.let { com.ximenes.products.domain.entities.product.ProductStatus.valueOf(it) }
    )
}

data class UpdateProductResponse(
    val id: String,
    val name: String,
    val description: String?,
    val sku: String,
    val category: String?,
    val defaultPrice: Double,
    val status: String,
    val updatedAt: String
) {
    companion object {
        fun fromOutput(output: UpdateProductOutput) = UpdateProductResponse(
            id = output.id,
            name = output.name,
            description = output.description,
            sku = output.sku,
            category = output.category,
            defaultPrice = output.defaultPrice.toDouble(),
            status = output.status.name,
            updatedAt = output.updatedAt
        )
    }
}

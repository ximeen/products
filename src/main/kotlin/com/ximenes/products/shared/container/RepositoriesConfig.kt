package com.ximenes.products.shared.container

import com.ximenes.products.application.use_cases.product.CreateProductUseCase
import com.ximenes.products.application.use_cases.product.DeleteProductUseCase
import com.ximenes.products.application.use_cases.product.GetProductUseCase
import com.ximenes.products.application.use_cases.product.ListProductsUseCase
import com.ximenes.products.application.use_cases.product.UpdateProductUseCase
import com.ximenes.products.application.use_cases.warehouse.UpdateWarehouseUseCase
import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.warehouse.IWarehouseRepository
import com.ximenes.products.domain.entities.warehouse_stock.IWarehouseStockRepository
import com.ximenes.products.infrastructure.database.jpa.repositories.ProductJpaRepository
import com.ximenes.products.infrastructure.database.jpa.repositories.WarehouseJpaRepository
import com.ximenes.products.infrastructure.database.jpa.repositories.WarehouseStockJpaRepository
import com.ximenes.products.infrastructure.database.repositories.ProductRepositoryImpl
import com.ximenes.products.infrastructure.database.repositories.WarehouseRepositoryImpl
import com.ximenes.products.infrastructure.database.repositories.WarehouseStockRepositoryImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RepositoriesConfig {

    @Bean
    fun productRepository(jpaRepository: ProductJpaRepository): IProductRepository =
        ProductRepositoryImpl(jpaRepository)

    @Bean
    fun warehouseRepository(jpaRepository: WarehouseJpaRepository): IWarehouseRepository =
        WarehouseRepositoryImpl(jpaRepository)

    @Bean
    fun warehouseStockRepository(jpaRepository: WarehouseStockJpaRepository): IWarehouseStockRepository =
        WarehouseStockRepositoryImpl(jpaRepository)

    @Bean
    fun createProductUseCase(productRepo: IProductRepository): CreateProductUseCase =
        CreateProductUseCase(productRepo)

    @Bean
    fun getProductUseCase(productRepo: IProductRepository): GetProductUseCase =
        GetProductUseCase(productRepo)

    @Bean
    fun listProductsUseCase(productRepo: IProductRepository): ListProductsUseCase =
        ListProductsUseCase(productRepo)

    @Bean
    fun updateProductUseCase(productRepo: IProductRepository): UpdateProductUseCase =
        UpdateProductUseCase(productRepo)

    @Bean
    fun deleteProductUseCase(
        productRepo: IProductRepository,
        stockRepo: IWarehouseStockRepository
    ): DeleteProductUseCase =
        DeleteProductUseCase(productRepo, stockRepo)

    @Bean
    fun updateWarehouseUseCase(
        warehouseRepo: IWarehouseRepository,
        stockRepo: IWarehouseStockRepository,
        productRepo: IProductRepository
    ): UpdateWarehouseUseCase =
        UpdateWarehouseUseCase(warehouseRepo, stockRepo, productRepo)
}

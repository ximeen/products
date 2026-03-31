package com.ximenes.products.application.use_cases.product

import com.ximenes.products.domain.entities.product.IProductRepository
import com.ximenes.products.domain.entities.warehouse_stock.IWarehouseStockRepository
import com.ximenes.products.shared.errors.ConflictError
import com.ximenes.products.shared.errors.NotFoundError
import org.springframework.stereotype.Component

@Component
class DeleteProductUseCase(
    private val productRepo: IProductRepository,
    private val stockRepo: IWarehouseStockRepository
) {
    fun execute(id: String) {
        val product = productRepo.findById(id)
            ?: throw NotFoundError("Produto", id)

        val hasStock = stockRepo.existsByProductIdWithPositiveQuantity(id)
        if (hasStock) {
            throw ConflictError("Não é possível excluir produto com estoque ativo em depósitos")
        }

        stockRepo.deleteByProductId(id)

        productRepo.delete(id)
    }
}

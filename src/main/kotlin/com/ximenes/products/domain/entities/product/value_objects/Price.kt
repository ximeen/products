package com.ximenes.products.domain.entities.product.value_objects

import com.ximenes.products.domain.shared.ValueObject
import java.math.BigDecimal

class Price private constructor(private val value: BigDecimal) : ValueObject<BigDecimal>(value) {

    fun getValue(): BigDecimal = value

    fun isPositive(): Boolean = value > BigDecimal.ZERO

    fun isZeroOrNegative(): Boolean = value <= BigDecimal.ZERO

    companion object {
        fun create(value: BigDecimal): Price {
            require(value >= BigDecimal.ZERO) { "Preço não pode ser negativo" }
            require(value > BigDecimal.ZERO) { "Preço deve ser maior que zero" }
            return Price(value)
        }

        fun from(value: BigDecimal): Price {
            return Price(value)
        }
    }
}

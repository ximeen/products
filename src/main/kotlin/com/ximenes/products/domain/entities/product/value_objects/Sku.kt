package com.ximenes.products.domain.entities.product.value_objects

import com.ximenes.products.domain.shared.ValueObject

class Sku private constructor(private val value: String) : ValueObject<String>(value) {

    fun getValue(): String = value

    companion object {
        private val SKU_REGEX = Regex("^[A-Z]{2,10}-[0-9]{1,6}$")

        fun create(raw: String): Sku {
            val trimmed = raw.trim().uppercase()
            require(trimmed.isNotEmpty()) { "SKU é obrigatório" }
            require(SKU_REGEX.matches(trimmed)) { "SKU deve seguir o formato ABC-1234 (letras maiúsculas, hífen, números)" }
            return Sku(trimmed)
        }
    }
}

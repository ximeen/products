package com.ximenes.products.shared.errors

open class BaseError(
    message: String,
    val statusCode: Int,
    val details: Any? = null,
) : RuntimeException(message)

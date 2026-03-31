package com.ximenes.products.shared.errors

open class BaseError(
    val code: String,
    message: String,
    val statusCode: Int,
    val details: Any? = null,
) : RuntimeException(message)

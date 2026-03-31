package com.ximenes.products.shared.errors

class NotFoundError(entity: String, id: String? = null) : BaseError(
    code = "NOT_FOUND",
    message = "$entity${if (id != null) " com id $id" else ""} não encontrado",
    statusCode = 404,
)

class ValidationError(message: String, details: Any? = null, code: String = "VALIDATION_ERROR") : BaseError(
    code = code,
    message = message,
    statusCode = 400,
    details = details,
)

class UnauthorizedError(message: String = "Acesso não autorizado") : BaseError(
    code = "UNAUTHORIZED",
    message = message,
    statusCode = 401,
)

class ForbiddenError(message: String = "Acesso proibido") : BaseError(
    code = "FORBIDDEN",
    message = message,
    statusCode = 403,
)

class ConflictError(message: String, details: Any? = null, code: String = "CONFLICT") : BaseError(
    code = code,
    message = message,
    statusCode = 409,
    details = details,
)

object ErrorCodes {
    const val PRODUCT_NAME_ALREADY_EXISTS = "PRODUCT_NAME_ALREADY_EXISTS"
    const val PRODUCT_SKU_ALREADY_EXISTS = "PRODUCT_SKU_ALREADY_EXISTS"
    const val WAREHOUSE_NAME_ALREADY_EXISTS = "WAREHOUSE_NAME_ALREADY_EXISTS"
    const val WAREHOUSE_HAS_STOCK = "WAREHOUSE_HAS_STOCK"
    const val PRODUCT_HAS_STOCK = "PRODUCT_HAS_STOCK"
    const val LOCATION_ALREADY_OCCUPIED = "LOCATION_ALREADY_OCCUPIED"
    const val WAREHOUSE_INACTIVE = "WAREHOUSE_INACTIVE"
}
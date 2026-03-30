package com.ximenes.products.shared.errors

class NotFoundError(entity: String, id: String? = null) : BaseError(
    message = "$entity${if (id != null) " com id $id" else ""} não encontrado",
    statusCode = 404,
)

class ValidationError(message: String, details: Any? = null) : BaseError(
    message = message,
    statusCode = 400,
    details = details,
)

class UnauthorizedError(message: String = "Acesso não autorizado") : BaseError(
    message = message,
    statusCode = 401,
)

class ForbiddenError(message: String = "Acesso proibido") : BaseError(
    message = message,
    statusCode = 403,
)

class ConflictError(message: String, details: Any? = null) : BaseError(
    message = message,
    statusCode = 409,
    details = details,
)

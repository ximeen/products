package com.ximenes.products.infrastructure.http.spring.config

import com.ximenes.products.shared.errors.BaseError
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BaseError::class)
    fun handleDomainError(ex: BaseError): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ex.statusCode)
            .body(ErrorResponse(error = ex.message ?: "Erro interno", details = ex.details))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(400)
            .body(ErrorResponse(error = "Erro de validação", details = details))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(400)
            .body(ErrorResponse(error = ex.message ?: "Erro de validação"))
}

data class ErrorResponse(val error: String, val details: Any? = null)

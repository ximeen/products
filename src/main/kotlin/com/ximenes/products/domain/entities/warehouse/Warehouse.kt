package com.ximenes.products.domain.entities.warehouse

import com.ximenes.products.domain.shared.Entity

data class WarehouseProps(
    val name: String,
    val address: String,
    var active: Boolean = true,
)

class Warehouse private constructor(
    props: WarehouseProps,
    id: String? = null
) : Entity<WarehouseProps>(props, id) {

    val name: String get() = props.name
    val address: String get() = props.address
    val active: Boolean get() = props.active

    fun assignCreatedAt(time: java.time.Instant) {
        this.createdAt = time
    }

    fun assignUpdatedAt(time: java.time.Instant) {
        this.updatedAt = time
    }

    fun deactivate() {
        props.active = false
        touch()
    }

    fun reactivate() {
        props.active = true
        touch()
    }

    fun isActive(): Boolean = props.active

    companion object {
        fun create(props: WarehouseProps, id: String? = null): Warehouse {
            require(props.name.trim().isNotEmpty()) { "Nome é obrigatório" }
            require(props.name.trim().length >= 3) { "Nome deve ter no mínimo 3 caracteres" }
            require(props.name.trim().length <= 100) { "Nome deve ter no máximo 100 caracteres" }
            require(props.address.trim().isNotEmpty()) { "Endereço é obrigatório" }
            require(props.address.trim().length >= 5) { "Endereço deve ter no mínimo 5 caracteres" }
            require(props.address.trim().length <= 255) { "Endereço deve ter no máximo 255 caracteres" }
            return Warehouse(props.copy(name = props.name.trim(), address = props.address.trim()), id)
        }
    }
}

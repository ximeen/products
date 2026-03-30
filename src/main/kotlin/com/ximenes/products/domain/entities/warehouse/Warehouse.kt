package com.ximenes.products.domain.entities.warehouse

import com.ximenes.products.domain.shared.Entity

data class WarehouseProps(
    val name: String,
    val address: String,
    val active: Boolean = true,
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

    fun updateWith(
        name: String? = null,
        address: String? = null,
        active: Boolean? = null
    ): Warehouse {
        val newName = (name ?: this.name).trim()
        val newAddress = (address ?: this.address).trim()
        val newActive = active ?: this.active

        require(newName.isNotEmpty()) { "Nome é obrigatório" }
        require(newName.length >= 3) { "Nome deve ter no mínimo 3 caracteres" }
        require(newName.length <= 100) { "Nome deve ter no máximo 100 caracteres" }
        require(newAddress.isNotEmpty()) { "Endereço é obrigatório" }
        require(newAddress.length <= 255) { "Endereço deve ter no máximo 255 caracteres" }

        val newProps = WarehouseProps(
            name = newName,
            address = newAddress,
            active = newActive
        )

        return Warehouse(newProps, this.id).also {
            it.createdAt = this.createdAt
        }
    }

    fun deactivate(): Warehouse = updateWith(active = false)
    fun activate(): Warehouse = updateWith(active = true)
    fun isActive(): Boolean = props.active

    companion object {
        fun create(props: WarehouseProps, id: String? = null): Warehouse {
            require(props.name.trim().isNotEmpty()) { "Nome é obrigatório" }
            require(props.name.trim().length >= 3) { "Nome deve ter no mínimo 3 caracteres" }
            require(props.name.trim().length <= 100) { "Nome deve ter no máximo 100 caracteres" }
            require(props.address.trim().isNotEmpty()) { "Endereço é obrigatório" }
            require(props.address.trim().length <= 255) { "Endereço deve ter no máximo 255 caracteres" }

            return Warehouse(
                props = props.copy(
                    name = props.name.trim(),
                    address = props.address.trim()
                ),
                id = id
            )
        }
    }
}

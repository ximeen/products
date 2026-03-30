package com.ximenes.products.domain.shared

import java.time.Instant

abstract class Entity<T>(
    protected val props: T,
    id: String? = null
) {
    val id: String = id ?: java.util.UUID.randomUUID().toString()
    var createdAt: Instant = Instant.now()
        protected set
    var updatedAt: Instant = Instant.now()
        protected set

    protected fun touch() {
        updatedAt = Instant.now()
    }

    fun equals(entity: Entity<T>?): Boolean {
        if (entity == null) return false
        return this.id == entity.id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity<*>) return false
        return this.id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

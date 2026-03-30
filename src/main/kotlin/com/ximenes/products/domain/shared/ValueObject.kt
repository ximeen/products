package com.ximenes.products.domain.shared

abstract class ValueObject<T>(protected val props: T) {
    fun equals(vo: ValueObject<T>?): Boolean {
        if (vo == null) return false
        return this.props == vo.props
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValueObject<*>) return false
        return this.props == other.props
    }

    override fun hashCode(): Int = props.hashCode()
}

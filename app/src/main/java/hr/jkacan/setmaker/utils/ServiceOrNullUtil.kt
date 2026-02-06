package hr.jkacan.setmaker.utils

import kotlin.reflect.KProperty0

fun <T> getServiceOrNull(prop: KProperty0<T>): T? =
    try {
        prop.get()
    } catch (_: UninitializedPropertyAccessException) {
        null
    }
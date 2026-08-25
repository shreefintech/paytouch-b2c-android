package com.shreefintech.paytouchconsumer.operator.model

data class OperatorSelectionItem(
    val id: String,
    val name: String,
    val code: String? = null,
    val extra: String? = null
)

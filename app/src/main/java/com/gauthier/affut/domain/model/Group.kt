package com.gauthier.affut.domain.model

data class Group(
    val id: String,
    val name: String,
    val code: String,
    val createdBy: String,
    val memberCount: Int = 0,
)

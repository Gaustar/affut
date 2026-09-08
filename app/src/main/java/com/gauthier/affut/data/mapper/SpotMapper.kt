package com.gauthier.affut.data.mapper

import com.gauthier.affut.data.local.entity.SpotEntity
import com.gauthier.affut.data.remote.firebase.dto.SpotDto
import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.domain.model.SpotType

fun SpotEntity.toDomain(): Spot = Spot(
    id = id,
    ownerId = ownerId,
    title = title,
    type = SpotType.fromStorageValue(type),
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    notes = notes,
    isShared = isShared,
    createdAt = createdAt,
    updatedAt = updatedAt,
    observedAt = observedAt,
)

fun Spot.toEntity(): SpotEntity = SpotEntity(
    id = id,
    ownerId = ownerId,
    title = title,
    type = type.name,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    notes = notes,
    isShared = isShared,
    createdAt = createdAt,
    updatedAt = updatedAt,
    observedAt = observedAt,
)

fun SpotDto.toDomain(): Spot = Spot(
    id = id,
    ownerId = ownerId,
    title = title,
    type = SpotType.fromStorageValue(type),
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    notes = notes,
    isShared = isShared,
    createdAt = createdAt,
    updatedAt = updatedAt,
    observedAt = observedAt,
)

fun Spot.toDto(): SpotDto = SpotDto(
    id = id,
    ownerId = ownerId,
    title = title,
    type = type.name,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    notes = notes,
    isShared = isShared,
    createdAt = createdAt,
    updatedAt = updatedAt,
    observedAt = observedAt,
)

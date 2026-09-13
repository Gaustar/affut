package com.gauthier.affut.data.mapper

import com.gauthier.affut.data.local.entity.SpotEntity
import com.gauthier.affut.data.remote.firebase.dto.SpotDto
import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.domain.model.SpotType

private fun String.toIdList(): List<String> = if (isBlank()) emptyList() else split(",")
private fun List<String>.toIdString(): String = joinToString(",")

fun SpotEntity.toDomain(): Spot = Spot(
    id = id,
    ownerId = ownerId,
    title = title,
    type = SpotType.fromStorageValue(type),
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    notes = notes,
    sharedWithFriendIds = sharedWithFriendIds.toIdList(),
    sharedWithGroupIds = sharedWithGroupIds.toIdList(),
    sharedWithUids = sharedWithUids.toIdList(),
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
    sharedWithFriendIds = sharedWithFriendIds.toIdString(),
    sharedWithGroupIds = sharedWithGroupIds.toIdString(),
    sharedWithUids = sharedWithUids.toIdString(),
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
    sharedWithFriendIds = sharedWithFriendIds,
    sharedWithGroupIds = sharedWithGroupIds,
    sharedWithUids = sharedWithUids,
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
    sharedWithFriendIds = sharedWithFriendIds,
    sharedWithGroupIds = sharedWithGroupIds,
    sharedWithUids = sharedWithUids,
    createdAt = createdAt,
    updatedAt = updatedAt,
    observedAt = observedAt,
)

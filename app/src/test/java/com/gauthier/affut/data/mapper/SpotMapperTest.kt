package com.gauthier.affut.data.mapper

import com.gauthier.affut.domain.model.Spot
import com.gauthier.affut.domain.model.SpotType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Les listes de partage (sharedWithFriendIds/sharedWithGroupIds/sharedWithUids) sont
 * stockées en Room comme une chaîne d'UID séparés par des virgules (pas de TypeConverter,
 * voir SpotEntity). Ce test garde ce vaet-vient honnête : exactement le genre de bug
 * silencieux (une conversion qui a l'air correcte mais perd des données) déjà rencontré
 * sur ce projet pour l'affichage de la visibilité météo.
 */
class SpotMapperTest {

    private fun sampleSpot(
        friendIds: List<String> = emptyList(),
        groupIds: List<String> = emptyList(),
        uids: List<String> = emptyList(),
    ) = Spot(
        id = "spot-1",
        ownerId = "owner-1",
        title = "Test",
        type = SpotType.AUTRE,
        latitude = 49.8,
        longitude = 5.0,
        accuracy = 5f,
        notes = "",
        sharedWithFriendIds = friendIds,
        sharedWithGroupIds = groupIds,
        sharedWithUids = uids,
        createdAt = 1L,
        updatedAt = 2L,
    )

    @Test
    fun `listes de partage vides survivent a l aller-retour Room`() {
        val spot = sampleSpot()
        val roundTripped = spot.toEntity().toDomain()
        assertEquals(emptyList<String>(), roundTripped.sharedWithFriendIds)
        assertEquals(emptyList<String>(), roundTripped.sharedWithGroupIds)
        assertEquals(emptyList<String>(), roundTripped.sharedWithUids)
    }

    @Test
    fun `listes de partage remplies survivent a l aller-retour Room`() {
        val spot = sampleSpot(
            friendIds = listOf("friendA", "friendB"),
            groupIds = listOf("group1"),
            uids = listOf("friendA", "friendB", "memberOfGroup1"),
        )
        val roundTripped = spot.toEntity().toDomain()
        assertEquals(listOf("friendA", "friendB"), roundTripped.sharedWithFriendIds)
        assertEquals(listOf("group1"), roundTripped.sharedWithGroupIds)
        assertEquals(listOf("friendA", "friendB", "memberOfGroup1"), roundTripped.sharedWithUids)
    }

    @Test
    fun `un seul UID partage survit a l aller-retour Room`() {
        // Cas limite : la jointure par virgule d'une liste à un seul élément ne doit pas
        // introduire de virgule parasite qui créerait un UID vide au découpage.
        val spot = sampleSpot(uids = listOf("soloFriend"))
        val roundTripped = spot.toEntity().toDomain()
        assertEquals(listOf("soloFriend"), roundTripped.sharedWithUids)
    }
}

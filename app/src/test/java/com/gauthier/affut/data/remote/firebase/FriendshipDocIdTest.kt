package com.gauthier.affut.data.remote.firebase

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'id du document d'amitié doit être le même quel que soit l'ordre des deux UID passés,
 * sinon les deux comptes créeraient chacun leur propre document au lieu de partager le même
 * (et l'amitié ne serait plus vraiment mutuelle — voir le commentaire de FriendshipDto).
 */
class FriendshipDocIdTest {

    @Test
    fun `id identique quel que soit l ordre des UID`() {
        assertEquals(friendshipDocId("uidA", "uidB"), friendshipDocId("uidB", "uidA"))
    }

    @Test
    fun `id contient les deux UID separes`() {
        val id = friendshipDocId("zzz", "aaa")
        assertEquals("aaa_zzz", id)
    }

    @Test
    fun `deux paires differentes donnent des id differents`() {
        assertEquals(false, friendshipDocId("uid1", "uid2") == friendshipDocId("uid1", "uid3"))
    }
}

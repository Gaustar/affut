package com.gauthier.affut.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reprend les cas déjà validés à la main (portage Python jetable) lors de l'écriture du
 * mécanisme de mise à jour GitHub, cette fois comme un vrai test qui reste dans le dépôt.
 */
class VersionComparatorTest {

    @Test
    fun `plus grande version mineure est detectee comme plus recente`() {
        assertTrue(isNewerVersion("1.0", "1.1"))
    }

    @Test
    fun `comparaison numerique pas textuelle - 1_9 contre 1_10`() {
        // Piège classique : "1.10" < "1.9" en comparaison de texte brut, mais bien
        // supérieur en comparaison numérique par composants.
        assertTrue(isNewerVersion("1.9", "1.10"))
        assertFalse(isNewerVersion("1.10", "1.9"))
    }

    @Test
    fun `version identique n est jamais plus recente`() {
        assertFalse(isNewerVersion("1.0", "1.0"))
    }

    @Test
    fun `version distante plus ancienne n est pas signalee`() {
        assertFalse(isNewerVersion("2.0", "1.9"))
    }

    @Test
    fun `prefixe v des tags GitHub est tolere`() {
        assertTrue(isNewerVersion("1.0", "v1.1"))
        assertTrue(isNewerVersion("v1.0", "v1.1"))
    }

    @Test
    fun `nombre de composants different se complete par des zeros`() {
        assertTrue(isNewerVersion("1.0", "1.0.1"))
        assertFalse(isNewerVersion("1.0.0", "1.0"))
    }

    @Test
    fun `format illisible ne produit jamais de faux positif`() {
        assertFalse(isNewerVersion("1.0", "abc"))
        assertFalse(isNewerVersion("abc", "1.0"))
        assertFalse(isNewerVersion("", "1.0"))
        assertFalse(isNewerVersion("1.0", ""))
        assertFalse(isNewerVersion("1.0", "1.0.x"))
    }
}

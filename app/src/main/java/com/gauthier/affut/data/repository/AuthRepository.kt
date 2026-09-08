package com.gauthier.affut.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Identité automatique via Google Sign-In (Credential Manager) : un tap pour
 * confirmer le compte Google déjà présent sur l'appareil, aucun mot de passe,
 * aucun compte Firebase à créer à la main — et ça survit à un changement de
 * téléphone puisque l'identité est liée au compte Google, pas à l'appareil.
 */
class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Déclenche la sélection du compte Google et connecte à Firebase. Ne fait rien si déjà connecté. */
    suspend fun ensureSignedIn(context: Context, webClientId: String): Result<FirebaseUser> {
        currentUser?.let { return Result.success(it) }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return Result.failure(IllegalStateException("Type d'identifiant Google inattendu."))
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val result = auth.signInWithCredential(firebaseCredential).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Connexion Google impossible."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}

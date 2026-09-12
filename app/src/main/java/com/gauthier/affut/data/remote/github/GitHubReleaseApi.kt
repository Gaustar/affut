package com.gauthier.affut.data.remote.github

import com.gauthier.affut.data.remote.github.dto.GitHubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubReleaseApi {
    /** Endpoint public, sans authentification (limité à 60 requêtes/heure par IP — largement
     * suffisant pour une vérification par lancement d'app entre quelques utilisateurs). */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GitHubReleaseDto
}

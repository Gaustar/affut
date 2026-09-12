package com.gauthier.affut.data.repository

import com.gauthier.affut.data.remote.github.GitHubReleaseApi
import com.gauthier.affut.data.remote.github.GitHubReleaseClient
import com.gauthier.affut.domain.model.AppUpdate
import com.gauthier.affut.util.isNewerVersion

private const val GITHUB_OWNER = "Gaustar"
private const val GITHUB_REPO = "affut"

class UpdateRepository(
    private val api: GitHubReleaseApi = GitHubReleaseClient.api,
) {
    /** null si déjà à jour, si hors-ligne, ou en cas d'erreur — vérifier une mise à jour n'a
     * aucune conséquence pour l'utilisateur (contrairement à une synchro ratée), un échec
     * silencieux ici est donc acceptable et ne mérite pas d'alerter. */
    suspend fun checkForUpdate(currentVersionName: String): AppUpdate? {
        return try {
            val release = api.getLatestRelease(GITHUB_OWNER, GITHUB_REPO)
            if (release.tagName.isBlank() || release.htmlUrl.isBlank()) return null
            if (!isNewerVersion(currentVersionName, release.tagName)) return null
            AppUpdate(versionLabel = release.tagName, releaseUrl = release.htmlUrl)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun create(): UpdateRepository = UpdateRepository()
    }
}

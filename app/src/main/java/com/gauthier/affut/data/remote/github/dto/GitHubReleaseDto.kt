package com.gauthier.affut.data.remote.github.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Reflète la réponse de l'API publique GitHub "releases/latest" (aucune clé requise). */
@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val name: String = "",
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@Serializable
data class GitHubReleaseAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
)

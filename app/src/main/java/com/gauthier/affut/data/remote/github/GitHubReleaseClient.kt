package com.gauthier.affut.data.remote.github

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import retrofit2.Retrofit

object GitHubReleaseClient {
    private val json = Json { ignoreUnknownKeys = true }

    val api: GitHubReleaseApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(json.asConverterFactory(MediaType.parse("application/json")!!))
        .build()
        .create(GitHubReleaseApi::class.java)
}

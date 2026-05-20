package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Image Generation Data Classes ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String,
    @Json(name = "imageSize") val imageSize: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Text/Metaphor Generation Data Classes ---

@JsonClass(generateAdapter = true)
data class GenerateTextRequest(
    @Json(name = "contents") val contents: List<ContentText>
)

@JsonClass(generateAdapter = true)
data class ContentText(
    @Json(name = "parts") val parts: List<PartText>
)

@JsonClass(generateAdapter = true)
data class PartText(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerateTextResponse(
    @Json(name = "candidates") val candidates: List<CandidateText>? = null
)

@JsonClass(generateAdapter = true)
data class CandidateText(
    @Json(name = "content") val content: ContentText? = null
)

// --- Service Interfaces ---

interface GeminiImageService {
    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateImage(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

interface GeminiTextService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateText(
        @Query("key") apiKey: String,
        @Body request: GenerateTextRequest
    ): GenerateTextResponse
}

// --- Retrofit Client ---

object RetrofitClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val imageService: GeminiImageService by lazy {
        retrofit.create(GeminiImageService::class.java)
    }

    val textService: GeminiTextService by lazy {
        retrofit.create(GeminiTextService::class.java)
    }
}

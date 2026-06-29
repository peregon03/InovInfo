package com.Electroinova.inovinfo.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiService {

    /**
     * Llama a Gemini 2.0 Flash para estructurar la observación técnica.
     * Base URL: https://generativelanguage.googleapis.com/
     */
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key")  apiKey:  String,
        @Body          request: GeminiRequest
    ): GeminiResponse
}

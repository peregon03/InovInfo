package com.Electroinova.inovinfo.data.remote

import com.google.gson.annotations.SerializedName

// ── Request ───────────────────────────────────────────────────────────────────

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val role:  String,          // "user" | "model"
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

// ── Response ──────────────────────────────────────────────────────────────────

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content:      GeminiContent?,
    @SerializedName("finishReason")
    val finishReason: String?
)

// ── Helper para extraer el texto generado ────────────────────────────────────

fun GeminiResponse.textoGenerado(): String? =
    candidates
        ?.firstOrNull()
        ?.content
        ?.parts
        ?.firstOrNull()
        ?.text
        ?.trim()

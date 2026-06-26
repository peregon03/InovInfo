package com.Electroinova.inovinfo.domain.model

/**
 * Modelo de dominio — Visita Técnica a DVR Móvil.
 *
 * Sedes: Tuluá, Buga, Sur Occidente, Cali, Río Paila.
 * Estados: OK, Con novedad, Inoperativa, Sin llaves, Batería desconectada.
 *
 * Implementación pendiente (se define cuando se construya NuevaVisitaScreen).
 */
data class Visita(
    val id: Long = 0,
    val numeroUnidad: String = "",
    val sede: String = "",
    val estado: EstadoVisita = EstadoVisita.OK,
    val observacion: String = "",
    val suministros: List<String> = emptyList(),
    val fotosUrls: List<String> = emptyList(),
    val requiereSeguimiento: Boolean = false,
    val fechaSeguimiento: String? = null,
    val notaSeguimiento: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)

enum class EstadoVisita(val label: String) {
    OK("OK"),
    CON_NOVEDAD("Con novedad"),
    INOPERATIVA("Inoperativa"),
    SIN_LLAVES("Sin llaves"),
    BATERIA_DESCONECTADA("Batería desconectada")
}

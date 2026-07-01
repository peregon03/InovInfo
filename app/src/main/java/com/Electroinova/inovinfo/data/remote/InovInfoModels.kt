package com.Electroinova.inovinfo.data.remote

// ── Catálogos ─────────────────────────────────────────────────────────────────

data class UnidadDto(
    val id:          Int,
    val numero:      String,
    val sede_nombre: String,
    val sede_id:     Int
)

data class SuministroDto(
    val id:        Int,
    val nombre:    String,
    val categoria: String? = null
)

data class EstadoDto(
    val id:        Int,
    val nombre:    String,
    val color_hex: String
)

// ── Visitas ───────────────────────────────────────────────────────────────────

data class SuministroRequest(
    val suministro_id: Int,
    val cantidad:      Int = 1
)

data class CrearVisitaRequest(
    val unidad_id:               Int,
    val estado_id:               Int,
    val novedad_texto:           String?,
    val observacion_estructurada: String?,
    val requiere_seguimiento:    Boolean,
    val fecha_seguimiento:       String?,
    val nota_seguimiento:        String?,
    val suministros:             List<SuministroRequest>
)

// Respuesta de /api/visitas/hoy
data class VisitaHoyDto(
    val id:                       Int,
    val unidad_numero:            String,
    val sede_nombre:              String,
    val estado:                   String,
    val color_hex:                String,
    val tecnico_nombre:           String,
    val observacion_estructurada: String?,
    val fotos_count:              Int = 0
)

// Respuesta de /api/visitas/pendientes
data class PendienteDto(
    val id:               Int,
    val unidad_numero:    String,
    val sede_nombre:      String,
    val fecha_seguimiento: String,
    val nota_seguimiento: String?,
    val tecnico_nombre:   String,
    val urgencia:         String  // "vencido" | "hoy" | "futuro"
)

// ── Usuarios (admin) ──────────────────────────────────────────────────────────

data class UsuarioAdminDto(
    val id:       Int,
    val nombre:   String,
    val email:    String,
    val rol:      String,
    val activo:   Boolean,
    val permisos: List<String> = emptyList()
)

data class ActualizarPermisosRequest(val permisos: List<String>)
data class ActualizarEstadoRequest(val activo: Boolean)

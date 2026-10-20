package com.Electroinova.inovinfo.data.remote

// ── Catálogos ─────────────────────────────────────────────────────────────────

data class UnidadDto(
    val id:          Int,
    val numero:      String,
    val sede_nombre: String,
    val sede_id:     Int
)

data class SuministroDto(
    val id:         Int,
    val nombre:     String,
    val categoria:  String?  = null,
    val precio_ref: Double?  = null,
    val activo:     Boolean  = true
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

// ── Catálogos (gestión coordinadora) ──────────────────────────────────────────

data class SedeDto(
    val id:     Int,
    val nombre: String
)

data class CrearUnidadRequest(
    val numero:  String,
    val sede_id: Int
)

data class ActualizarUnidadRequest(
    val numero:  String,
    val sede_id: Int,
    val activa:  Boolean
)

data class CrearSedeRequest(val nombre: String)

data class ActualizarSedeRequest(val nombre: String, val activa: Boolean)

data class CrearSuministroRequest(
    val nombre:     String,
    val categoria:  String?,
    val precio_ref: Double?
)

data class ActualizarSuministroRequest(
    val nombre:     String,
    val categoria:  String?,
    val precio_ref: Double?,
    val activo:     Boolean
)

// ── Fotos ─────────────────────────────────────────────────────────────────────

data class FotoResponse(
    val id:  Int,
    val url: String
)

// ── Informes ────────────────────────────────────────────────────────────────

data class GenerarInformeRequest(
    val fechaDesde:  String,   // "YYYY-MM-DD"
    val fechaHasta:  String,   // "YYYY-MM-DD"
    val sedeId:      Int?    = null,
    val sedeNombre:  String? = null,
    val linkFotos:   String? = null
)

data class InformeResponse(
    val sheetUrl:      String,
    val pestana:       String,
    val totalVisitas:  Int
)

// ── Importación masiva desde Excel ────────────────────────────────────────────

data class ImportarExcelResponse(
    val sedesCreadas:   Int,
    val unidadesCreadas: Int,
    val errores:        List<String> = emptyList()
)

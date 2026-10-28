package com.Electroinova.inovinfo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.Electroinova.inovinfo.data.remote.EstadoDto
import com.Electroinova.inovinfo.data.remote.PendienteDto
import com.Electroinova.inovinfo.data.remote.SedeDto
import com.Electroinova.inovinfo.data.remote.SuministroDto
import com.Electroinova.inovinfo.data.remote.UnidadDto
import com.Electroinova.inovinfo.data.remote.VisitaHoyDto

// ── Catálogos (caché del servidor) ────────────────────────────────────────────

@Entity(tableName = "sedes")
data class SedeEntity(
    @PrimaryKey
    val id:     Int,
    val nombre: String
)

@Entity(tableName = "unidades")
data class UnidadEntity(
    @PrimaryKey
    val id:           Int,
    val numero:       String,
    val placa:        String? = null,
    @ColumnInfo(name = "sede_nombre")
    val sedeNombre:   String,
    @ColumnInfo(name = "sede_id")
    val sedeId:       Int
)

@Entity(tableName = "estados")
data class EstadoEntity(
    @PrimaryKey
    val id:        Int,
    val nombre:    String,
    @ColumnInfo(name = "color_hex")
    val colorHex:  String
)

@Entity(tableName = "suministros")
data class SuministroEntity(
    @PrimaryKey
    val id:        Int,
    val nombre:    String,
    val categoria: String?  = null,
    @ColumnInfo(name = "precio_ref")
    val precioRef: Double?  = null,
    val activo:    Boolean  = true
)

// ── Visitas locales (creadas offline, se sincronizan con el servidor) ─────────

@Entity(tableName = "visitas")
data class VisitaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId:                 Long    = 0,

    // Nulo hasta que se sincroniza exitosamente con el servidor
    @ColumnInfo(name = "server_id")
    val serverId:                Int?    = null,

    // "PENDING" | "SYNCED" | "ERROR"
    @ColumnInfo(name = "sync_status")
    val syncStatus:              String  = "PENDING",

    // Campos del formulario (espejo de CrearVisitaRequest)
    @ColumnInfo(name = "unidad_id")
    val unidadId:                Int,
    @ColumnInfo(name = "estado_id")
    val estadoId:                Int,
    @ColumnInfo(name = "novedad_texto")
    val novedadTexto:            String? = null,
    @ColumnInfo(name = "observacion_estructurada")
    val observacionEstructurada: String? = null,
    @ColumnInfo(name = "requiere_seguimiento")
    val requiereSeguimiento:     Boolean = false,
    @ColumnInfo(name = "fecha_seguimiento")
    val fechaSeguimiento:        String? = null,
    @ColumnInfo(name = "nota_seguimiento")
    val notaSeguimiento:         String? = null,

    // Marca de tiempo local de creación
    @ColumnInfo(name = "created_at_local")
    val createdAtLocal:          Long    = System.currentTimeMillis()
)

@Entity(
    tableName = "visita_suministros",
    foreignKeys = [
        ForeignKey(
            entity        = VisitaEntity::class,
            parentColumns = ["local_id"],
            childColumns  = ["visita_local_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("visita_local_id")]
)
data class VisitaSuministroEntity(
    @PrimaryKey(autoGenerate = true)
    val id:                Long = 0,
    @ColumnInfo(name = "visita_local_id")
    val visitaLocalId:     Long,
    @ColumnInfo(name = "suministro_id")
    val suministroId:      Int,
    val cantidad:          Int  = 1
)

@Entity(
    tableName = "visita_fotos",
    foreignKeys = [
        ForeignKey(
            entity        = VisitaEntity::class,
            parentColumns = ["local_id"],
            childColumns  = ["visita_local_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("visita_local_id")]
)
data class VisitaFotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id:            Long    = 0,
    @ColumnInfo(name = "visita_local_id")
    val visitaLocalId: Long,
    @ColumnInfo(name = "local_uri")
    val localUri:      String,
    @ColumnInfo(name = "remote_url")
    val remoteUrl:     String? = null,
    val uploaded:      Boolean = false
)

// ── Caché de listados del servidor ────────────────────────────────────────────

@Entity(tableName = "visitas_hoy_cache")
data class VisitaHoyCache(
    @PrimaryKey
    val id:                       Int,
    @ColumnInfo(name = "unidad_id")
    val unidadId:                 Int    = 0,
    @ColumnInfo(name = "unidad_numero")
    val unidadNumero:             String,
    @ColumnInfo(name = "sede_nombre")
    val sedeNombre:               String,
    @ColumnInfo(name = "sede_id")
    val sedeId:                   Int    = 0,
    val estado:                   String,
    @ColumnInfo(name = "color_hex")
    val colorHex:                 String,
    @ColumnInfo(name = "tecnico_nombre")
    val tecnicoNombre:            String,
    @ColumnInfo(name = "observacion_estructurada")
    val observacionEstructurada:  String? = null,
    @ColumnInfo(name = "fotos_count")
    val fotosCount:               Int    = 0
)

@Entity(tableName = "pendientes_cache")
data class PendienteCache(
    @PrimaryKey
    val id:                Int,
    @ColumnInfo(name = "unidad_id")
    val unidadId:          Int    = 0,
    @ColumnInfo(name = "unidad_numero")
    val unidadNumero:      String,
    @ColumnInfo(name = "sede_nombre")
    val sedeNombre:        String,
    @ColumnInfo(name = "fecha_seguimiento")
    val fechaSeguimiento:  String,
    @ColumnInfo(name = "nota_seguimiento")
    val notaSeguimiento:   String?,
    @ColumnInfo(name = "tecnico_nombre")
    val tecnicoNombre:     String,
    val urgencia:          String
)

// ── Extensiones: Entity ↔ DTO ─────────────────────────────────────────────────

fun SedeDto.toEntity()     = SedeEntity(id = id, nombre = nombre)
fun SedeEntity.toDto()     = SedeDto(id = id, nombre = nombre)

fun UnidadDto.toEntity()   = UnidadEntity(
    id         = id,
    numero     = numero,
    placa      = placa,
    sedeNombre = sede_nombre,
    sedeId     = sede_id
)
fun UnidadEntity.toDto()   = UnidadDto(
    id          = id,
    numero      = numero,
    placa       = placa,
    sede_nombre = sedeNombre,
    sede_id     = sedeId
)

fun EstadoDto.toEntity()   = EstadoEntity(id = id, nombre = nombre, colorHex = color_hex)
fun EstadoEntity.toDto()   = EstadoDto(id = id, nombre = nombre, color_hex = colorHex)

fun SuministroDto.toEntity() = SuministroEntity(
    id        = id,
    nombre    = nombre,
    categoria = categoria,
    precioRef = precio_ref,
    activo    = activo
)
fun SuministroEntity.toDto() = SuministroDto(
    id         = id,
    nombre     = nombre,
    categoria  = categoria,
    precio_ref = precioRef,
    activo     = activo
)

fun VisitaHoyDto.toCache()   = VisitaHoyCache(
    id                      = id,
    unidadId                = unidad_id,
    unidadNumero            = unidad_numero,
    sedeNombre              = sede_nombre,
    sedeId                  = sede_id,
    estado                  = estado,
    colorHex                = color_hex,
    tecnicoNombre           = tecnico_nombre,
    observacionEstructurada = observacion_estructurada,
    fotosCount              = fotos_count
)
fun VisitaHoyCache.toDto()   = VisitaHoyDto(
    id                       = id,
    unidad_id                = unidadId,
    unidad_numero            = unidadNumero,
    sede_nombre              = sedeNombre,
    sede_id                  = sedeId,
    estado                   = estado,
    color_hex                = colorHex,
    tecnico_nombre           = tecnicoNombre,
    observacion_estructurada = observacionEstructurada,
    fotos_count              = fotosCount
)

fun PendienteDto.toCache()   = PendienteCache(
    id               = id,
    unidadId         = unidad_id,
    unidadNumero     = unidad_numero,
    sedeNombre       = sede_nombre,
    fechaSeguimiento = fecha_seguimiento,
    notaSeguimiento  = nota_seguimiento,
    tecnicoNombre    = tecnico_nombre,
    urgencia         = urgencia
)
fun PendienteCache.toDto()   = PendienteDto(
    id               = id,
    unidad_id        = unidadId,
    unidad_numero    = unidadNumero,
    sede_nombre      = sedeNombre,
    fecha_seguimiento = fechaSeguimiento,
    nota_seguimiento = notaSeguimiento,
    tecnico_nombre   = tecnicoNombre,
    urgencia         = urgencia
)

-- ═══════════════════════════════════════════════════════════════════
-- InovInfo — Schema PostgreSQL
-- Electroinova Soluciones
-- ═══════════════════════════════════════════════════════════════════

-- ── Sedes ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sedes (
    id        SERIAL PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL UNIQUE,
    activa    BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO sedes (nombre) VALUES
    ('Tuluá'), ('Buga'), ('Sur Occidente'), ('Cali'), ('Río Paila')
ON CONFLICT DO NOTHING;

-- ── Usuarios ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS usuarios (
    id             SERIAL PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    rol            VARCHAR(20)  NOT NULL CHECK (rol IN ('coordinadora', 'tecnico')),
    activo         BOOLEAN DEFAULT TRUE,
    created_at     TIMESTAMP DEFAULT NOW()
);

-- ── Permisos granulares ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS permisos_usuario (
    id          SERIAL PRIMARY KEY,
    usuario_id  INTEGER REFERENCES usuarios(id) ON DELETE CASCADE,
    permiso     VARCHAR(50) NOT NULL,
    -- Permisos disponibles:
    -- EDITAR_CATALOGOS  → puede editar suministros y tipos de novedad
    -- VER_TODAS_VISITAS → puede ver visitas de otros técnicos
    -- EXPORTAR_SHEETS   → puede exportar a Google Sheets
    UNIQUE(usuario_id, permiso)
);

-- ── Unidades ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS unidades (
    id        SERIAL PRIMARY KEY,
    numero    VARCHAR(20)  NOT NULL UNIQUE,
    sede_id   INTEGER REFERENCES sedes(id),
    activa    BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ── Catálogo: estados de visita ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS estados_visita (
    id         SERIAL PRIMARY KEY,
    nombre     VARCHAR(50) NOT NULL UNIQUE,
    color_hex  VARCHAR(7)  DEFAULT '#9E9E9E',
    activo     BOOLEAN DEFAULT TRUE
);

INSERT INTO estados_visita (nombre, color_hex) VALUES
    ('OK',                   '#2E7D32'),
    ('Con novedad',          '#F9A825'),
    ('Inoperativa',          '#E24B4A'),
    ('Sin llaves',           '#757575'),
    ('Bat. desconectada',    '#9E9E9E')
ON CONFLICT DO NOTHING;

-- ── Catálogo: suministros ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS suministros (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    categoria   VARCHAR(100),
    precio_ref  DECIMAL(10,2),
    activo      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW()
);

INSERT INTO suministros (nombre, categoria) VALUES
    ('Sum. Accesorios general',               'General'),
    ('Suministro e Instalación Visor 7"',     'Pantallas'),
    ('Cable de cámara 2.5m',                  'Cableado'),
    ('Cable de cámara 1.5m',                  'Cableado'),
    ('Conectores tipo avión 4 pines',         'Conectores'),
    ('Fusibles',                               'Eléctrico')
ON CONFLICT DO NOTHING;

-- ── Catálogo: tipos de novedad ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tipos_novedad (
    id                   SERIAL PRIMARY KEY,
    nombre               VARCHAR(150) NOT NULL,
    requiere_descripcion BOOLEAN DEFAULT TRUE,
    activo               BOOLEAN DEFAULT TRUE
);

INSERT INTO tipos_novedad (nombre) VALUES
    ('Cámara dañada'),
    ('GPS sin señal'),
    ('Batería desconectada'),
    ('Pantalla sin imagen'),
    ('DVR no enciende'),
    ('Cable en mal estado'),
    ('Unidad sin movimiento')
ON CONFLICT DO NOTHING;

-- ── Visitas ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS visitas (
    id                       SERIAL PRIMARY KEY,
    unidad_id                INTEGER REFERENCES unidades(id),
    tecnico_id               INTEGER REFERENCES usuarios(id),
    estado_id                INTEGER REFERENCES estados_visita(id),
    novedad_texto            TEXT,
    observacion_estructurada TEXT,
    requiere_seguimiento     BOOLEAN DEFAULT FALSE,
    fecha_seguimiento        DATE,
    nota_seguimiento         TEXT,
    sincronizado             BOOLEAN DEFAULT TRUE,
    created_at               TIMESTAMP DEFAULT NOW()
);

-- ── Suministros usados en cada visita ────────────────────────────────
CREATE TABLE IF NOT EXISTS visita_suministros (
    id            SERIAL PRIMARY KEY,
    visita_id     INTEGER REFERENCES visitas(id) ON DELETE CASCADE,
    suministro_id INTEGER REFERENCES suministros(id),
    cantidad      INTEGER DEFAULT 1
);

-- ── Fotos de visitas ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS visita_fotos (
    id        SERIAL PRIMARY KEY,
    visita_id INTEGER REFERENCES visitas(id) ON DELETE CASCADE,
    url       TEXT NOT NULL,
    orden     INTEGER DEFAULT 0
);

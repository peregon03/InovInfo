-- Tabla para códigos de verificación de email y recuperación de contraseña
CREATE TABLE IF NOT EXISTS codigos_verificacion (
    id         SERIAL PRIMARY KEY,
    email      VARCHAR(150) NOT NULL,
    codigo     VARCHAR(6)   NOT NULL,
    tipo       VARCHAR(20)  NOT NULL CHECK (tipo IN ('registro', 'recuperacion')),
    datos_temp TEXT,
    usado      BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Índice para búsqueda rápida
CREATE INDEX IF NOT EXISTS idx_codigos_email_tipo ON codigos_verificacion(email, tipo);

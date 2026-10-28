const { google } = require('googleapis');

/**
 * Autenticación Google:
 *   - Sheets: cuenta de servicio (JWT)
 *   - Drive:  OAuth2 con refresh token (cuenta real con cuota de almacenamiento)
 *
 * Variables de entorno requeridas:
 *   GOOGLE_SERVICE_ACCOUNT_EMAIL  — email de la cuenta de servicio
 *   GOOGLE_PRIVATE_KEY            — clave privada (con \n reales)
 *   GOOGLE_SHEET_ID               — ID del archivo de Google Sheets destino
 *   GOOGLE_DRIVE_FOLDER_ID        — ID de la carpeta raíz en Drive para fotos
 *   GOOGLE_OAUTH_CLIENT_ID        — OAuth2 client ID
 *   GOOGLE_OAUTH_CLIENT_SECRET    — OAuth2 client secret
 *   GOOGLE_OAUTH_REFRESH_TOKEN    — Refresh token de la cuenta dueña del Drive
 */

let _sheets   = null;
let _drive    = null;
let _oauthDrive = null;

// ── Auth: Service Account (para Sheets) ─────────────────────────────────────
function getServiceAuth() {
    const key = (process.env.GOOGLE_PRIVATE_KEY || '').replace(/\\n/g, '\n');
    return new google.auth.JWT(
        process.env.GOOGLE_SERVICE_ACCOUNT_EMAIL,
        null,
        key,
        [
            'https://www.googleapis.com/auth/spreadsheets',
            'https://www.googleapis.com/auth/drive',
        ]
    );
}

// ── Auth: OAuth2 (para Drive — cuenta real con cuota) ───────────────────────
function getOAuth2Client() {
    const oauth2 = new google.auth.OAuth2(
        process.env.GOOGLE_OAUTH_CLIENT_ID,
        process.env.GOOGLE_OAUTH_CLIENT_SECRET
    );
    oauth2.setCredentials({
        refresh_token: process.env.GOOGLE_OAUTH_REFRESH_TOKEN,
    });
    return oauth2;
}

function getSheetsClient() {
    if (!_sheets) {
        _sheets = google.sheets({ version: 'v4', auth: getServiceAuth() });
    }
    return _sheets;
}

// Drive con OAuth2 (cuenta real) — fallback a service account si no hay OAuth
function getDriveClient() {
    if (process.env.GOOGLE_OAUTH_REFRESH_TOKEN) {
        if (!_oauthDrive) {
            _oauthDrive = google.drive({ version: 'v3', auth: getOAuth2Client() });
        }
        return _oauthDrive;
    }
    // Fallback: service account (funciona para listar/crear carpetas compartidas)
    if (!_drive) {
        _drive = google.drive({ version: 'v3', auth: getServiceAuth() });
    }
    return _drive;
}

// Mantener compatibilidad con getAuth() usado en informeService
function getAuth() {
    return getServiceAuth();
}

/**
 * Busca una subcarpeta por nombre dentro de un padre.
 * Si no existe, la crea. Devuelve el ID de la carpeta.
 */
async function obtenerOCrearCarpeta(drive, nombre, parentId) {
    // Buscar carpeta existente
    const busqueda = await drive.files.list({
        q: `name='${nombre}' and '${parentId}' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false`,
        fields: 'files(id)',
        spaces: 'drive',
    });

    if (busqueda.data.files.length > 0) {
        return busqueda.data.files[0].id;
    }

    // Crear carpeta
    const res = await drive.files.create({
        requestBody: {
            name:     nombre,
            mimeType: 'application/vnd.google-apps.folder',
            parents:  [parentId],
        },
        fields: 'id',
    });
    return res.data.id;
}

/**
 * Sube un archivo a Google Drive organizado en subcarpetas: /año/sede/
 *
 * @param {Buffer} fileBuffer  - Contenido del archivo en memoria
 * @param {string} fileName    - Nombre del archivo a crear en Drive
 * @param {string} mimeType    - Tipo MIME (ej. "image/jpeg")
 * @param {string} [sedeNombre] - Nombre de la sede (para subcarpeta)
 * @returns {Promise<string>}  - URL pública: https://drive.google.com/uc?id=<fileId>
 */
async function subirFotoDrive(fileBuffer, fileName, mimeType, sedeNombre) {
    const drive    = getDriveClient();
    const rootId   = process.env.GOOGLE_DRIVE_FOLDER_ID;

    // Determinar carpeta destino: raíz → año → sede → fecha (YYYY-MM-DD)
    let targetFolder = rootId || null;
    if (rootId) {
        const ahora = new Date();
        const anio  = ahora.getFullYear().toString();
        const fecha = ahora.toISOString().split('T')[0]; // YYYY-MM-DD

        const carpetaAnio = await obtenerOCrearCarpeta(drive, anio, rootId);
        const carpetaSede = sedeNombre
            ? await obtenerOCrearCarpeta(drive, sedeNombre, carpetaAnio)
            : carpetaAnio;
        targetFolder = await obtenerOCrearCarpeta(drive, fecha, carpetaSede);
    }

    const { Readable } = require('stream');
    const stream = Readable.from(fileBuffer);

    // Crear el archivo en Drive (como la cuenta OAuth2 — tiene cuota)
    const fileRes = await drive.files.create({
        requestBody: {
            name:    fileName,
            parents: targetFolder ? [targetFolder] : [],
        },
        media: {
            mimeType,
            body: stream,
        },
        fields: 'id',
    });

    const fileId = fileRes.data.id;

    // Hacer el archivo público (cualquiera con el enlace puede leer)
    await drive.permissions.create({
        fileId,
        requestBody: {
            role: 'reader',
            type: 'anyone',
        },
    });

    return `https://drive.google.com/uc?id=${fileId}`;
}

module.exports = { getSheetsClient, getDriveClient, getAuth, subirFotoDrive };

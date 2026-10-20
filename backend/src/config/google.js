const { google } = require('googleapis');

/**
 * Autenticación con cuenta de servicio de Google.
 *
 * Variables de entorno requeridas:
 *   GOOGLE_SERVICE_ACCOUNT_EMAIL  — email de la cuenta de servicio
 *   GOOGLE_PRIVATE_KEY            — clave privada (con \n reales)
 *   GOOGLE_SHEET_ID               — ID del archivo de Google Sheets destino
 *   GOOGLE_DRIVE_FOLDER_ID        — ID de la carpeta raíz en Drive para fotos (opcional)
 */

let _sheets = null;
let _drive  = null;

function getAuth() {
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

function getSheetsClient() {
    if (!_sheets) {
        _sheets = google.sheets({ version: 'v4', auth: getAuth() });
    }
    return _sheets;
}

function getDriveClient() {
    if (!_drive) {
        _drive = google.drive({ version: 'v3', auth: getAuth() });
    }
    return _drive;
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

    // Determinar carpeta destino: raíz → año → sede
    let targetFolder = rootId || null;
    if (rootId) {
        const anio = new Date().getFullYear().toString();
        const carpetaAnio = await obtenerOCrearCarpeta(drive, anio, rootId);
        if (sedeNombre) {
            targetFolder = await obtenerOCrearCarpeta(drive, sedeNombre, carpetaAnio);
        } else {
            targetFolder = carpetaAnio;
        }
    }

    const { Readable } = require('stream');
    const stream = Readable.from(fileBuffer);

    // Crear el archivo en Drive
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

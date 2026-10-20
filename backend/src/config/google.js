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
 * Sube un archivo a Google Drive y lo hace públicamente accesible.
 *
 * @param {Buffer} fileBuffer  - Contenido del archivo en memoria
 * @param {string} fileName    - Nombre del archivo a crear en Drive
 * @param {string} mimeType    - Tipo MIME (ej. "image/jpeg")
 * @returns {Promise<string>}  - URL pública: https://drive.google.com/uc?id=<fileId>
 */
async function subirFotoDrive(fileBuffer, fileName, mimeType) {
    const drive    = getDriveClient();
    const folderId = process.env.GOOGLE_DRIVE_FOLDER_ID;

    const { Readable } = require('stream');
    const stream = Readable.from(fileBuffer);

    // Crear el archivo en Drive
    const fileRes = await drive.files.create({
        requestBody: {
            name:    fileName,
            parents: folderId ? [folderId] : [],
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

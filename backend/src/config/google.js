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

module.exports = { getSheetsClient, getDriveClient, getAuth };

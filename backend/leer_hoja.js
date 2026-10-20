// Script temporal para leer una pestaña del Sheet
require('dotenv').config();
const { getSheetsClient } = require('./src/config/google');

async function main() {
    const sheets = getSheetsClient();
    const spreadsheetId = process.env.GOOGLE_SHEET_ID;

    // Leer la pestaña
    const res = await sheets.spreadsheets.values.get({
        spreadsheetId,
        range: "'Buga-Tulua-Sur occ 26Sept2026'!A1:E100",
    });

    const rows = res.data.values || [];
    for (const row of rows) {
        console.log(row.join(' | '));
    }
}

main().catch(console.error);

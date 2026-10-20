const router  = require('express').Router();
const multer  = require('multer');
const ExcelJS = require('exceljs');
const pool    = require('../config/db');
const { verificarToken, soloCoordinadora } = require('../middleware/auth');

// ── Multer en memoria (máx. 10 MB) ───────────────────────────────────────────
const upload = multer({
    storage: multer.memoryStorage(),
    limits:  { fileSize: 10 * 1024 * 1024 },
    fileFilter: (_req, file, cb) => {
        if (file.originalname.toLowerCase().endsWith('.xlsx')) {
            cb(null, true);
        } else {
            cb(new Error('Solo se aceptan archivos .xlsx'));
        }
    }
});

// ── POST /api/importar/excel ──────────────────────────────────────────────────
router.post(
    '/excel',
    verificarToken,
    soloCoordinadora,
    upload.single('archivo'),
    async (req, res) => {
        if (!req.file) {
            return res.status(400).json({ error: 'Archivo .xlsx requerido' });
        }

        try {
            const workbook = new ExcelJS.Workbook();
            await workbook.xlsx.load(req.file.buffer);

            const hoja = workbook.worksheets[0];
            if (!hoja) {
                return res.status(400).json({ error: 'El archivo no contiene hojas' });
            }

            // Recolectar filas válidas (saltando la primera fila de encabezado)
            const filas  = [];
            const errores = [];

            hoja.eachRow((row, rowNumber) => {
                if (rowNumber === 1) return; // encabezado

                const sede   = String(row.getCell(1).value ?? '').trim();
                const unidad = String(row.getCell(2).value ?? '').trim();

                if (!sede || !unidad) {
                    errores.push(`Fila ${rowNumber}: sede o unidad vacíos`);
                    return;
                }
                filas.push({ sede, unidad });
            });

            if (filas.length === 0 && errores.length === 0) {
                return res.status(400).json({ error: 'El archivo no tiene filas de datos' });
            }

            // Obtener nombres de sede únicos
            const nombresSedesUnicos = [...new Set(filas.map(f => f.sede))];

            let sedesCreadas   = 0;
            let unidadesCreadas = 0;

            // Insertar sedes ignorando duplicados
            for (const nombreSede of nombresSedesUnicos) {
                const result = await pool.query(
                    `INSERT INTO sedes (nombre) VALUES ($1)
                     ON CONFLICT (nombre) DO NOTHING
                     RETURNING id`,
                    [nombreSede]
                );
                if (result.rows.length > 0) sedesCreadas++;
            }

            // Insertar unidades
            for (const fila of filas) {
                try {
                    // Obtener el id de la sede (ya existente o recién creada)
                    const sedeResult = await pool.query(
                        'SELECT id FROM sedes WHERE nombre = $1',
                        [fila.sede]
                    );

                    if (sedeResult.rows.length === 0) {
                        errores.push(`Sede '${fila.sede}' no encontrada para unidad '${fila.unidad}'`);
                        continue;
                    }

                    const sedeId = sedeResult.rows[0].id;

                    const insResult = await pool.query(
                        `INSERT INTO unidades (numero, sede_id) VALUES ($1, $2)
                         ON CONFLICT (numero, sede_id) DO NOTHING
                         RETURNING id`,
                        [fila.unidad, sedeId]
                    );

                    if (insResult.rows.length > 0) unidadesCreadas++;

                } catch (err) {
                    errores.push(`Error en unidad '${fila.unidad}': ${err.message}`);
                }
            }

            res.json({ sedesCreadas, unidadesCreadas, errores });

        } catch (err) {
            console.error('[POST /importar/excel]', err.message);
            res.status(500).json({ error: `Error al procesar el archivo: ${err.message}` });
        }
    }
);

module.exports = router;

const router = require('express').Router();
const { verificarToken, coordinadoraOPermiso } = require('../middleware/auth');
const { generarInforme } = require('../services/informeService');

router.use(verificarToken);

/**
 * POST /api/informes/generar
 *
 * Body:
 *   fechaDesde  — string 'YYYY-MM-DD' (requerido)
 *   fechaHasta  — string 'YYYY-MM-DD' (requerido)
 *   sedeId      — number (opcional, filtra por sede)
 *   sedeNombre  — string (opcional, para el nombre de la pestaña)
 *   linkFotos   — string (opcional, link a la carpeta de fotos en Drive)
 *
 * Response:
 *   { sheetUrl, pestana, totalVisitas }
 */
router.post('/generar', coordinadoraOPermiso('EXPORTAR_SHEETS'), async (req, res) => {
    const { fechaDesde, fechaHasta, sedeId, sedeNombre, linkFotos } = req.body;

    if (!fechaDesde || !fechaHasta) {
        return res.status(400).json({ error: 'fechaDesde y fechaHasta son requeridos (formato YYYY-MM-DD)' });
    }

    // Validar formato de fecha básico
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (!dateRegex.test(fechaDesde) || !dateRegex.test(fechaHasta)) {
        return res.status(400).json({ error: 'Las fechas deben tener formato YYYY-MM-DD' });
    }

    try {
        const resultado = await generarInforme({
            fechaDesde,
            fechaHasta,
            sedeId: sedeId || null,
            sedeNombre: sedeNombre || null,
            linkFotos: linkFotos || null
        });

        res.json(resultado);

    } catch (err) {
        console.error('Error generando informe:', err.message);

        if (err.message.includes('No hay visitas')) {
            return res.status(404).json({ error: err.message });
        }
        if (err.message.includes('GOOGLE_SHEET_ID')) {
            return res.status(500).json({ error: 'Google Sheets no está configurado en el servidor' });
        }

        res.status(500).json({ error: 'Error al generar el informe: ' + err.message });
    }
});

/**
 * GET /api/informes/sedes
 * Lista las sedes disponibles para el selector de la app.
 */
router.get('/sedes', async (req, res) => {
    try {
        const pool = require('../config/db');
        const result = await pool.query('SELECT id, nombre FROM sedes WHERE activa = TRUE ORDER BY nombre');
        res.json(result.rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;

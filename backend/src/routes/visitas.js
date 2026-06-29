const router = require('express').Router();
const pool   = require('../config/db');
const { verificarToken, coordinadoraOPermiso } = require('../middleware/auth');

router.use(verificarToken);

// GET /api/visitas — coordinadora ve todas, técnico ve solo las suyas
router.get('/', async (req, res) => {
    try {
        const esCoordinadora  = req.usuario.rol === 'coordinadora';
        const tienePermiso    = req.usuario.permisos?.includes('VER_TODAS_VISITAS');
        const verTodas        = esCoordinadora || tienePermiso;

        const query = `
            SELECT v.id, v.created_at, v.requiere_seguimiento,
                   v.fecha_seguimiento, v.nota_seguimiento,
                   v.novedad_texto, v.observacion_estructurada,
                   u2.nombre  AS tecnico_nombre,
                   un.numero  AS unidad_numero,
                   s.nombre   AS sede_nombre,
                   ev.nombre  AS estado,
                   ev.color_hex,
                   COALESCE(
                       json_agg(
                           json_build_object('nombre', sm.nombre, 'cantidad', vs.cantidad)
                       ) FILTER (WHERE sm.id IS NOT NULL), '[]'
                   ) AS suministros,
                   COALESCE(
                       json_agg(vf.url) FILTER (WHERE vf.id IS NOT NULL), '[]'
                   ) AS fotos
            FROM visitas v
            JOIN usuarios u2      ON v.tecnico_id   = u2.id
            JOIN unidades un      ON v.unidad_id    = un.id
            JOIN sedes s          ON un.sede_id     = s.id
            JOIN estados_visita ev ON v.estado_id   = ev.id
            LEFT JOIN visita_suministros vs ON v.id = vs.visita_id
            LEFT JOIN suministros sm        ON vs.suministro_id = sm.id
            LEFT JOIN visita_fotos vf       ON v.id = vf.visita_id
            ${verTodas ? '' : 'WHERE v.tecnico_id = $1'}
            GROUP BY v.id, u2.nombre, un.numero, s.nombre, ev.nombre, ev.color_hex
            ORDER BY v.created_at DESC
        `;

        const params = verTodas ? [] : [req.usuario.id];
        const result = await pool.query(query, params);
        res.json(result.rows);

    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// GET /api/visitas/hoy
router.get('/hoy', async (req, res) => {
    try {
        const esCoordinadora = req.usuario.rol === 'coordinadora';
        const tienePermiso   = req.usuario.permisos?.includes('VER_TODAS_VISITAS');
        const verTodas       = esCoordinadora || tienePermiso;

        const query = `
            SELECT v.id, un.numero AS unidad_numero, s.nombre AS sede_nombre,
                   ev.nombre AS estado, ev.color_hex, u2.nombre AS tecnico_nombre,
                   v.observacion_estructurada,
                   (SELECT COUNT(*) FROM visita_fotos vf WHERE vf.visita_id = v.id) AS fotos_count
            FROM visitas v
            JOIN unidades un       ON v.unidad_id  = un.id
            JOIN sedes s           ON un.sede_id   = s.id
            JOIN estados_visita ev ON v.estado_id  = ev.id
            JOIN usuarios u2       ON v.tecnico_id = u2.id
            WHERE DATE(v.created_at) = CURRENT_DATE
            ${verTodas ? '' : 'AND v.tecnico_id = $1'}
            ORDER BY v.created_at DESC
        `;

        const params = verTodas ? [] : [req.usuario.id];
        const result = await pool.query(query, params);
        res.json(result.rows);

    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// GET /api/visitas/pendientes
router.get('/pendientes', async (req, res) => {
    try {
        const esCoordinadora = req.usuario.rol === 'coordinadora';
        const tienePermiso   = req.usuario.permisos?.includes('VER_TODAS_VISITAS');
        const verTodas       = esCoordinadora || tienePermiso;

        const query = `
            SELECT v.id, un.numero AS unidad_numero, s.nombre AS sede_nombre,
                   v.fecha_seguimiento, v.nota_seguimiento,
                   u2.nombre AS tecnico_nombre,
                   CASE
                       WHEN v.fecha_seguimiento < CURRENT_DATE THEN 'vencido'
                       WHEN v.fecha_seguimiento = CURRENT_DATE THEN 'hoy'
                       ELSE 'futuro'
                   END AS urgencia
            FROM visitas v
            JOIN unidades un ON v.unidad_id  = un.id
            JOIN sedes s     ON un.sede_id   = s.id
            JOIN usuarios u2 ON v.tecnico_id = u2.id
            WHERE v.requiere_seguimiento = TRUE
              AND v.fecha_seguimiento IS NOT NULL
            ${verTodas ? '' : 'AND v.tecnico_id = $1'}
            ORDER BY v.fecha_seguimiento ASC
        `;

        const params = verTodas ? [] : [req.usuario.id];
        const result = await pool.query(query, params);
        res.json(result.rows);

    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// POST /api/visitas
router.post('/', async (req, res) => {
    const {
        unidad_id, estado_id, novedad_texto, observacion_estructurada,
        requiere_seguimiento, fecha_seguimiento, nota_seguimiento,
        suministros  // array de { suministro_id, cantidad }
    } = req.body;

    if (!unidad_id || !estado_id) {
        return res.status(400).json({ error: 'Unidad y estado son requeridos' });
    }

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        const visitaResult = await client.query(`
            INSERT INTO visitas
                (unidad_id, tecnico_id, estado_id, novedad_texto,
                 observacion_estructurada, requiere_seguimiento,
                 fecha_seguimiento, nota_seguimiento)
            VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
            RETURNING *
        `, [
            unidad_id, req.usuario.id, estado_id, novedad_texto,
            observacion_estructurada, requiere_seguimiento || false,
            fecha_seguimiento || null, nota_seguimiento || null
        ]);

        const visita = visitaResult.rows[0];

        if (suministros && suministros.length > 0) {
            for (const s of suministros) {
                await client.query(
                    'INSERT INTO visita_suministros (visita_id, suministro_id, cantidad) VALUES ($1,$2,$3)',
                    [visita.id, s.suministro_id, s.cantidad || 1]
                );
            }
        }

        await client.query('COMMIT');
        res.status(201).json(visita);

    } catch (err) {
        await client.query('ROLLBACK');
        res.status(500).json({ error: err.message });
    } finally {
        client.release();
    }
});

module.exports = router;

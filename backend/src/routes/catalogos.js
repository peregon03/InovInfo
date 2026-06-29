const router = require('express').Router();
const pool   = require('../config/db');
const { verificarToken, coordinadoraOPermiso } = require('../middleware/auth');

router.use(verificarToken);

// ── SEDES (solo lectura para todos) ──────────────────────────────────────────
router.get('/sedes', async (req, res) => {
    const result = await pool.query('SELECT * FROM sedes WHERE activa = TRUE ORDER BY nombre');
    res.json(result.rows);
});

// ── UNIDADES ──────────────────────────────────────────────────────────────────
router.get('/unidades', async (req, res) => {
    const result = await pool.query(`
        SELECT u.id, u.numero, u.activa, s.nombre AS sede_nombre, s.id AS sede_id
        FROM unidades u
        JOIN sedes s ON u.sede_id = s.id
        WHERE u.activa = TRUE
        ORDER BY u.numero
    `);
    res.json(result.rows);
});

router.post('/unidades', coordinadoraOPermiso('EDITAR_CATALOGOS'), async (req, res) => {
    const { numero, sede_id } = req.body;
    if (!numero || !sede_id) return res.status(400).json({ error: 'Número y sede requeridos' });
    try {
        const result = await pool.query(
            'INSERT INTO unidades (numero, sede_id) VALUES ($1,$2) RETURNING *',
            [numero, sede_id]
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        if (err.code === '23505') return res.status(409).json({ error: 'Número de unidad ya existe' });
        res.status(500).json({ error: err.message });
    }
});

router.put('/unidades/:id', coordinadoraOPermiso('EDITAR_CATALOGOS'), async (req, res) => {
    const { numero, sede_id, activa } = req.body;
    try {
        const result = await pool.query(
            'UPDATE unidades SET numero=$1, sede_id=$2, activa=$3 WHERE id=$4 RETURNING *',
            [numero, sede_id, activa, req.params.id]
        );
        res.json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

router.delete('/unidades/:id', coordinadoraOPermiso('EDITAR_CATALOGOS'), async (req, res) => {
    await pool.query('UPDATE unidades SET activa = FALSE WHERE id = $1', [req.params.id]);
    res.json({ mensaje: 'Unidad desactivada' });
});

// ── SUMINISTROS ───────────────────────────────────────────────────────────────
router.get('/suministros', async (req, res) => {
    const result = await pool.query(
        'SELECT * FROM suministros WHERE activo = TRUE ORDER BY categoria, nombre'
    );
    res.json(result.rows);
});

router.post('/suministros', coordinadoraOPermiso('EDITAR_CATALOGOS'), async (req, res) => {
    const { nombre, categoria, precio_ref } = req.body;
    if (!nombre) return res.status(400).json({ error: 'Nombre requerido' });
    try {
        const result = await pool.query(
            'INSERT INTO suministros (nombre, categoria, precio_ref) VALUES ($1,$2,$3) RETURNING *',
            [nombre, categoria || null, precio_ref || null]
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

router.put('/suministros/:id', coordinadoraOPermiso('EDITAR_CATALOGOS'), async (req, res) => {
    const { nombre, categoria, precio_ref, activo } = req.body;
    try {
        const result = await pool.query(
            'UPDATE suministros SET nombre=$1, categoria=$2, precio_ref=$3, activo=$4 WHERE id=$5 RETURNING *',
            [nombre, categoria, precio_ref, activo, req.params.id]
        );
        res.json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ── TIPOS DE NOVEDAD ──────────────────────────────────────────────────────────
router.get('/tipos-novedad', async (req, res) => {
    const result = await pool.query(
        'SELECT * FROM tipos_novedad WHERE activo = TRUE ORDER BY nombre'
    );
    res.json(result.rows);
});

router.post('/tipos-novedad', coordinadoraOPermiso('EDITAR_CATALOGOS'), async (req, res) => {
    const { nombre, requiere_descripcion } = req.body;
    if (!nombre) return res.status(400).json({ error: 'Nombre requerido' });
    try {
        const result = await pool.query(
            'INSERT INTO tipos_novedad (nombre, requiere_descripcion) VALUES ($1,$2) RETURNING *',
            [nombre, requiere_descripcion ?? true]
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

router.put('/tipos-novedad/:id', coordinadoraOPermiso('EDITAR_CATALOGOS'), async (req, res) => {
    const { nombre, requiere_descripcion, activo } = req.body;
    try {
        const result = await pool.query(
            'UPDATE tipos_novedad SET nombre=$1, requiere_descripcion=$2, activo=$3 WHERE id=$4 RETURNING *',
            [nombre, requiere_descripcion, activo, req.params.id]
        );
        res.json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ── ESTADOS DE VISITA ─────────────────────────────────────────────────────────
router.get('/estados', async (req, res) => {
    const result = await pool.query(
        'SELECT * FROM estados_visita WHERE activo = TRUE ORDER BY id'
    );
    res.json(result.rows);
});

module.exports = router;

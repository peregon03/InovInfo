const router  = require('express').Router();
const bcrypt  = require('bcryptjs');
const jwt     = require('jsonwebtoken');
const pool    = require('../config/db');
const { verificarToken, soloCoordinadora } = require('../middleware/auth');

// ── POST /api/auth/login ──────────────────────────────────────────────────────
router.post('/login', async (req, res) => {
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({ error: 'Email y contraseña requeridos' });
    }

    try {
        const result = await pool.query(
            'SELECT * FROM usuarios WHERE email = $1 AND activo = TRUE',
            [email.toLowerCase().trim()]
        );

        const usuario = result.rows[0];
        if (!usuario) {
            return res.status(401).json({ error: 'Credenciales inválidas' });
        }

        const passwordValido = await bcrypt.compare(password, usuario.password_hash);
        if (!passwordValido) {
            return res.status(401).json({ error: 'Credenciales inválidas' });
        }

        // Obtener permisos del usuario
        const permisosResult = await pool.query(
            'SELECT permiso FROM permisos_usuario WHERE usuario_id = $1',
            [usuario.id]
        );
        const permisos = permisosResult.rows.map(r => r.permiso);

        const token = jwt.sign(
            {
                id:       usuario.id,
                nombre:   usuario.nombre,
                email:    usuario.email,
                rol:      usuario.rol,
                permisos: permisos
            },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN }
        );

        res.json({
            token,
            usuario: {
                id:       usuario.id,
                nombre:   usuario.nombre,
                email:    usuario.email,
                rol:      usuario.rol,
                permisos: permisos
            }
        });

    } catch (err) {
        console.error('Error en login:', err.message);
        res.status(500).json({ error: 'Error interno del servidor' });
    }
});

// ── POST /api/auth/register (solo coordinadora) ───────────────────────────────
router.post('/register', verificarToken, soloCoordinadora, async (req, res) => {
    const { nombre, email, password, rol } = req.body;

    if (!nombre || !email || !password || !rol) {
        return res.status(400).json({ error: 'Todos los campos son requeridos' });
    }

    if (!['coordinadora', 'tecnico'].includes(rol)) {
        return res.status(400).json({ error: 'Rol inválido' });
    }

    try {
        const existe = await pool.query(
            'SELECT id FROM usuarios WHERE email = $1',
            [email.toLowerCase().trim()]
        );

        if (existe.rows.length > 0) {
            return res.status(409).json({ error: 'El email ya está registrado' });
        }

        const passwordHash = await bcrypt.hash(password, 12);

        const result = await pool.query(
            `INSERT INTO usuarios (nombre, email, password_hash, rol)
             VALUES ($1, $2, $3, $4) RETURNING id, nombre, email, rol`,
            [nombre.trim(), email.toLowerCase().trim(), passwordHash, rol]
        );

        res.status(201).json({ usuario: result.rows[0] });

    } catch (err) {
        console.error('Error en register:', err.message);
        res.status(500).json({ error: 'Error interno del servidor' });
    }
});

// ── GET /api/auth/me ──────────────────────────────────────────────────────────
router.get('/me', verificarToken, async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT id, nombre, email, rol, created_at FROM usuarios WHERE id = $1',
            [req.usuario.id]
        );
        const permisosResult = await pool.query(
            'SELECT permiso FROM permisos_usuario WHERE usuario_id = $1',
            [req.usuario.id]
        );
        res.json({
            ...result.rows[0],
            permisos: permisosResult.rows.map(r => r.permiso)
        });
    } catch (err) {
        res.status(500).json({ error: 'Error interno del servidor' });
    }
});

module.exports = router;

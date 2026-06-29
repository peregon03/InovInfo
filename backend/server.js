require('dotenv').config();
const app  = require('./src/app');
const pool = require('./src/config/db');

const PORT = process.env.PORT || 3000;

// Verificar conexión a BD antes de arrancar
pool.query('SELECT NOW()', (err) => {
    if (err) {
        console.error('❌ No se pudo conectar a PostgreSQL:', err.message);
        process.exit(1);
    }
    console.log('✅ PostgreSQL conectado');
    app.listen(PORT, '0.0.0.0', () => {
        console.log(`🚀 InovInfo API corriendo en puerto ${PORT}`);
        console.log(`   Entorno: ${process.env.NODE_ENV}`);
    });
});

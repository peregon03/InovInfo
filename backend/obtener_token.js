/**
 * Script de un solo uso para obtener el refresh_token de Google OAuth2.
 *
 * 1. Agrega GOOGLE_OAUTH_CLIENT_ID y GOOGLE_OAUTH_CLIENT_SECRET al .env
 * 2. Corre: node obtener_token.js
 * 3. Abre la URL en el navegador, autoriza con electroinova.soluciones@gmail.com
 * 4. Copia el refresh_token y agrégalo al .env como GOOGLE_OAUTH_REFRESH_TOKEN
 * 5. Reinicia pm2: pm2 restart inovinfo-api
 * 6. Puedes borrar este script después.
 */

require('dotenv').config();
const { google } = require('googleapis');
const http = require('http');
const url  = require('url');

const CLIENT_ID     = process.env.GOOGLE_OAUTH_CLIENT_ID;
const CLIENT_SECRET = process.env.GOOGLE_OAUTH_CLIENT_SECRET;
const REDIRECT_URI  = 'http://localhost:3000/oauth/callback';

if (!CLIENT_ID || !CLIENT_SECRET) {
    console.error('❌ Falta GOOGLE_OAUTH_CLIENT_ID o GOOGLE_OAUTH_CLIENT_SECRET en .env');
    process.exit(1);
}

const oauth2Client = new google.auth.OAuth2(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);

const authUrl = oauth2Client.generateAuthUrl({
    access_type: 'offline',
    prompt:      'consent',
    scope: [
        'https://www.googleapis.com/auth/drive',
    ],
});

console.log('\n🔗 Abre esta URL en tu navegador:\n');
console.log(authUrl);
console.log('\n⏳ Esperando autorización...\n');

// Servidor temporal para capturar el callback
const server = http.createServer(async (req, res) => {
    const parsed = url.parse(req.url, true);
    if (parsed.pathname !== '/oauth/callback') {
        res.writeHead(404);
        res.end('Not found');
        return;
    }

    const code = parsed.query.code;
    if (!code) {
        res.writeHead(400);
        res.end('No code received');
        return;
    }

    try {
        const { tokens } = await oauth2Client.getToken(code);

        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end('<h1>✅ Token obtenido! Puedes cerrar esta pestaña.</h1><p>Revisa la terminal.</p>');

        console.log('✅ Tokens obtenidos:\n');
        console.log('refresh_token:', tokens.refresh_token);
        console.log('\n📋 Agrega esto al .env del servidor:\n');
        console.log(`GOOGLE_OAUTH_REFRESH_TOKEN=${tokens.refresh_token}`);
        console.log('\nLuego reinicia: pm2 restart inovinfo-api');

        server.close();
        process.exit(0);
    } catch (err) {
        res.writeHead(500);
        res.end('Error: ' + err.message);
        console.error('❌ Error obteniendo token:', err.message);
    }
});

server.listen(3000, () => {
    console.log('Servidor temporal en http://localhost:3000');
    console.log('(Se cerrará automáticamente después de obtener el token)');
});

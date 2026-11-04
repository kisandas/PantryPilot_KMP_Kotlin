const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const port = Number(process.env.PORT || 8094);
const root = path.resolve(__dirname, '../webApp/build/dist/js/developmentExecutable');
if (!fs.existsSync(path.join(root, 'index.html')) || !fs.existsSync(path.join(root, 'webApp.js'))) {
  console.error('Web build is missing. Run: .\\gradlew.bat :webApp:packageWebPreview --no-configuration-cache');
  process.exit(1);
}
const server = http.createServer((req,res) => {
  let relative;
  try { relative = decodeURIComponent(req.url.split('?')[0]); } catch { res.writeHead(400).end(); return; }
  let file = path.resolve(root, '.' + (relative === '/' ? '/index.html' : relative));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  const mime = {'.html':'text/html','.js':'application/javascript','.mjs':'application/javascript','.wasm':'application/wasm','.css':'text/css','.json':'application/json','.svg':'image/svg+xml','.png':'image/png','.jpg':'image/jpeg','.jpeg':'image/jpeg','.ttf':'font/ttf'}[path.extname(file)] || 'application/octet-stream';
  fs.readFile(file,(err,body) => {
    if(err) { res.writeHead(404,{'Content-Type':'text/plain','Cache-Control':'no-store'}).end('Build asset not found. Rebuild the web preview, then refresh.');return; }
    res.writeHead(200,{'Content-Type':mime,'Cache-Control':'no-store'});res.end(body);
  });
});
server.on('error', error => {
  console.error(error.code === 'EADDRINUSE' ? 'Port ' + port + ' is already in use. Stop the old preview with Ctrl+C, then restart this command.' : error.message);
  process.exitCode = 1;
});
server.listen(port,'127.0.0.1',() => {
  console.log('PantryPilot preview: http://localhost:' + port);
  console.log('Serving: ' + root);
});




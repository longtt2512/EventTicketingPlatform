import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../frontend/dist');
const port = Number(process.env.FRONTEND_PORT ?? 14200);
const gateway = new URL(process.env.API_GATEWAY_URL ?? 'http://127.0.0.1:18080');
const apps = [['/org', 'organizer'], ['/admin', 'admin'], ['/staff', 'checkin'], ['', 'customer']]
  .map(([prefix, name]) => ({ prefix, name, directory: path.join(root, `${name}-web/browser`) }));
const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.json': 'application/json', '.woff2': 'font/woff2', '.ttf': 'font/ttf', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg' };
for (const app of apps) {
  if (!fs.existsSync(path.join(app.directory, 'index.html'))) throw new Error(`Build ${app.name}-web before serving.`);
}
const server = http.createServer((req, res) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('X-Frame-Options', 'DENY');
  if (req.url.startsWith('/api/')) {
    const upstream = http.request(new URL(req.url, gateway), { method: req.method, headers: { ...req.headers, host: gateway.host } }, response => {
      res.writeHead(response.statusCode, response.headers); response.pipe(res);
    });
    upstream.setTimeout(10000, () => upstream.destroy());
    upstream.on('error', () => { if (!res.headersSent) res.writeHead(502, { 'Content-Type': 'application/problem+json' }); res.end('{"code":"SERVICE_UNAVAILABLE","detail":"API is unavailable"}'); });
    req.pipe(upstream);
    return;
  }
  if (!['GET', 'HEAD'].includes(req.method)) { res.writeHead(405, { Allow: 'GET, HEAD' }); res.end(); return; }
  let url, pathname;
  try { url = new URL(req.url, 'http://local'); pathname = decodeURIComponent(url.pathname); } catch { res.writeHead(400); res.end(); return; }
  const app = apps.find(a => !a.prefix || pathname === a.prefix || pathname.startsWith(a.prefix + '/'));
  if (app.prefix && pathname === app.prefix) { res.writeHead(308, { Location: app.prefix + '/' + url.search }); res.end(); return; }
  const relative = pathname.slice(app.prefix.length);
  const target = path.resolve(app.directory, '.' + relative);
  if (!target.startsWith(app.directory + path.sep) && target !== app.directory) { res.writeHead(403); res.end(); return; }
  const exists = fs.existsSync(target) && fs.statSync(target).isFile();
  // Missing resources must be 404, never an HTML document disguised as JS or an image.
  if (!exists && (path.extname(relative) || relative.startsWith('/assets/'))) { res.writeHead(404); res.end(); return; }
  const file = exists ? target : path.join(app.directory, 'index.html');
  res.writeHead(200, { 'Content-Type': types[path.extname(file)] ?? 'application/octet-stream', 'Cache-Control': 'no-store' });
  if (req.method === 'HEAD') res.end(); else fs.createReadStream(file).pipe(res);
});
server.listen(port, '127.0.0.1', () => apps.forEach(app => console.log(`${app.name}-web http://localhost:${port}${app.prefix}/`)));
process.on('SIGTERM', () => server.close()); process.on('SIGINT', () => server.close());

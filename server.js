const http = require('http');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const PORT = 3000;
const ASSETS_DIR = path.join(__dirname, 'app/src/main/assets');
const FONTS_DIR = path.join(__dirname, 'app/src/main/res/font');

console.log('Loading Quran data in memory...');
let surahsMeta = [];
let juzMeta = [];
let pages16 = [];
let pages604 = [];
let searchList = [];

try {
  surahsMeta = JSON.parse(fs.readFileSync(path.join(ASSETS_DIR, 'surahs_meta.json'), 'utf8'));
  juzMeta = JSON.parse(fs.readFileSync(path.join(ASSETS_DIR, 'juz_meta.json'), 'utf8'));
  pages16 = JSON.parse(zlib.gunzipSync(fs.readFileSync(path.join(ASSETS_DIR, 'quran_16lines.json.gz'))));
  pages604 = JSON.parse(zlib.gunzipSync(fs.readFileSync(path.join(ASSETS_DIR, 'quran_604pages.json.gz'))));
  const rawUthmani = JSON.parse(zlib.gunzipSync(fs.readFileSync(path.join(ASSETS_DIR, 'quran_uthmani.json.gz'))));
  
  // Build fast search list
  rawUthmani.forEach(s => {
    s.ayahs.forEach(a => {
      searchList.push({
        surahNumber: s.number,
        surahName: s.name,
        ayahNumber: a.numberInSurah,
        text: a.text,
        page604: a.page,
        page16: Math.max(1, Math.min(581, Math.round((a.page * 581) / 604))),
        juz: a.juz
      });
    });
  });
  console.log('Quran datasets loaded successfully!');
} catch (err) {
  console.error('Error loading Quran data:', err);
}

function normalizeArabic(text) {
  return (text || '')
    .replace(/[\u064B-\u065F\u0670]/g, '')
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي')
    .replace(/ـ/g, '')
    .trim()
    .toLowerCase();
}

const server = http.createServer((req, res) => {
  const urlObj = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = urlObj.pathname;

  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // API endpoints
  if (pathname === '/api/surahs') {
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(surahsMeta));
    return;
  }

  if (pathname === '/api/juz') {
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(juzMeta));
    return;
  }

  if (pathname.startsWith('/api/page/16/')) {
    const pageNum = parseInt(pathname.split('/')[4], 10);
    const page = pages16[pageNum - 1];
    if (page) {
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify(page));
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Page not found' }));
    }
    return;
  }

  if (pathname.startsWith('/api/page/604/')) {
    const pageNum = parseInt(pathname.split('/')[4], 10);
    const page = pages604[pageNum - 1];
    if (page) {
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify(page));
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Page not found' }));
    }
    return;
  }

  if (pathname === '/api/search') {
    const q = urlObj.searchParams.get('q') || '';
    const normQ = normalizeArabic(q);
    if (!normQ) {
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify([]));
      return;
    }
    const results = searchList.filter(item => {
      return normalizeArabic(item.text).includes(normQ) || normalizeArabic(item.surahName).includes(normQ);
    }).slice(0, 100);

    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(results));
    return;
  }

  // Fonts
  if (pathname === '/font/amiri_regular.ttf') {
    const fontPath = path.join(FONTS_DIR, 'amiri_regular.ttf');
    if (fs.existsSync(fontPath)) {
      res.writeHead(200, { 'Content-Type': 'font/ttf' });
      fs.createReadStream(fontPath).pipe(res);
      return;
    }
  }

  if (pathname === '/font/amiri_bold.ttf') {
    const fontPath = path.join(FONTS_DIR, 'amiri_bold.ttf');
    if (fs.existsSync(fontPath)) {
      res.writeHead(200, { 'Content-Type': 'font/ttf' });
      fs.createReadStream(fontPath).pipe(res);
      return;
    }
  }

  // Serve static files or index.html
  let filePath = path.join(__dirname, 'public', pathname === '/' ? 'index.html' : pathname);
  if (!fs.existsSync(filePath)) {
    filePath = path.join(__dirname, 'public', 'index.html');
  }

  const ext = path.extname(filePath).toLowerCase();
  let contentType = 'text/html';
  if (ext === '.css') contentType = 'text/css';
  else if (ext === '.js') contentType = 'application/javascript';
  else if (ext === '.json') contentType = 'application/json';
  else if (ext === '.png') contentType = 'image/png';
  else if (ext === '.svg') contentType = 'image/svg+xml';
  else if (ext === '.ttf') contentType = 'font/ttf';

  res.writeHead(200, { 'Content-Type': `${contentType}; charset=utf-8` });
  fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Holy Quran Emulator & Dev Server running on http://0.0.0.0:${PORT}`);
});

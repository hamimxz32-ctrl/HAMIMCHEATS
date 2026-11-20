import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = path.resolve(__dirname, '..');
const LOCAL_CONFIG_PATH = path.join(ROOT_DIR, 'remote-config', 'app-config.json');
const PORT = Number(process.env.PORT || 4173);

const SESSION_SECRET = process.env.SESSION_SECRET || crypto.randomBytes(32).toString('hex');
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || '';
const ENV_GITHUB_TOKEN = process.env.GITHUB_TOKEN || '';

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.jpg': 'image/jpeg',
  '.png': 'image/png',
};

function parseCookies(req) {
  const header = req.headers.cookie || '';
  const result = {};
  for (const part of header.split(';')) {
    const idx = part.indexOf('=');
    if (idx > 0) {
      const k = part.slice(0, idx).trim();
      const v = part.slice(idx + 1).trim();
      result[k] = decodeURIComponent(v);
    }
  }
  return result;
}

function createSessionToken() {
  const payload = `${Date.now()}.${crypto.randomBytes(16).toString('hex')}`;
  const sig = crypto.createHmac('sha256', SESSION_SECRET).update(payload).digest('hex');
  return `${payload}.${sig}`;
}

function verifySessionToken(token) {
  if (!token) return false;
  const parts = token.split('.');
  if (parts.length !== 3) return false;
  const payload = `${parts[0]}.${parts[1]}`;
  const expected = crypto.createHmac('sha256', SESSION_SECRET).update(payload).digest('hex');
  const a = Buffer.from(parts[2], 'utf8');
  const b = Buffer.from(expected, 'utf8');
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function isAuthenticated(req) {
  if (!ADMIN_PASSWORD) return true;
  const cookies = parseCookies(req);
  return verifySessionToken(cookies.codigos_session);
}

function sendJson(res, status, data, headers = {}) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', ...headers });
  res.end(JSON.stringify(data));
}

async function readJsonBody(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const raw = Buffer.concat(chunks).toString('utf8');
  return raw ? JSON.parse(raw) : {};
}

function validateRemoteConfig(config) {
  if (!config || typeof config !== 'object' || Array.isArray(config)) {
    throw new Error('Invalid config object.');
  }
  return {
    schemaVersion: Number(config.schemaVersion || 1),
    updatedAt: new Date().toISOString(),
    appTitle: String(config.appTitle || 'Codigos Client').slice(0, 30),
    version: {
      displayVersion: String(config.version?.displayVersion || '1.0.0').slice(0, 24),
      latestBuild: Number(config.version?.latestBuild || 1),
      updateBannerEnabled: Boolean(config.version?.updateBannerEnabled),
      updateTitle: String(config.version?.updateTitle || 'Update Ready').slice(0, 60),
      updateMessage: String(config.version?.updateMessage || '').slice(0, 200),
      updateUrl: String(config.version?.updateUrl || '').slice(0, 300),
    },
    maintenance: {
      enabled: Boolean(config.maintenance?.enabled),
      title: String(config.maintenance?.title || 'Maintenance').slice(0, 60),
      message: String(config.maintenance?.message || '').slice(0, 240),
    },
    announcement: {
      enabled: Boolean(config.announcement?.enabled),
      title: String(config.announcement?.title || '').slice(0, 60),
      message: String(config.announcement?.message || '').slice(0, 240),
      severity: ['info', 'warning', 'promo'].includes(config.announcement?.severity)
        ? config.announcement.severity
        : 'info',
    },
    ui: {
      accentColorHex: /^#[0-9A-Fa-f]{6}$/.test(config.ui?.accentColorHex || '')
        ? config.ui.accentColorHex
        : '#FF4D8D',
      sparkleBackdropDefault: Boolean(config.ui?.sparkleBackdropDefault ?? true),
      showQuickStats: Boolean(config.ui?.showQuickStats ?? true),
      showOverlaySection: Boolean(config.ui?.showOverlaySection ?? true),
    },
    games: Array.isArray(config.games)
      ? config.games.map((g) => ({
          id: String(g.id || '').slice(0, 40),
          name: String(g.name || '').slice(0, 40),
          packageName: String(g.packageName || '').slice(0, 80),
          edition: String(g.edition || 'Standard').slice(0, 30),
          resourceSizeMb: Number(g.resourceSizeMb || 10),
          enabled: Boolean(g.enabled ?? true),
        }))
      : [],
  };
}

async function publishToGitHub({ owner, repo, branch, filePath, token, contentJson, message }) {
  const apiUrl = `https://api.github.com/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/contents/${filePath}`;
  const headers = {
    Authorization: `Bearer ${token}`,
    Accept: 'application/vnd.github+json',
    'User-Agent': 'Codigos-Admin-Dashboard',
    'X-GitHub-Api-Version': '2022-11-28',
  };

  let existingSha = undefined;
  const getResp = await fetch(`${apiUrl}?ref=${encodeURIComponent(branch)}`, { headers });
  if (getResp.ok) {
    const existing = await getResp.json();
    existingSha = existing.sha;
  } else if (getResp.status !== 404) {
    const errText = await getResp.text();
    throw new Error(`GitHub lookup failed (${getResp.status}): ${errText}`);
  }

  const encodedContent = Buffer.from(contentJson, 'utf8').toString('base64');
  const putResp = await fetch(apiUrl, {
    method: 'PUT',
    headers: { ...headers, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message: message || 'Update remote-config/app-config.json via Codigos Admin',
      content: encodedContent,
      branch,
      ...(existingSha ? { sha: existingSha } : {}),
    }),
  });

  if (!putResp.ok) {
    const errText = await putResp.text();
    throw new Error(`GitHub publish failed (${putResp.status}): ${errText}`);
  }
  return putResp.json();
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || '/', `http://localhost:${PORT}`);

  if (req.method === 'GET' && url.pathname === '/api/session') {
    return sendJson(res, 200, {
      authenticated: isAuthenticated(req),
      passwordRequired: Boolean(ADMIN_PASSWORD),
      serverTokenConfigured: Boolean(ENV_GITHUB_TOKEN),
      defaults: {
        owner: process.env.GITHUB_OWNER || '',
        repo: process.env.GITHUB_REPO || '',
        branch: process.env.GITHUB_BRANCH || 'main',
        filePath: process.env.GITHUB_CONFIG_PATH || 'remote-config/app-config.json',
      },
    });
  }

  if (req.method === 'POST' && url.pathname === '/api/login') {
    try {
      const { password } = await readJsonBody(req);
      if (!ADMIN_PASSWORD) {
        return sendJson(res, 200, { ok: true });
      }
      const a = Buffer.from(String(password || ''), 'utf8');
      const b = Buffer.from(ADMIN_PASSWORD, 'utf8');
      if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
        return sendJson(res, 401, { error: 'Invalid admin password.' });
      }
      const token = createSessionToken();
      return sendJson(
        res,
        200,
        { ok: true },
        { 'Set-Cookie': `codigos_session=${encodeURIComponent(token)}; HttpOnly; SameSite=Strict; Path=/` }
      );
    } catch (err) {
      return sendJson(res, 400, { error: err.message });
    }
  }

  if (req.method === 'GET' && url.pathname === '/api/config') {
    if (!isAuthenticated(req)) return sendJson(res, 401, { error: 'Authentication required.' });
    try {
      const raw = await fs.readFile(LOCAL_CONFIG_PATH, 'utf8');
      return sendJson(res, 200, JSON.parse(raw));
    } catch (err) {
      return sendJson(res, 500, { error: err.message });
    }
  }

  if (req.method === 'POST' && url.pathname === '/api/publish') {
    if (!isAuthenticated(req)) return sendJson(res, 401, { error: 'Authentication required.' });
    try {
      const body = await readJsonBody(req);
      const cleanConfig = validateRemoteConfig(body.config);
      const formattedJson = JSON.stringify(cleanConfig, null, 2) + '\n';
      await fs.writeFile(LOCAL_CONFIG_PATH, formattedJson, 'utf8');

      const owner = body.github?.owner || process.env.GITHUB_OWNER;
      const repo = body.github?.repo || process.env.GITHUB_REPO;
      const branch = body.github?.branch || process.env.GITHUB_BRANCH || 'main';
      const filePath = body.github?.filePath || process.env.GITHUB_CONFIG_PATH || 'remote-config/app-config.json';
      const token = ENV_GITHUB_TOKEN || body.github?.token;

      let githubResult = null;
      if (owner && repo && token) {
        githubResult = await publishToGitHub({
          owner,
          repo,
          branch,
          filePath,
          token,
          contentJson: formattedJson,
          message: body.commitMessage,
        });
      }

      return sendJson(res, 200, {
        ok: true,
        config: cleanConfig,
        publishedToGitHub: Boolean(githubResult),
        commitSha: githubResult?.commit?.sha || null,
        rawUrl:
          owner && repo
            ? `https://raw.githubusercontent.com/${owner}/${repo}/${branch}/${filePath}`
            : null,
      });
    } catch (err) {
      return sendJson(res, 400, { error: err.message });
    }
  }

  const safePath = url.pathname === '/' ? '/index.html' : url.pathname;
  const resolved = path.resolve(__dirname, '.' + safePath);
  if (!resolved.startsWith(__dirname)) {
    res.writeHead(403);
    return res.end('Forbidden');
  }
  try {
    const fileData = await fs.readFile(resolved);
    const ext = path.extname(resolved);
    res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'application/octet-stream' });
    res.end(fileData);
  } catch {
    res.writeHead(404);
    res.end('Not Found');
  }
});

server.listen(PORT, () => {
  console.log(`Codigos Admin Dashboard listening on http://localhost:${PORT}`);
});

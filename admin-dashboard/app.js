const els = {
  authCard: document.getElementById('authCard'),
  adminPassword: document.getElementById('adminPassword'),
  loginBtn: document.getElementById('loginBtn'),
  statusBanner: document.getElementById('statusBanner'),
  reloadBtn: document.getElementById('reloadBtn'),
  publishBtn: document.getElementById('publishBtn'),
  ghOwner: document.getElementById('ghOwner'),
  ghRepo: document.getElementById('ghRepo'),
  ghBranch: document.getElementById('ghBranch'),
  ghPath: document.getElementById('ghPath'),
  ghToken: document.getElementById('ghToken'),
  tokenContainer: document.getElementById('tokenContainer'),
  rawUrlPreview: document.getElementById('rawUrlPreview'),
  annEnabled: document.getElementById('annEnabled'),
  annTitle: document.getElementById('annTitle'),
  annSeverity: document.getElementById('annSeverity'),
  annMessage: document.getElementById('annMessage'),
  maintEnabled: document.getElementById('maintEnabled'),
  maintTitle: document.getElementById('maintTitle'),
  maintMessage: document.getElementById('maintMessage'),
  verBannerEnabled: document.getElementById('verBannerEnabled'),
  verDisplay: document.getElementById('verDisplay'),
  verBuild: document.getElementById('verBuild'),
  verTitle: document.getElementById('verTitle'),
  verUrl: document.getElementById('verUrl'),
  verMessage: document.getElementById('verMessage'),
  uiAppTitle: document.getElementById('uiAppTitle'),
  uiAccentPicker: document.getElementById('uiAccentPicker'),
  uiAccentHex: document.getElementById('uiAccentHex'),
  uiSparkles: document.getElementById('uiSparkles'),
  uiQuickStats: document.getElementById('uiQuickStats'),
  uiOverlaySection: document.getElementById('uiOverlaySection'),
  gamesList: document.getElementById('gamesList'),
  jsonPreview: document.getElementById('jsonPreview'),
};

let currentGames = [];

function showStatus(message, isError = false) {
  els.statusBanner.textContent = message;
  els.statusBanner.className = `status-banner ${isError ? 'status-err' : 'status-ok'}`;
}

function updateRawUrlPreview() {
  const owner = els.ghOwner.value.trim();
  const repo = els.ghRepo.value.trim();
  const branch = els.ghBranch.value.trim() || 'main';
  const filePath = els.ghPath.value.trim() || 'remote-config/app-config.json';
  els.rawUrlPreview.textContent =
    owner && repo
      ? `Android Raw Config URL: https://raw.githubusercontent.com/${owner}/${repo}/${branch}/${filePath}`
      : 'Enter GitHub Owner and Repository to view the Android Raw Config URL.';
}

function collectConfig() {
  const hex = /^#[0-9A-Fa-f]{6}$/.test(els.uiAccentHex.value.trim())
    ? els.uiAccentHex.value.trim()
    : '#FF4D8D';
  return {
    schemaVersion: 1,
    updatedAt: new Date().toISOString(),
    appTitle: els.uiAppTitle.value.trim() || 'Codigos Client',
    version: {
      displayVersion: els.verDisplay.value.trim() || '1.0.0',
      latestBuild: Number(els.verBuild.value || 1),
      updateBannerEnabled: els.verBannerEnabled.checked,
      updateTitle: els.verTitle.value.trim() || 'Update Ready',
      updateMessage: els.verMessage.value.trim(),
      updateUrl: els.verUrl.value.trim(),
    },
    maintenance: {
      enabled: els.maintEnabled.checked,
      title: els.maintTitle.value.trim() || 'Maintenance',
      message: els.maintMessage.value.trim(),
    },
    announcement: {
      enabled: els.annEnabled.checked,
      title: els.annTitle.value.trim(),
      message: els.annMessage.value.trim(),
      severity: els.annSeverity.value,
    },
    ui: {
      accentColorHex: hex,
      sparkleBackdropDefault: els.uiSparkles.checked,
      showQuickStats: els.uiQuickStats.checked,
      showOverlaySection: els.uiOverlaySection.checked,
    },
    games: currentGames,
  };
}

function renderJsonPreview() {
  els.jsonPreview.textContent = JSON.stringify(collectConfig(), null, 2);
  updateRawUrlPreview();
}

function renderGames(games) {
  currentGames = games || [];
  els.gamesList.innerHTML = '';
  currentGames.forEach((game, idx) => {
    const wrap = document.createElement('div');
    wrap.className = 'game-item';
    wrap.innerHTML = `
      <div class="card-header">
        <strong>${game.name}</strong>
        <label class="switch">
          <input type="checkbox" data-idx="${idx}" data-field="enabled" ${game.enabled ? 'checked' : ''} />
          <span>Visible</span>
        </label>
      </div>
      <div class="grid-two">
        <label class="field">
          <span>Edition</span>
          <input type="text" data-idx="${idx}" data-field="edition" value="${game.edition || ''}" />
        </label>
        <label class="field">
          <span>Bundle Size (MB)</span>
          <input type="number" step="0.1" data-idx="${idx}" data-field="resourceSizeMb" value="${game.resourceSizeMb || 10}" />
        </label>
      </div>
    `;
    els.gamesList.appendChild(wrap);
  });

  els.gamesList.querySelectorAll('input').forEach((input) => {
    input.addEventListener('input', (e) => {
      const idx = Number(e.target.dataset.idx);
      const field = e.target.dataset.field;
      if (field === 'enabled') {
        currentGames[idx].enabled = e.target.checked;
      } else if (field === 'resourceSizeMb') {
        currentGames[idx].resourceSizeMb = Number(e.target.value || 10);
      } else {
        currentGames[idx][field] = e.target.value;
      }
      renderJsonPreview();
    });
  });
}

function populateForm(config) {
  els.annEnabled.checked = Boolean(config.announcement?.enabled);
  els.annTitle.value = config.announcement?.title || '';
  els.annSeverity.value = config.announcement?.severity || 'info';
  els.annMessage.value = config.announcement?.message || '';

  els.maintEnabled.checked = Boolean(config.maintenance?.enabled);
  els.maintTitle.value = config.maintenance?.title || 'Maintenance';
  els.maintMessage.value = config.maintenance?.message || '';

  els.verBannerEnabled.checked = Boolean(config.version?.updateBannerEnabled);
  els.verDisplay.value = config.version?.displayVersion || '1.0.0';
  els.verBuild.value = config.version?.latestBuild || 1;
  els.verTitle.value = config.version?.updateTitle || 'Update Ready';
  els.verUrl.value = config.version?.updateUrl || '';
  els.verMessage.value = config.version?.updateMessage || '';

  els.uiAppTitle.value = config.appTitle || 'Codigos Client';
  const accent = config.ui?.accentColorHex || '#FF4D8D';
  els.uiAccentPicker.value = accent;
  els.uiAccentHex.value = accent;
  els.uiSparkles.checked = Boolean(config.ui?.sparkleBackdropDefault ?? true);
  els.uiQuickStats.checked = Boolean(config.ui?.showQuickStats ?? true);
  els.uiOverlaySection.checked = Boolean(config.ui?.showOverlaySection ?? true);

  renderGames(config.games || []);
  renderJsonPreview();
}

async function loadSessionAndConfig() {
  try {
    const sessionResp = await fetch('/api/session');
    if (sessionResp.ok) {
      const session = await sessionResp.json();
      if (session.defaults.owner && !els.ghOwner.value) els.ghOwner.value = session.defaults.owner;
      if (session.defaults.repo && !els.ghRepo.value) els.ghRepo.value = session.defaults.repo;
      if (session.defaults.branch) els.ghBranch.value = session.defaults.branch;
      if (session.defaults.filePath) els.ghPath.value = session.defaults.filePath;
      if (session.serverTokenConfigured) {
        els.tokenContainer.classList.add('hidden');
      }
      if (!session.authenticated) {
        els.authCard.classList.remove('hidden');
        return;
      }
      els.authCard.classList.add('hidden');
      const cfgResp = await fetch('/api/config');
      if (cfgResp.ok) {
        populateForm(await cfgResp.json());
        return;
      }
    }
  } catch {
    // Static fallback when opened without Node server
  }
  const fallback = await fetch('../remote-config/app-config.json');
  populateForm(await fallback.json());
}

els.loginBtn.addEventListener('click', async () => {
  const resp = await fetch('/api/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password: els.adminPassword.value }),
  });
  if (resp.ok) {
    showStatus('Signed in.');
    await loadSessionAndConfig();
  } else {
    showStatus('Invalid admin password.', true);
  }
});

els.publishBtn.addEventListener('click', async () => {
  const config = collectConfig();
  const github = {
    owner: els.ghOwner.value.trim(),
    repo: els.ghRepo.value.trim(),
    branch: els.ghBranch.value.trim() || 'main',
    filePath: els.ghPath.value.trim() || 'remote-config/app-config.json',
    token: els.ghToken.value.trim(),
  };

  try {
    const resp = await fetch('/api/publish', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ config, github }),
    });
    const data = await resp.json();
    if (!resp.ok) throw new Error(data.error || 'Publish failed');
    showStatus(
      data.publishedToGitHub
        ? `Published to GitHub (commit ${data.commitSha?.slice(0, 7) || 'ok'}).`
        : 'Saved local remote-config/app-config.json. Add GitHub repo and token to push directly.'
    );
  } catch (err) {
    showStatus(err.message, true);
  }
});

els.reloadBtn.addEventListener('click', loadSessionAndConfig);
els.uiAccentPicker.addEventListener('input', (e) => {
  els.uiAccentHex.value = e.target.value.toUpperCase();
  renderJsonPreview();
});
els.uiAccentHex.addEventListener('input', (e) => {
  if (/^#[0-9A-Fa-f]{6}$/.test(e.target.value)) {
    els.uiAccentPicker.value = e.target.value;
  }
  renderJsonPreview();
});

document.querySelectorAll('input, select, textarea').forEach((el) => {
  el.addEventListener('input', renderJsonPreview);
});

loadSessionAndConfig();

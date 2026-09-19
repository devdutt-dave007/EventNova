// =========================================================
// EventNova 3.0 — Frontend Controller & State Management
// Connected to Node.js Backend & MySQL Database (eventnova3)
// Enforces: Institute-Only Event Hosting & Full Auth Flow
// =========================================================

const STATE = {
  currentTab: 'events',
  auth: {
    isAuthenticated: false,
    accountType: 'GUEST', // 'USER' | 'INSTITUTE' | 'GUEST'
    user: null
  },
  users: [],
  institutes: [],
  events: [],
  registrations: [],
  metrics: null,
  filters: {
    search: '',
    category: 'ALL',
    eligibility: 'ALL',
    price: 'ALL'
  }
};

// --- INITIALIZATION ---
document.addEventListener('DOMContentLoaded', async () => {
  setupNavigation();
  setupFilters();
  setupModals();
  restoreSession();
  
  await checkSystemStatus();
  await loadInstitutes();
  await loadUsers();
  await loadOverviewMetrics();
  await loadEvents();
  await loadLedger();
  await loadInspector();

  // If not logged in, prompt auth modal after slight delay
  if (!STATE.auth.isAuthenticated) {
    setTimeout(() => {
      openAuthModal('USER_LOGIN');
    }, 400);
  }

  // Polling for live sync
  setInterval(() => {
    loadOverviewMetrics();
    if (STATE.currentTab === 'events') loadEvents(false);
  }, 10000);
});

// --- AUTHENTICATION & SESSION MANAGEMENT ---
function restoreSession() {
  try {
    const saved = localStorage.getItem('eventnova_session');
    if (saved) {
      const session = JSON.parse(saved);
      STATE.auth = session;
    }
  } catch (e) {
    console.warn('Could not restore session', e);
  }
  updateAuthUI();
}

function saveSession(accountType, userData) {
  STATE.auth = {
    isAuthenticated: true,
    accountType: accountType,
    user: userData
  };
  localStorage.setItem('eventnova_session', JSON.stringify(STATE.auth));
  updateAuthUI();
  if (accountType === 'USER') {
    loadMyRegistrations();
  }
}

function logout() {
  localStorage.removeItem('eventnova_session');
  STATE.auth = {
    isAuthenticated: false,
    accountType: 'GUEST',
    user: null
  };
  updateAuthUI();
  showToast('You have been logged out.', 'success');
  openAuthModal('USER_LOGIN');
}

function continueAsGuest() {
  closeModal('authModal');
  STATE.auth = {
    isAuthenticated: false,
    accountType: 'GUEST',
    user: null
  };
  updateAuthUI();
  showToast('Browsing as Guest. Sign in to register for events or manage wallets.', 'success');
}

function updateAuthUI() {
  const profileName = document.getElementById('profileName');
  const profileRole = document.getElementById('profileRoleBadge');
  const profileSub = document.getElementById('profileSub');
  const profileAvatar = document.getElementById('profileAvatar');
  const authActionBtn = document.getElementById('authActionBtn');
  const sessionBalance = document.getElementById('sessionBalance');
  const walletPillLabel = document.getElementById('walletPillLabel');
  const currentPersonaName = document.getElementById('currentPersonaName');
  const roleNoticeBanner = document.getElementById('userRoleNoticeBanner');

  if (STATE.auth.isAuthenticated && STATE.auth.user) {
    const u = STATE.auth.user;
    profileName.innerText = u.name;
    profileRole.innerText = u.role;
    profileRole.className = `role-badge ${u.role.toLowerCase()}`;
    profileAvatar.innerText = u.name.charAt(0).toUpperCase();
    authActionBtn.innerText = 'Log Out';
    authActionBtn.onclick = logout;

    if (STATE.auth.accountType === 'INSTITUTE') {
      profileSub.innerText = 'Accredited Host Admin';
      walletPillLabel.innerText = 'ESCROW:';
      sessionBalance.innerText = `₹${parseFloat(u.balance || 0).toFixed(2)}`;
      if (roleNoticeBanner) {
        roleNoticeBanner.innerHTML = `
          <div class="notice-icon">🏛</div>
          <div class="notice-text">
            <strong>Institute Admin Active:</strong> You are authorized to <strong>create campus events</strong>, manage participant capacities, and initiate refunds.
          </div>
        `;
      }
    } else {
      // Normal User (Student, Faculty, External)
      profileSub.innerText = u.instituteName ? `${u.instituteName}` : 'External Participant';
      walletPillLabel.innerText = 'WALLET:';
      sessionBalance.innerText = `₹${parseFloat(u.balance || 0).toFixed(2)}`;
      if (currentPersonaName) currentPersonaName.innerText = `${u.name} (${u.role})`;
      if (roleNoticeBanner) {
        roleNoticeBanner.innerHTML = `
          <div class="notice-icon">ℹ</div>
          <div class="notice-text">
            <strong>Participant Mode:</strong> Logged in as <strong>${u.name} (${u.role})</strong>. You can register for campus events. <em>Note: Only accredited educational institutes have permission to host events.</em>
          </div>
        `;
      }
    }
  } else {
    // Guest
    profileName.innerText = 'Guest User';
    profileRole.innerText = 'GUEST';
    profileRole.className = 'role-badge';
    profileAvatar.innerText = 'G';
    profileSub.innerText = 'Click Sign In';
    authActionBtn.innerText = 'Sign In';
    authActionBtn.onclick = () => openAuthModal('USER_LOGIN');
    sessionBalance.innerText = '₹0.00';
    if (currentPersonaName) currentPersonaName.innerText = 'Guest (Sign In to View)';
    if (roleNoticeBanner) {
      roleNoticeBanner.innerHTML = `
        <div class="notice-icon">✦</div>
        <div class="notice-text">
          <strong>Public Guest Mode:</strong> Discovering all inter-college events. Sign in to register or manage your wallet.
        </div>
      `;
    }
  }
}

function handleAuthAction() {
  if (STATE.auth.isAuthenticated) {
    logout();
  } else {
    openAuthModal('USER_LOGIN');
  }
}

// Open Auth Modal with mode
function openAuthModal(mode = 'USER_LOGIN') {
  setAuthMode(mode);
  openModal('authModal');
}

function setAuthMode(mode) {
  const uToggle = document.getElementById('userLoginToggleBtn');
  const iToggle = document.getElementById('instLoginToggleBtn');
  const rToggle = document.getElementById('registerToggleBtn');

  const uPane = document.getElementById('userLoginPane');
  const iPane = document.getElementById('instLoginPane');
  const rPane = document.getElementById('registerPane');

  uToggle.classList.toggle('active', mode === 'USER_LOGIN');
  iToggle.classList.toggle('active', mode === 'INST_LOGIN');
  rToggle.classList.toggle('active', mode === 'REGISTER');

  uPane.style.display = mode === 'USER_LOGIN' ? 'block' : 'none';
  iPane.style.display = mode === 'INST_LOGIN' ? 'block' : 'none';
  rPane.style.display = mode === 'REGISTER' ? 'block' : 'none';
}

// Demo quick-fill helpers
function quickFillUser(email, pass) {
  document.getElementById('userEmailInput').value = email;
  document.getElementById('userPassInput').value = pass;
}

function quickFillInst(email, pass) {
  document.getElementById('instEmailInput').value = email;
  document.getElementById('instPassInput').value = pass;
}

// User Sign In Submit
async function handleUserLogin(e) {
  e.preventDefault();
  const btn = document.getElementById('userSignInBtn');
  btn.disabled = true;
  btn.innerText = 'Authenticating...';

  const email = document.getElementById('userEmailInput').value;
  const password = document.getElementById('userPassInput').value;

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, accountType: 'USER' })
    });
    const result = await res.json();

    if (result.success) {
      saveSession('USER', result.user);
      closeModal('authModal');
      showToast(`Welcome back, ${result.user.name}!`, 'success');
      await loadEvents(false);
      await loadMyRegistrations();
    } else {
      showToast(result.error || 'Login failed', 'error');
    }
  } catch (err) {
    showToast(`Network error: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Sign In as User</span> <span class="btn-arrow">→</span>';
  }
}

// Institute Sign In Submit
async function handleInstituteLogin(e) {
  e.preventDefault();
  const btn = document.getElementById('instSignInBtn');
  btn.disabled = true;
  btn.innerText = 'Verifying Institute...';

  const email = document.getElementById('instEmailInput').value;
  const password = document.getElementById('instPassInput').value;

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, accountType: 'INSTITUTE' })
    });
    const result = await res.json();

    if (result.success) {
      saveSession('INSTITUTE', result.user);
      closeModal('authModal');
      showToast(`Institute authenticated: ${result.user.name}`, 'success');
      await loadInstitutes();
      switchTab('institutes');
    } else {
      showToast(result.error || 'Institute login failed', 'error');
    }
  } catch (err) {
    showToast(`Network error: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Sign In to Institute Console</span> <span class="btn-arrow">→</span>';
  }
}

// User Registration Submit
function toggleRegAccountType() {
  const isUser = document.querySelector('input[name="regAccountType"]:checked').value === 'USER';
  document.getElementById('newUserRegForm').style.display = isUser ? 'block' : 'none';
  document.getElementById('newInstRegForm').style.display = isUser ? 'none' : 'block';
}

function handleRoleChange() {
  const role = document.getElementById('regRole').value;
  const instGroup = document.getElementById('regInstituteGroup');
  const studentFields = document.getElementById('regStudentFields');
  const facultyFields = document.getElementById('regFacultyFields');

  if (role === 'EXTERNAL') {
    instGroup.style.display = 'none';
    studentFields.style.display = 'none';
    facultyFields.style.display = 'none';
  } else if (role === 'STUDENT') {
    instGroup.style.display = 'block';
    studentFields.style.display = 'block';
    facultyFields.style.display = 'none';
  } else if (role === 'FACULTY') {
    instGroup.style.display = 'block';
    studentFields.style.display = 'none';
    facultyFields.style.display = 'block';
  }
}

async function handleUserRegisterSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('submitUserRegBtn');
  btn.disabled = true;
  btn.innerText = 'Creating User & Crediting ₹500...';

  const role = document.getElementById('regRole').value;
  const payload = {
    name: document.getElementById('regName').value,
    email: document.getElementById('regEmail').value,
    password: document.getElementById('regPassword').value,
    phone: document.getElementById('regPhone').value,
    role: role,
    instituteId: role === 'EXTERNAL' ? null : document.getElementById('regInstituteSelect').value,
    enrollmentNo: role === 'STUDENT' ? document.getElementById('regEnrollment').value : null,
    employeeId: role === 'FACULTY' ? document.getElementById('regEmployeeId').value : null
  };

  try {
    const res = await fetch('/api/auth/register-user', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();

    if (result.success) {
      showToast(result.message, 'success');
      // Auto login user
      const loginRes = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: payload.email, password: payload.password, accountType: 'USER' })
      });
      const loginResult = await loginRes.json();
      if (loginResult.success) {
        saveSession('USER', loginResult.user);
        closeModal('authModal');
      }
      await loadUsers();
      await loadOverviewMetrics();
    } else {
      showToast(result.error || 'Registration failed', 'error');
    }
  } catch (err) {
    showToast(`Error: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Create Account & Claim ₹500</span> <span class="btn-arrow">→</span>';
  }
}

async function handleInstRegisterSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('submitInstRegBtn');
  btn.disabled = true;
  btn.innerText = 'Registering Institute...';

  const payload = {
    name: document.getElementById('instRegName').value,
    email: document.getElementById('instRegEmail').value,
    password: document.getElementById('instRegPassword').value,
    address: document.getElementById('instRegAddress').value
  };

  try {
    const res = await fetch('/api/auth/register-institute', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();

    if (result.success) {
      showToast('Institute registered successfully! Please log in.', 'success');
      setAuthMode('INST_LOGIN');
      document.getElementById('instEmailInput').value = payload.email;
      await loadInstitutes();
      await loadOverviewMetrics();
    } else {
      showToast(result.error || 'Institute registration failed', 'error');
    }
  } catch (err) {
    showToast(`Error: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Register Institute</span> <span class="btn-arrow">→</span>';
  }
}

// ============================================
// STRICT PERMISSION: NORMAL USERS CANNOT HOST EVENTS
// ============================================
function handleHostEventClick() {
  if (!STATE.auth.isAuthenticated) {
    showToast('Please sign in with an accredited Institute account to host events.', 'error');
    openAuthModal('INST_LOGIN');
    return;
  }

  if (STATE.auth.accountType !== 'INSTITUTE') {
    // Normal User (Student, Faculty, External) is trying to host event
    const restrictionRole = document.getElementById('restrictionCurrentRole');
    if (restrictionRole) {
      restrictionRole.innerText = `${STATE.auth.user.name} (${STATE.auth.user.role})`;
    }
    openModal('roleRestrictionModal');
    return;
  }

  // Caller is an authenticated Institute
  const select = document.getElementById('eventHostInstitute');
  if (select) {
    select.value = STATE.auth.user.id;
    select.disabled = true; // Lock to their own institute
  }
  openModal('createEventModal');
}

// --- NAVIGATION ---
function setupNavigation() {
  const navButtons = document.querySelectorAll('.nav-btn');
  navButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.getAttribute('data-tab');
      switchTab(tab);
    });
  });

  const ledgerTabs = document.querySelectorAll('.ledger-tab-btn');
  ledgerTabs.forEach(btn => {
    btn.addEventListener('click', () => {
      ledgerTabs.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const subtab = btn.getAttribute('data-subtab');
      if (subtab === 'wallet-ledger') {
        document.getElementById('walletLedgerPane').style.display = 'block';
        document.getElementById('eventTxnPane').style.display = 'none';
      } else {
        document.getElementById('walletLedgerPane').style.display = 'none';
        document.getElementById('eventTxnPane').style.display = 'block';
      }
    });
  });

  document.getElementById('refreshLedgerBtn')?.addEventListener('click', loadLedger);
}

function switchTab(tabId) {
  STATE.currentTab = tabId;

  document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
  });

  document.querySelectorAll('.tab-pane').forEach(pane => {
    pane.classList.remove('active');
  });

  const targetPane = document.getElementById(`tab-${tabId}`);
  if (targetPane) {
    targetPane.classList.add('active');
  }

  if (tabId === 'registrations') {
    if (!STATE.auth.isAuthenticated || STATE.auth.accountType !== 'USER') {
      showToast('Sign in as a user to view personal registrations.', 'error');
      openAuthModal('USER_LOGIN');
    } else {
      loadMyRegistrations();
    }
  }
  if (tabId === 'institutes') loadInstitutes();
  if (tabId === 'ledger') loadLedger();
  if (tabId === 'inspector') loadInspector();
}

// --- SYSTEM STATUS & METRICS ---
async function checkSystemStatus() {
  try {
    const res = await fetch('/api/status');
    const data = await res.json();
    const badgeText = document.getElementById('dbStatusText');
    if (data.connected) {
      badgeText.innerHTML = `MySQL: <strong>${data.database}</strong> • Connected`;
      const meta = document.getElementById('inspectorDbMeta');
      if (meta) meta.innerText = `Database: ${data.database} (${data.mysqlVersion})`;
    } else {
      badgeText.innerText = 'MySQL: Offline';
    }
  } catch (err) {
    document.getElementById('dbStatusText').innerText = 'MySQL: Disconnected';
  }
}

async function loadOverviewMetrics() {
  try {
    const res = await fetch('/api/overview');
    const result = await res.json();
    if (result.success) {
      const d = result.data;
      STATE.metrics = d;
      document.getElementById('kpiTotalEvents').innerText = d.totalEvents;
      document.getElementById('kpiOpenEvents').innerText = `${d.openEvents} Open for Registration`;
      document.getElementById('kpiConfirmedRegs').innerText = d.confirmedRegistrations;
      document.getElementById('kpiWaitlistRegs').innerText = `${d.waitlistedRegistrations} in FIFO Waitlist`;
      document.getElementById('kpiTotalInstitutes').innerText = d.totalInstitutes;
      document.getElementById('kpiWalletPool').innerText = `₹${d.userWalletSum.toLocaleString()}`;
    }
  } catch (e) {
    console.warn('Metrics load failed', e);
  }
}

// --- INSTITUTES & USERS LISTS ---
async function loadInstitutes() {
  try {
    const res = await fetch('/api/institutes');
    const result = await res.json();
    if (result.success) {
      STATE.institutes = result.data;
      renderInstitutes(result.data);

      const hostSelect = document.getElementById('eventHostInstitute');
      const regInstSelect = document.getElementById('regInstituteSelect');
      if (hostSelect) {
        hostSelect.innerHTML = result.data.map(inst => `
          <option value="${inst.Institute_ID}">${inst.Institute_Name}</option>
        `).join('');
      }
      if (regInstSelect) {
        regInstSelect.innerHTML = result.data.map(inst => `
          <option value="${inst.Institute_ID}">${inst.Institute_Name}</option>
        `).join('');
      }
    }
  } catch (e) {
    console.error('Error loading institutes', e);
  }
}

function renderInstitutes(institutes) {
  const container = document.getElementById('institutesGrid');
  if (!container) return;

  container.innerHTML = institutes.map(inst => {
    const isCurrentInstitute = STATE.auth.accountType === 'INSTITUTE' && STATE.auth.user?.id === inst.Institute_ID;

    return `
      <div class="inst-card">
        <div>
          <div class="inst-header">
            <div class="inst-icon">🏛</div>
            <div>
              <h3 class="inst-name">${escapeHtml(inst.Institute_Name)}</h3>
              <span class="inst-email">${escapeHtml(inst.Email)}</span>
            </div>
          </div>
          <p style="color: var(--text-muted); font-size: 12px; margin-bottom: 12px;">📍 ${escapeHtml(inst.Address)}</p>
          <div class="inst-stats">
            <div class="meta-item">
              <span class="meta-item-label">HOSTED EVENTS</span>
              <span class="meta-item-val">${inst.EventCount} Published</span>
            </div>
            <div class="meta-item">
              <span class="meta-item-label">ESCROW BALANCE</span>
              <span class="meta-item-val" style="color: #ffffff;">₹${parseFloat(inst.Balance).toFixed(2)}</span>
            </div>
          </div>
        </div>
        <div class="event-card-footer">
          <span style="font-size: 11px; font-family: var(--font-mono); color: var(--text-dim);">Joined: ${inst.Joining_Date.split('T')[0]}</span>
          ${isCurrentInstitute ? `
            <button class="btn btn-primary btn-xs" onclick="handleHostEventClick()">+ Host as ${inst.Institute_Name.split(' ')[0]}</button>
          ` : `
            <button class="btn btn-outline btn-xs" onclick="handleHostEventClick()">Host Event</button>
          `}
        </div>
      </div>
    `;
  }).join('');
}

async function loadUsers() {
  try {
    const res = await fetch('/api/users');
    const result = await res.json();
    if (result.success) {
      STATE.users = result.data;
      // If currently logged in as user, update latest balance from DB
      if (STATE.auth.isAuthenticated && STATE.auth.accountType === 'USER') {
        const fresh = result.data.find(u => u.User_ID === STATE.auth.user.id);
        if (fresh) {
          STATE.auth.user.balance = parseFloat(fresh.Balance);
          saveSession('USER', STATE.auth.user);
        }
      }
    }
  } catch (e) {
    console.error('Error loading users', e);
  }
}

// --- EVENTS DISCOVERY ---
function setupFilters() {
  const searchInput = document.getElementById('eventSearchInput');
  const searchClear = document.getElementById('searchClearBtn');
  const catFilter = document.getElementById('categoryFilter');
  const eligFilter = document.getElementById('eligibilityFilter');
  const priceFilter = document.getElementById('priceFilter');
  const resetBtn = document.getElementById('resetFiltersBtn');

  searchInput.addEventListener('input', (e) => {
    STATE.filters.search = e.target.value.trim();
    searchClear.style.display = STATE.filters.search ? 'block' : 'none';
    debounce(loadEvents, 300)();
  });

  searchClear.addEventListener('click', () => {
    searchInput.value = '';
    STATE.filters.search = '';
    searchClear.style.display = 'none';
    loadEvents();
  });

  catFilter.addEventListener('change', (e) => {
    STATE.filters.category = e.target.value;
    loadEvents();
  });

  eligFilter.addEventListener('change', (e) => {
    STATE.filters.eligibility = e.target.value;
    loadEvents();
  });

  priceFilter.addEventListener('change', (e) => {
    STATE.filters.price = e.target.value;
    loadEvents();
  });

  resetBtn.addEventListener('click', () => {
    searchInput.value = '';
    searchClear.style.display = 'none';
    catFilter.value = 'ALL';
    eligFilter.value = 'ALL';
    priceFilter.value = 'ALL';
    STATE.filters = { search: '', category: 'ALL', eligibility: 'ALL', price: 'ALL' };
    loadEvents();
  });
}

let debounceTimeout;
function debounce(func, wait) {
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(debounceTimeout);
      func(...args);
    };
    clearTimeout(debounceTimeout);
    debounceTimeout = setTimeout(later, wait);
  };
}

async function loadEvents(showSpinner = true) {
  const grid = document.getElementById('eventsGrid');
  if (showSpinner) {
    grid.innerHTML = `
      <div class="loading-state">
        <div class="spinner"></div>
        <p>Querying EventNova database...</p>
      </div>
    `;
  }

  try {
    const params = new URLSearchParams();
    if (STATE.filters.search) params.append('search', STATE.filters.search);
    if (STATE.filters.category !== 'ALL') params.append('category', STATE.filters.category);
    if (STATE.filters.eligibility !== 'ALL') params.append('eligibility', STATE.filters.eligibility);

    const res = await fetch(`/api/events?${params.toString()}`);
    const result = await res.json();

    if (result.success) {
      let events = result.data;

      if (STATE.filters.price === 'FREE') {
        events = events.filter(e => parseFloat(e.Ticket_Price) === 0);
      } else if (STATE.filters.price === 'PAID') {
        events = events.filter(e => parseFloat(e.Ticket_Price) > 0);
      }

      STATE.events = events;
      document.getElementById('eventResultCount').innerText = `${events.length} event${events.length === 1 ? '' : 's'} found`;
      renderEvents(events);
    }
  } catch (err) {
    grid.innerHTML = `<p class="error-msg">Error loading events: ${err.message}</p>`;
  }
}

function renderEvents(events) {
  const grid = document.getElementById('eventsGrid');
  if (!grid) return;

  if (events.length === 0) {
    grid.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 60px 20px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-subtle);">
        <p style="font-size: 16px; color: var(--text-muted); margin-bottom: 8px;">No events match the selected criteria.</p>
        <button class="btn btn-outline btn-sm" onclick="document.getElementById('resetFiltersBtn').click()">Clear Filters</button>
      </div>
    `;
    return;
  }

  grid.innerHTML = events.map(e => {
    const isFree = parseFloat(e.Ticket_Price) === 0;
    const capacityPct = Math.min(100, Math.round((e.ConfirmedCount / e.Capacity) * 100));
    const isFull = e.ConfirmedCount >= e.Capacity;
    const isOpen = e.Status === 'OPEN';
    const isHostInstitute = STATE.auth.accountType === 'INSTITUTE' && STATE.auth.user?.id === e.Institute_ID;

    return `
      <div class="event-card">
        <div>
          <div class="event-card-header">
            <span class="category-pill">${e.Category}</span>
            <span class="status-tag ${e.Status.toLowerCase()}">${e.Status}</span>
          </div>

          <h3 class="event-title">${escapeHtml(e.Event_Name)}</h3>
          <div class="event-institute">🏛 ${escapeHtml(e.Institute_Name)}</div>
          <p class="event-desc">${escapeHtml(e.Description)}</p>

          <div class="event-meta-grid">
            <div class="meta-item">
              <span class="meta-item-label">DATE & TIME</span>
              <span class="meta-item-val">📅 ${e.Event_Date.split('T')[0]}</span>
            </div>
            <div class="meta-item">
              <span class="meta-item-label">VENUE</span>
              <span class="meta-item-val" title="${escapeHtml(e.Venue)}">📍 ${escapeHtml(e.Venue)}</span>
            </div>
            <div class="meta-item">
              <span class="meta-item-label">FORMAT</span>
              <span class="meta-item-val">👥 ${e.Participation_Type}</span>
            </div>
            <div class="meta-item">
              <span class="meta-item-label">ELIGIBILITY</span>
              <span class="meta-item-val">🎯 ${formatEligibility(e.Eligibility)}</span>
            </div>
          </div>

          <!-- Capacity Bar -->
          <div class="capacity-container">
            <div class="capacity-info">
              <span>CAPACITY: ${e.ConfirmedCount} / ${e.Capacity} spots</span>
              <span>${isFull ? `Waitlist: ${e.WaitlistCount}` : `${e.Capacity - e.ConfirmedCount} left`}</span>
            </div>
            <div class="capacity-bar">
              <div class="capacity-fill" style="width: ${capacityPct}%;"></div>
            </div>
          </div>
        </div>

        <div class="event-card-footer">
          <div class="price-tag">
            <span class="price-tag-label">TICKET FEE</span>
            <span class="price-tag-amount">${isFree ? 'FREE' : `₹${parseFloat(e.Ticket_Price).toFixed(2)}`}</span>
          </div>

          <div>
            ${isHostInstitute && isOpen ? `
              <button class="btn btn-danger btn-xs" onclick="cancelEventByInstitute(${e.Event_ID})">
                Cancel Event (100% Refund)
              </button>
            ` : isOpen ? `
              <button class="btn btn-primary btn-sm" onclick="openRegistrationModal(${e.Event_ID})">
                <span>${isFull ? 'Join Waitlist' : 'Register'}</span>
                <span class="btn-arrow">→</span>
              </button>
            ` : `
              <button class="btn btn-ghost btn-sm" disabled style="opacity: 0.5;">
                Event Closed
              </button>
            `}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function formatEligibility(el) {
  switch (el) {
    case 'STUDENT_NATIVE': return 'Native Students';
    case 'FACULTY_NATIVE': return 'Native Faculty';
    case 'EXTERNAL': return 'External Only';
    case 'ALL': return 'Open to All';
    default: return el;
  }
}

// --- EVENT REGISTRATION ---
let activeRegistrationEvent = null;

function openRegistrationModal(eventId) {
  if (!STATE.auth.isAuthenticated || STATE.auth.accountType !== 'USER') {
    showToast('Please sign in as a student, faculty, or external participant to register.', 'error');
    openAuthModal('USER_LOGIN');
    return;
  }

  const event = STATE.events.find(e => e.Event_ID === eventId);
  if (!event) return;

  activeRegistrationEvent = event;
  const user = STATE.auth.user;

  document.getElementById('regFormEventId').value = event.Event_ID;
  document.getElementById('regModalEventTitle').innerText = event.Event_Name;
  document.getElementById('regModalInstitute').innerText = event.Institute_Name;
  document.getElementById('regModalDateTime').innerText = `${event.Event_Date.split('T')[0]} @ ${event.Start_Time}`;
  document.getElementById('regModalEligibility').innerText = formatEligibility(event.Eligibility);
  document.getElementById('regModalType').innerText = `${event.Participation_Type} Registration`;

  const isFree = parseFloat(event.Ticket_Price) === 0;
  document.getElementById('regModalPrice').innerText = isFree ? 'FREE' : `₹${parseFloat(event.Ticket_Price).toFixed(2)}`;

  document.getElementById('regFormUserName').value = `${user.name} (${user.role} - Balance: ₹${user.balance.toFixed(2)})`;

  const paymentGroup = document.getElementById('paymentModeGroup');
  paymentGroup.style.display = isFree ? 'none' : 'block';

  const teamSection = document.getElementById('teamMembersSection');
  const teamList = document.getElementById('teamMembersList');
  teamList.innerHTML = '';

  if (event.Participation_Type === 'TEAM') {
    teamSection.style.display = 'block';
    addTeamMemberRow();
  } else {
    teamSection.style.display = 'none';
  }

  openModal('registrationModal');
}

function addTeamMemberRow() {
  const list = document.getElementById('teamMembersList');
  const count = list.children.length + 1;
  const row = document.createElement('div');
  row.className = 'team-member-row';
  row.innerHTML = `
    <input type="text" class="form-input flex-2" placeholder="Teammate ${count} Full Name" required>
    <input type="text" class="form-input flex-1" placeholder="Branch" required>
    <input type="text" class="form-input flex-1" placeholder="Enrollment / ID #" required>
    <button type="button" class="btn btn-ghost btn-xs" onclick="this.parentElement.remove()" style="color: #e06060;">&times;</button>
  `;
  list.appendChild(row);
}

document.getElementById('addTeamMemberBtn')?.addEventListener('click', addTeamMemberRow);

async function handleRegistrationSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('confirmRegBtn');
  btn.disabled = true;
  btn.innerText = 'Calling Stored Procedure...';

  try {
    const eventId = parseInt(document.getElementById('regFormEventId').value);
    const event = activeRegistrationEvent;
    const isTeam = event.Participation_Type === 'TEAM';
    const paymentMode = document.querySelector('input[name="paymentMode"]:checked')?.value || 'WALLET';

    const teamMembers = [];
    if (isTeam) {
      const rows = document.querySelectorAll('#teamMembersList .team-member-row');
      rows.forEach(r => {
        const inputs = r.querySelectorAll('input');
        teamMembers.push({
          name: inputs[0].value.trim(),
          branch: inputs[1].value.trim(),
          enrollmentNo: inputs[2].value.trim()
        });
      });
    }

    const res = await fetch('/api/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: STATE.auth.user.id,
        eventId,
        paymentMode,
        isTeam,
        teamMembers
      })
    });

    const result = await res.json();

    if (result.success) {
      closeModal('registrationModal');
      showToast(result.message || 'Registration successfully completed!', 'success');
      await loadUsers();
      await loadEvents(false);
      await loadOverviewMetrics();
      loadMyRegistrations();
    } else {
      showToast(`Registration Failed: ${result.error}`, 'error');
    }
  } catch (err) {
    showToast(`Network error: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Confirm Registration</span> <span class="btn-arrow">→</span>';
  }
}

// --- MY REGISTRATIONS ---
async function loadMyRegistrations() {
  const container = document.getElementById('myRegistrationsContainer');
  if (!container) return;

  if (!STATE.auth.isAuthenticated || STATE.auth.accountType !== 'USER') {
    container.innerHTML = `
      <div style="text-align: center; padding: 60px 20px; background: var(--bg-surface); border-radius: var(--radius-md); border: 1px solid var(--border-subtle);">
        <p style="font-size: 16px; color: var(--text-muted); margin-bottom: 12px;">Sign in with a participant account to view personal tickets and passes.</p>
        <button class="btn btn-primary btn-sm" onclick="openAuthModal('USER_LOGIN')">Sign In as User</button>
      </div>
    `;
    return;
  }

  container.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Fetching registrations from MySQL...</p></div>`;

  try {
    const res = await fetch(`/api/users/${STATE.auth.user.id}`);
    const result = await res.json();

    if (result.success) {
      const regs = result.registrations;
      STATE.registrations = regs;
      renderRegistrations(regs);
    }
  } catch (err) {
    container.innerHTML = `<p class="error-msg">Error loading registrations: ${err.message}</p>`;
  }
}

function renderRegistrations(regs) {
  const container = document.getElementById('myRegistrationsContainer');
  if (!container) return;

  if (regs.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 60px 20px; background: var(--bg-surface); border-radius: var(--radius-md); border: 1px solid var(--border-subtle);">
        <p style="font-size: 16px; color: var(--text-muted); margin-bottom: 8px;">You have not registered for any events yet.</p>
        <button class="btn btn-primary btn-sm" onclick="switchTab('events')">Discover Live Events</button>
      </div>
    `;
    return;
  }

  container.innerHTML = regs.map(r => {
    const isCancelled = r.Registration_Status === 'CANCELLED';

    return `
      <div class="reg-card">
        <div class="reg-main-info">
          <div class="reg-ticket-badge">
            <span class="ticket-tag">PASS</span>
            <span class="ticket-num">#${r.Registration_ID}</span>
          </div>

          <div class="reg-details">
            <div style="display: flex; align-items: center; gap: 8px;">
              <h4 class="reg-event-title">${escapeHtml(r.Event_Name)}</h4>
              <span class="status-tag ${r.Registration_Status.toLowerCase()}">${r.Registration_Status}</span>
              ${r.Team_ID ? `<span class="category-pill" style="font-size: 9px;">${r.Team_ID}</span>` : ''}
            </div>
            <div class="reg-subline">
              <span>🏛 ${escapeHtml(r.Institute_Name)}</span>
              <span>&bull;</span>
              <span>📅 ${r.Event_Date.split('T')[0]}</span>
              <span>&bull;</span>
              <span>📍 ${escapeHtml(r.Venue)}</span>
              <span>&bull;</span>
              <span>Paid: ₹${r.PaidAmount ? parseFloat(r.PaidAmount).toFixed(2) : '0.00'} (${r.Payment_Mode || 'FREE'})</span>
            </div>
          </div>
        </div>

        <div class="reg-actions">
          <button class="btn btn-outline btn-xs" onclick="viewTicketModal(${r.Registration_ID})">
            📄 Admission Pass
          </button>
          ${!isCancelled ? `
            <button class="btn btn-danger btn-xs" onclick="cancelRegistration(${r.Registration_ID})">
              Cancel (50% Refund)
            </button>
          ` : `
            <span style="font-size: 11px; font-family: var(--font-mono); color: var(--text-dim);">Cancelled & Refunded</span>
          `}
        </div>
      </div>
    `;
  }).join('');
}

async function cancelRegistration(registrationId) {
  if (!confirm('Cancel this registration? As per EventNova rules, a 50% refund will be credited back to your wallet immediately.')) {
    return;
  }

  try {
    const res = await fetch(`/api/registrations/${registrationId}/cancel`, {
      method: 'POST'
    });
    const result = await res.json();

    if (result.success) {
      showToast(`Registration cancelled. Refund of ₹${result.refundAmount || 0} credited to your wallet.`, 'success');
      await loadUsers();
      await loadMyRegistrations();
      await loadEvents(false);
      await loadOverviewMetrics();
    } else {
      showToast(`Cancellation failed: ${result.message || result.error}`, 'error');
    }
  } catch (e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

async function cancelEventByInstitute(eventId) {
  if (!confirm('Cancel this entire event? This will trigger stored procedure sp_cancel_event_by_institute to issue 100% full refunds to all attendees and clear the waitlist.')) {
    return;
  }

  try {
    const res = await fetch(`/api/events/${eventId}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        callerRole: STATE.auth.accountType,
        instituteId: STATE.auth.user.id
      })
    });
    const result = await res.json();

    if (result.success) {
      showToast(`Event cancelled. 100% refunds processed for ${result.refundedCount} registrants.`, 'success');
      await loadEvents();
      await loadInstitutes();
      await loadOverviewMetrics();
    } else {
      showToast(`Cancellation failed: ${result.message || result.error}`, 'error');
    }
  } catch (e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

function viewTicketModal(regId) {
  const reg = STATE.registrations.find(r => r.Registration_ID === regId);
  if (!reg) return;

  const content = document.getElementById('ticketModalContent');
  content.innerHTML = `
    <div style="background: #000000; border: 2px dashed #444; border-radius: 8px; padding: 20px; font-family: var(--font-mono); color: #fff;">
      <div style="text-align: center; border-bottom: 1px solid #333; padding-bottom: 12px; margin-bottom: 16px;">
        <h2 style="font-size: 18px; letter-spacing: 2px;">EVENTNOVA ADMISSION PASS</h2>
        <span style="font-size: 11px; color: #888;">REGISTRATION ID: EN-REG-${reg.Registration_ID}</span>
      </div>
      <div style="display: flex; flex-direction: column; gap: 8px; font-size: 13px;">
        <div><strong>EVENT:</strong> ${escapeHtml(reg.Event_Name)}</div>
        <div><strong>VENUE:</strong> ${escapeHtml(reg.Venue)}</div>
        <div><strong>HOST:</strong> ${escapeHtml(reg.Institute_Name)}</div>
        <div><strong>DATE:</strong> ${reg.Event_Date.split('T')[0]}</div>
        <div><strong>ATTENDEE:</strong> ${escapeHtml(STATE.auth.user.name)}</div>
        <div><strong>STATUS:</strong> <span style="background: #fff; color: #000; padding: 2px 6px; font-weight: bold;">${reg.Registration_Status}</span></div>
        ${reg.Team_ID ? `<div><strong>TEAM ID:</strong> ${reg.Team_ID}</div>` : ''}
        <div><strong>AMOUNT PAID:</strong> ₹${reg.PaidAmount ? parseFloat(reg.PaidAmount).toFixed(2) : '0.00'}</div>
      </div>
      <div style="text-align: center; margin-top: 20px; border-top: 1px solid #333; padding-top: 10px; font-size: 10px; color: #666;">
        PRESENT THIS DIGITAL PASS OR PRINTED COPY AT CAMPUS ENTRY GATE
      </div>
    </div>
  `;
  openModal('ticketModal');
}

// --- WALLET RECHARGE ---
function setupModals() {
  document.getElementById('openRechargeModalBtn')?.addEventListener('click', openRechargeModal);
  document.getElementById('walletPill')?.addEventListener('click', () => {
    if (STATE.auth.accountType === 'USER') {
      openRechargeModal();
    } else {
      showToast('Wallet recharge is available for user accounts.', 'error');
    }
  });
  document.getElementById('heroRechargeBtn')?.addEventListener('click', openRechargeModal);
}

function openRechargeModal() {
  if (!STATE.auth.isAuthenticated || STATE.auth.accountType !== 'USER') {
    showToast('Please sign in as a user to recharge your wallet.', 'error');
    openAuthModal('USER_LOGIN');
    return;
  }

  const user = STATE.auth.user;
  document.getElementById('rechargeTargetUser').value = `${user.name} (Balance: ₹${user.balance.toFixed(2)})`;
  document.getElementById('rechargeAmountInput').value = '';
  openModal('rechargeModal');
}

function setRechargeAmount(amt) {
  document.getElementById('rechargeAmountInput').value = amt;
}

async function handleRechargeSubmit(e) {
  e.preventDefault();
  const amount = parseFloat(document.getElementById('rechargeAmountInput').value);
  if (!amount || amount <= 0) return;

  const btn = document.getElementById('submitRechargeBtn');
  btn.disabled = true;
  btn.innerText = 'Calling sp_recharge_wallet...';

  try {
    const res = await fetch('/api/wallet/recharge', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: STATE.auth.user.id, amount })
    });

    const result = await res.json();
    if (result.success) {
      closeModal('rechargeModal');
      showToast(`Wallet credited with ₹${amount.toFixed(2)} successfully!`, 'success');
      await loadUsers();
      await loadOverviewMetrics();
    } else {
      showToast(`Recharge failed: ${result.error}`, 'error');
    }
  } catch (err) {
    showToast(`Error: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Top-Up Wallet</span> <span class="btn-arrow">→</span>';
  }
}

// --- CREATE EVENT SUBMIT (INSTITUTE ONLY) ---
async function handleCreateEventSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('submitCreateEventBtn');
  btn.disabled = true;
  btn.innerText = 'Publishing Event...';

  try {
    const payload = {
      callerRole: STATE.auth.accountType,
      instituteId: parseInt(document.getElementById('eventHostInstitute').value),
      eventName: document.getElementById('eventFormName').value.trim(),
      category: document.getElementById('eventFormCategory').value,
      eligibility: document.getElementById('eventFormEligibility').value,
      participationType: document.getElementById('eventFormType').value,
      description: document.getElementById('eventFormDesc').value.trim(),
      venue: document.getElementById('eventFormVenue').value.trim(),
      capacity: parseInt(document.getElementById('eventFormCapacity').value),
      ticketPrice: parseFloat(document.getElementById('eventFormPrice').value),
      eventDate: document.getElementById('eventFormDate').value,
      startTime: document.getElementById('eventFormStartTime').value + ':00',
      endTime: document.getElementById('eventFormEndTime').value + ':00'
    };

    const res = await fetch('/api/events', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const result = await res.json();
    if (result.success) {
      closeModal('createEventModal');
      showToast('Campus Event successfully published by Institute!', 'success');
      document.getElementById('createEventForm').reset();
      await loadEvents();
      await loadInstitutes();
      await loadOverviewMetrics();
      switchTab('events');
    } else {
      showToast(`Event creation failed: ${result.error}`, 'error');
    }
  } catch (err) {
    showToast(`Error: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Publish Event to Catalog</span> <span class="btn-arrow">→</span>';
  }
}

// --- LEDGER VIEWER ---
async function loadLedger() {
  try {
    const res1 = await fetch('/api/wallet-transactions');
    const result1 = await res1.json();
    const wBody = document.getElementById('walletLedgerBody');

    if (result1.success && wBody) {
      if (result1.data.length === 0) {
        wBody.innerHTML = '<tr><td colspan="7" class="text-center">No ledger entries found.</td></tr>';
      } else {
        wBody.innerHTML = result1.data.map(t => `
          <tr>
            <td style="font-family: var(--font-mono); font-weight: bold;">#${t.Wallet_Txn_ID}</td>
            <td style="font-family: var(--font-mono); font-size: 11px;">${t.Entry_Date.replace('T', ' ').substring(0, 19)}</td>
            <td><strong>${escapeHtml(t.OwnerName || t.Owner_Type + ' ' + t.Owner_ID)}</strong></td>
            <td><span class="category-pill" style="font-size: 9px;">${t.Entry_Type}</span></td>
            <td style="font-family: var(--font-mono); font-weight: bold; color: ${t.Entry_Type.includes('CREDIT') || t.Entry_Type === 'BONUS' || t.Entry_Type === 'RECHARGE' ? '#ffffff' : '#9999a4'};">
              ${t.Entry_Type.includes('CREDIT') || t.Entry_Type === 'BONUS' || t.Entry_Type === 'RECHARGE' ? '+' : '-'}₹${parseFloat(t.Amount).toFixed(2)}
            </td>
            <td style="font-family: var(--font-mono);">₹${parseFloat(t.Balance_After).toFixed(2)}</td>
            <td style="color: var(--text-dim); font-size: 11px;">${escapeHtml(t.Description)}</td>
          </tr>
        `).join('');
      }
    }

    const res2 = await fetch('/api/transactions');
    const result2 = await res2.json();
    const eBody = document.getElementById('eventTxnBody');

    if (result2.success && eBody) {
      if (result2.data.length === 0) {
        eBody.innerHTML = '<tr><td colspan="8" class="text-center">No invoice records found.</td></tr>';
      } else {
        eBody.innerHTML = result2.data.map(t => `
          <tr>
            <td style="font-family: var(--font-mono); font-weight: bold;">INV-${t.Transaction_ID}</td>
            <td style="font-family: var(--font-mono); font-size: 11px;">${t.Transaction_Date.replace('T', ' ').substring(0, 19)}</td>
            <td>${escapeHtml(t.UserName)}</td>
            <td><strong>${escapeHtml(t.Event_Name)}</strong></td>
            <td>${escapeHtml(t.Institute_Name)}</td>
            <td><span class="category-pill" style="font-size: 9px;">${t.Payment_Mode}</span></td>
            <td style="font-family: var(--font-mono); font-weight: bold;">₹${parseFloat(t.Amount).toFixed(2)}</td>
            <td><span class="status-tag ${t.Transaction_Status === 'SUCCESS' ? 'open' : t.Transaction_Status === 'REFUNDED' ? 'cancelled' : 'closed'}">${t.Transaction_Status}</span></td>
          </tr>
        `).join('');
      }
    }
  } catch (err) {
    console.error('Error loading ledger', err);
  }
}

// --- DB INSPECTOR ---
async function loadInspector() {
  const container = document.getElementById('inspectorTablesGrid');
  if (!container) return;

  try {
    const res = await fetch('/api/schema-summary');
    const result = await res.json();

    if (result.success) {
      container.innerHTML = result.tables.map(t => `
        <div class="table-schema-card">
          <div class="table-schema-header">
            <span class="table-name">TABLE: ${t.table}</span>
            <span class="table-badge">${t.rows} Rows &bull; ${t.columnCount} Cols</span>
          </div>
          <div class="columns-preview">
            ${t.columns.slice(0, 6).map(c => `
              <div class="col-item">
                <span class="col-name">${c.field} ${c.key === 'PRI' ? '🔑' : ''}</span>
                <span>${c.type}</span>
              </div>
            `).join('')}
            ${t.columns.length > 6 ? `<div style="text-align: center; color: var(--text-dim); padding-top: 4px;">+ ${t.columns.length - 6} more columns</div>` : ''}
          </div>
        </div>
      `).join('');
    }
  } catch (err) {
    container.innerHTML = `<p class="error-msg">Error loading database schema: ${err.message}</p>`;
  }
}

// --- MODAL UTILITIES ---
function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('active');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('active');
}

document.querySelectorAll('.modal-backdrop').forEach(modal => {
  modal.addEventListener('click', (e) => {
    if (e.target === modal) modal.classList.remove('active');
  });
});

function showToast(msg, type = 'success') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${type === 'success' ? '✓' : '⚠'}</span> <span>${escapeHtml(msg)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4500);
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

/**
 * CloudStore — Application Controller
 */

// State variables
let currentView = 'dashboard';
let allUnits = [];
let allBookings = [];
let pendingBookingUnit = null;
let pendingCancelBookingId = null;

// ================= INITIALIZATION & AUTH STATE =================

document.addEventListener('DOMContentLoaded', () => {
    initApp();
    setupDropZone();
});

function initApp() {
    updateEnvironmentBadge();

    // Listen for authentication changes
    window.authService.onAuthStateChanged((user) => {
        const authGate = document.getElementById('auth-gate');
        if (user) {
            authGate.classList.remove('active');
            updateHeaderUser(user);
            loadDashboard();
        } else {
            authGate.classList.add('active');
        }
    });
}

function updateEnvironmentBadge() {
    const isMock = window.APP_CONFIG.useMockApi || !window.APP_CONFIG.apiBaseUrl;
    const badge = document.getElementById('env-badge');
    const text = document.getElementById('env-text');
    if (isMock) {
        badge.className = 'mode-badge mock';
        text.textContent = 'Demo Mode';
    } else {
        badge.className = 'mode-badge live';
        text.textContent = 'Live AWS';
    }
}

function updateHeaderUser(user) {
    if (!user) return;
    const email = user.email || 'Customer';
    const initials = email.substring(0, 2).toUpperCase();
    document.getElementById('header-user-email').textContent = email;
    document.getElementById('user-avatar-initials').textContent = initials;
    document.getElementById('welcome-user-heading').textContent = `Welcome back, ${email.split('@')[0]}!`;
}

// ================= NAVIGATION =================

function navigateTo(viewId) {
    currentView = viewId;

    // Update active sidebar link
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.toggle('active', item.dataset.view === viewId);
    });

    // Update active section
    document.querySelectorAll('.view-section').forEach(sec => {
        sec.classList.remove('active');
    });

    const activeSec = document.getElementById(`view-${viewId}`);
    if (activeSec) {
        activeSec.classList.add('active');
    }

    // Update title
    const titles = {
        dashboard: 'Dashboard',
        units: 'Storage Units',
        bookings: 'My Bookings',
        files: 'File Storage'
    };
    document.getElementById('page-title').textContent = titles[viewId] || 'Dashboard';

    // Auto load view data
    if (viewId === 'dashboard') loadDashboard();
    if (viewId === 'units') loadUnits();
    if (viewId === 'bookings') loadBookings();
    if (viewId === 'files') renderUserFiles();

    // Close mobile sidebar if open
    document.getElementById('sidebar').classList.remove('open');
}

function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('open');
}

// ================= AUTHENTICATION ACTIONS =================

function showAuthTab(tab) {
    document.getElementById('tab-login-btn').classList.toggle('active', tab === 'login');
    document.getElementById('tab-register-btn').classList.toggle('active', tab === 'register');
    document.getElementById('tab-verify-btn').classList.toggle('active', tab === 'verify');

    document.getElementById('login-form').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
    document.getElementById('verify-form').style.display = tab === 'verify' ? 'block' : 'none';
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    const btn = document.getElementById('login-submit-btn');

    try {
        btn.disabled = true;
        btn.textContent = 'Signing in...';
        await window.authService.signIn(email, password);
        showToast('Signed in successfully!', 'success');
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Sign In';
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;
    const btn = document.getElementById('reg-submit-btn');

    try {
        btn.disabled = true;
        btn.textContent = 'Creating account...';
        const result = await window.authService.signUp(email, password);
        if (result.userConfirmed) {
            showToast('Account created and verified! You can now sign in.', 'success');
            document.getElementById('login-email').value = email;
            showAuthTab('login');
        } else {
            showToast('Account created! Please enter the code sent to your email.', 'info');
            document.getElementById('verify-email').value = email;
            showAuthTab('verify');
        }
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Create Account';
    }
}

async function handleVerify(e) {
    e.preventDefault();
    const email = document.getElementById('verify-email').value.trim();
    const code = document.getElementById('verify-code').value.trim();
    const btn = document.getElementById('verify-submit-btn');

    try {
        btn.disabled = true;
        btn.textContent = 'Confirming...';
        await window.authService.confirmSignUp(email, code);
        showToast('Account confirmed! You may now sign in.', 'success');
        document.getElementById('login-email').value = email;
        showAuthTab('login');
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Confirm Account';
    }
}

function handleSignOut() {
    window.authService.signOut();
    showToast('Signed out', 'info');
}

// ================= DASHBOARD =================

async function loadDashboard() {
    try {
        const [units, bookings] = await Promise.all([
            window.apiService.getUnits().catch(() => []),
            window.apiService.getBookings().catch(() => [])
        ]);

        const files = window.apiService.getUserFiles();

        allUnits = units;
        allBookings = bookings;

        const availableCount = units.filter(u => u.available).length;
        const activeBookingsCount = bookings.filter(b => b.status === 'ACTIVE').length;

        document.getElementById('stat-total-units').textContent = units.length;
        document.getElementById('stat-available-units').textContent = availableCount;
        document.getElementById('stat-active-bookings').textContent = activeBookingsCount;
        document.getElementById('stat-stored-files').textContent = files.length;

        // Render preview table
        const tbody = document.getElementById('dashboard-bookings-tbody');
        if (bookings.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="empty-state">No bookings found. Reserve an available unit today!</td></tr>`;
            return;
        }

        const recent = bookings.slice(0, 5);
        tbody.innerHTML = recent.map(b => `
            <tr>
                <td><span class="code-badge">${escapeHtml(b.id)}</span></td>
                <td><strong>${escapeHtml(b.unitId)}</strong></td>
                <td>${formatDate(b.bookingDate)}</td>
                <td><span class="badge ${b.status === 'ACTIVE' ? 'badge-active' : 'badge-cancelled'}">${escapeHtml(b.status)}</span></td>
            </tr>
        `).join('');

    } catch (e) {
        console.error('Error loading dashboard:', e);
    }
}

// ================= STORAGE UNITS =================

async function loadUnits() {
    const container = document.getElementById('units-grid-container');
    container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--navy-600);">Loading storage units...</div>';

    try {
        allUnits = await window.apiService.getUnits();
        filterUnits();
    } catch (err) {
        container.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--danger-text);">Error loading units: ${escapeHtml(err.message)}</div>`;
    }
}

function filterUnits() {
    const searchTerm = (document.getElementById('unit-search').value || '').toLowerCase();
    const sizeFilter = document.getElementById('unit-size-filter').value;
    const availOnly = document.getElementById('unit-avail-only').checked;

    const filtered = allUnits.filter(u => {
        const matchesSearch = u.name.toLowerCase().includes(searchTerm) || u.id.toLowerCase().includes(searchTerm);
        const matchesSize = sizeFilter === 'ALL' || (u.size && u.size.toLowerCase() === sizeFilter.toLowerCase());
        const matchesAvail = !availOnly || u.available;
        return matchesSearch && matchesSize && matchesAvail;
    });

    renderUnits(filtered);
}

function renderUnits(units) {
    const container = document.getElementById('units-grid-container');

    if (units.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1/-1;">
                <svg fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/></svg>
                <h3>No storage units match your criteria</h3>
                <p>Try clearing filters or search keywords.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = units.map(unit => {
        const isAvail = unit.available;
        return `
            <div class="unit-card">
                <div>
                    <div class="unit-card-header">
                        <div>
                            <span class="unit-id">${escapeHtml(unit.id)}</span>
                            <h3 class="unit-name">${escapeHtml(unit.name)}</h3>
                        </div>
                        <span class="badge ${isAvail ? 'badge-available' : 'badge-occupied'}">
                            ${isAvail ? 'Available' : 'Occupied'}
                        </span>
                    </div>

                    <div style="margin: 8px 0;">
                        <span class="badge badge-size">${escapeHtml(unit.size || 'Standard')}</span>
                    </div>

                    <ul class="unit-features">
                        <li>
                            <svg fill="none" viewBox="0 0 24 24" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                            <span>24/7 Keycard / Passcode Access</span>
                        </li>
                        <li>
                            <svg fill="none" viewBox="0 0 24 24" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                            <span>Video Surveillance & Fire Monitoring</span>
                        </li>
                        <li>
                            <svg fill="none" viewBox="0 0 24 24" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                            <span>Clean, Dry, Secure Compartment</span>
                        </li>
                    </ul>
                </div>

                <div class="unit-footer">
                    <div class="unit-price">
                        <span class="price-currency">$</span>
                        <span class="price-amount">${Number(unit.price).toFixed(2)}</span>
                        <span class="price-period">/mo</span>
                    </div>
                    <button class="btn btn-primary btn-sm" ${!isAvail ? 'disabled' : ''} onclick="promptBooking('${escapeHtml(unit.id)}', '${escapeHtml(unit.name)}', ${unit.price})">
                        ${isAvail ? 'Book Unit' : 'Unavailable'}
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function promptBooking(unitId, unitName, price) {
    pendingBookingUnit = { unitId, unitName, price };
    document.getElementById('modal-book-unit-name').textContent = unitName;
    document.getElementById('modal-book-unit-id').textContent = unitId;
    document.getElementById('modal-book-unit-price').textContent = `$${Number(price).toFixed(2)} / month`;
    openModal('booking-modal');
}

async function executeBooking() {
    if (!pendingBookingUnit) return;
    const btn = document.getElementById('confirm-booking-btn');

    try {
        btn.disabled = true;
        btn.textContent = 'Processing...';
        await window.apiService.createBooking(pendingBookingUnit.unitId);
        showToast(`Successfully booked ${pendingBookingUnit.unitName}!`, 'success');
        closeModal('booking-modal');
        loadUnits();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Confirm Booking';
        pendingBookingUnit = null;
    }
}

// ================= BOOKINGS =================

async function loadBookings() {
    const tbody = document.getElementById('bookings-table-tbody');
    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; padding: 30px; color: var(--navy-600);">Loading bookings...</td></tr>';

    try {
        allBookings = await window.apiService.getBookings();
        renderBookings(allBookings);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--danger-text); padding: 30px;">Error loading bookings: ${escapeHtml(err.message)}</td></tr>`;
    }
}

function renderBookings(bookings) {
    const tbody = document.getElementById('bookings-table-tbody');

    if (bookings.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="empty-state">
                    <svg fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
                    <h3>No bookings found</h3>
                    <p>Go to Storage Units to reserve your first space.</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = bookings.map(b => {
        const isCancelable = b.status === 'ACTIVE';
        return `
            <tr>
                <td><span class="code-badge">${escapeHtml(b.id)}</span></td>
                <td><strong>${escapeHtml(b.unitId)}</strong></td>
                <td>${formatDate(b.bookingDate)}</td>
                <td>
                    <span class="badge ${b.status === 'ACTIVE' ? 'badge-active' : 'badge-cancelled'}">
                        ${escapeHtml(b.status)}
                    </span>
                </td>
                <td style="text-align: right;">
                    ${isCancelable ? `
                        <button class="btn btn-danger btn-sm" onclick="promptCancelBooking('${escapeHtml(b.id)}')">
                            Cancel Booking
                        </button>
                    ` : `
                        <span style="font-size: 0.8rem; color: var(--navy-600);">Cancelled</span>
                    `}
                </td>
            </tr>
        `;
    }).join('');
}

function promptCancelBooking(bookingId) {
    pendingCancelBookingId = bookingId;
    document.getElementById('modal-cancel-booking-id').textContent = bookingId;
    openModal('cancel-modal');
}

async function executeCancellation() {
    if (!pendingCancelBookingId) return;
    const btn = document.getElementById('confirm-cancel-btn');

    try {
        btn.disabled = true;
        btn.textContent = 'Cancelling...';
        await window.apiService.cancelBooking(pendingCancelBookingId);
        showToast('Booking cancelled successfully', 'success');
        closeModal('cancel-modal');
        loadBookings();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Yes, Cancel Booking';
        pendingCancelBookingId = null;
    }
}

// ================= FILE STORAGE =================

function setupDropZone() {
    const dropZone = document.getElementById('drop-zone');
    if (!dropZone) return;

    ['dragenter', 'dragover'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.remove('dragover');
        });
    });

    dropZone.addEventListener('drop', (e) => {
        const files = e.dataTransfer.files;
        handleFileSelected(files);
    });
}

async function handleFileSelected(files) {
    if (!files || files.length === 0) return;
    const file = files[0];

    // Check size limit (max 50MB)
    if (file.size > 50 * 1024 * 1024) {
        showToast('File too large. Maximum size is 50MB.', 'error');
        return;
    }

    const progressBox = document.getElementById('upload-progress-box');
    const fill = document.getElementById('upload-progress-fill');
    const percentLabel = document.getElementById('upload-progress-percent');
    const filenameLabel = document.getElementById('upload-progress-filename');

    progressBox.style.display = 'block';
    filenameLabel.textContent = `Uploading ${file.name}...`;
    fill.style.width = '0%';
    percentLabel.textContent = '0%';

    try {
        await window.apiService.uploadFile(file, (percent) => {
            fill.style.width = `${percent}%`;
            percentLabel.textContent = `${percent}%`;
        });

        showToast(`Uploaded ${file.name} successfully to S3!`, 'success');
        renderUserFiles();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        setTimeout(() => {
            progressBox.style.display = 'none';
        }, 1500);
        document.getElementById('file-input').value = '';
    }
}

function renderUserFiles() {
    const tbody = document.getElementById('files-table-tbody');
    const files = window.apiService.getUserFiles();

    if (files.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="empty-state">
                    <svg fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/></svg>
                    <h3>No files uploaded yet</h3>
                    <p>Upload lease contracts, IDs, or storage photos above.</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = files.map(f => `
        <tr>
            <td>
                <div style="display: flex; align-items: center; gap: 10px;">
                    <span style="font-size: 1.25rem;">📄</span>
                    <div>
                        <div style="font-weight: 600; color: var(--navy-900);">${escapeHtml(f.fileName)}</div>
                        <div style="font-size: 0.76rem; color: var(--navy-600);">${escapeHtml(f.contentType || 'binary/data')}</div>
                    </div>
                </div>
            </td>
            <td>${formatBytes(f.size || 0)}</td>
            <td>${formatDate(f.uploadedAt)}</td>
            <td style="text-align: right;">
                <button class="btn btn-secondary btn-sm" onclick="downloadFile('${escapeHtml(f.fileName)}')">
                    <svg style="width: 14px; height: 14px; stroke: currentColor; fill: none;" viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
                    Download
                </button>
            </td>
        </tr>
    `).join('');
}

async function downloadFile(fileName) {
    try {
        showToast(`Requesting pre-signed download URL for ${fileName}...`, 'info');
        const res = await window.apiService.getDownloadUrl(fileName);
        if (res.downloadUrl) {
            window.open(res.downloadUrl, '_blank');
        } else {
            throw new Error('Download URL was not generated');
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ================= MODALS & SETTINGS =================

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('active');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
}

function openSettingsModal() {
    document.getElementById('cfg-mock-mode').value = window.APP_CONFIG.useMockApi ? 'true' : 'false';
    document.getElementById('cfg-api-url').value = window.APP_CONFIG.apiBaseUrl;
    document.getElementById('cfg-region').value = window.APP_CONFIG.cognito.region;
    document.getElementById('cfg-user-pool-id').value = window.APP_CONFIG.cognito.userPoolId;
    document.getElementById('cfg-client-id').value = window.APP_CONFIG.cognito.clientId;
    toggleConfigFields(window.APP_CONFIG.useMockApi ? 'true' : 'false');
    openModal('settings-modal');
}

function toggleConfigFields(val) {
    const fields = document.getElementById('cfg-aws-fields');
    fields.style.display = val === 'false' ? 'block' : 'none';
}

function saveSettings() {
    const useMock = document.getElementById('cfg-mock-mode').value === 'true';
    const apiUrl = document.getElementById('cfg-api-url').value.trim();
    const region = document.getElementById('cfg-region').value.trim();
    const userPoolId = document.getElementById('cfg-user-pool-id').value.trim();
    const clientId = document.getElementById('cfg-client-id').value.trim();

    window.updateConfig(apiUrl, region, userPoolId, clientId, useMock);
    updateEnvironmentBadge();
    closeModal('settings-modal');
    showToast('Configuration updated!', 'success');
}

// ================= UTILITIES =================

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '⚠️';

    toast.innerHTML = `<span>${icon}</span> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(20px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function formatDate(isoStr) {
    if (!isoStr) return '-';
    try {
        const d = new Date(isoStr);
        return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
        return isoStr;
    }
}

function formatBytes(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

const state = { hospitals: [], patients: [], noticeTimer: null };
const $ = (selector) => document.querySelector(selector);

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
}

function timeLabel(minutes) {
  if (minutes == null || minutes < 0) return '—';
  const hours = Math.floor(minutes / 60) % 24;
  const mins = minutes % 60;
  return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}`;
}

function showNotice(message, isError = false) {
  const notice = $('#notice');
  notice.textContent = message;
  notice.className = `notice ${isError ? 'error' : 'success'}`;
  notice.hidden = false;
  clearTimeout(state.noticeTimer);
  state.noticeTimer = setTimeout(() => { notice.hidden = true; }, 6000);
  notice.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers }
  });
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.error || data?.message || `Request failed (${response.status})`);
  return data;
}

function renderHospitals() {
  const grid = $('#hospital-grid');
  $('#hospital-count').textContent = `${state.hospitals.length} ${state.hospitals.length === 1 ? 'facility' : 'facilities'}`;
  if (!state.hospitals.length) {
    grid.innerHTML = '<div class="loading-card">No hospitals available.</div>';
    return;
  }
  let totalBeds = 0;
  let availableBeds = 0;
  grid.innerHTML = state.hospitals.map((hospital) => {
    const beds = hospital.beds || [];
    const available = beds.filter((bed) => !bed.occupied).length;
    const rooms = hospital.operatingRooms || [];
    const staff = hospital.staffMembers || [];
    totalBeds += beds.length;
    availableBeds += available;
    const ratio = beds.length ? Math.round((available / beds.length) * 100) : 0;
    return `<article class="hospital-card">
      <div class="hospital-title"><div><h3>${escapeHtml(hospital.name)}</h3><p>${escapeHtml(hospital.location)}</p></div><span class="facility-icon">⌂</span></div>
      <div class="capacity-row"><span>Available bed capacity</span><strong>${available} / ${beds.length}</strong></div>
      <div class="capacity-track"><div class="capacity-fill" style="width:${ratio}%"></div></div>
      <div class="facility-stats"><span><strong>${rooms.length}</strong> operating rooms</span><span><strong>${staff.length}</strong> care staff</span></div>
    </article>`;
  }).join('');
  $('#kpi-beds').textContent = availableBeds;
  $('#kpi-bed-total').textContent = `Available of ${totalBeds} total beds`;
}

function statusOf(patient) {
  if (patient.discharged) return 'discharged';
  return patient.scheduled ? 'scheduled' : 'pending';
}

function priorityClass(priority) {
  const value = String(priority || '').toLowerCase();
  return ['emergency', 'urgent', 'elective'].includes(value) ? value : 'elective';
}

function renderPatients() {
  const query = $('#patient-search').value.trim().toLowerCase();
  const filter = $('#status-filter').value;
  const filtered = state.patients.filter((patient) => {
    const status = statusOf(patient);
    const matchesStatus = filter === 'all' || status === filter;
    const matchesQuery = !query || `${patient.name} ${patient.id} ${patient.surgeryType}`.toLowerCase().includes(query);
    return matchesStatus && matchesQuery;
  });
  const scheduled = state.patients.filter((patient) => statusOf(patient) === 'scheduled').length;
  const pending = state.patients.filter((patient) => statusOf(patient) === 'pending').length;
  $('#kpi-patients').textContent = state.patients.length;
  $('#kpi-scheduled').textContent = scheduled;
  $('#kpi-pending').textContent = pending;
  $('#patient-count').textContent = `Showing ${filtered.length} of ${state.patients.length} patients`;
  const rows = $('#patient-rows');
  if (!filtered.length) {
    rows.innerHTML = '<tr><td colspan="7" class="empty-state">No patients match this view.</td></tr>';
    return;
  }
  rows.innerHTML = filtered.map((patient) => {
    const status = statusOf(patient);
    const initials = String(patient.name || 'P').split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
    const schedule = patient.discharged ? `Discharged ${timeLabel(patient.dischargeTime)}` : patient.scheduled ? `${timeLabel(patient.scheduledStartTime)} · ${patient.surgeryDuration} min` : 'Awaiting assignment';
    const action = status === 'scheduled' && patient.assignedBedId >= 0
      ? `<button class="table-action" data-discharge="${patient.id}" data-name="${escapeHtml(patient.name)}">Discharge</button>`
      : '';
    const priority = String(patient.priority || 'ELECTIVE');
    return `<tr>
      <td><div class="patient-cell"><span class="patient-avatar">${escapeHtml(initials)}</span><span><span class="patient-name">${escapeHtml(patient.name)}</span><span class="patient-id">P${escapeHtml(patient.id)} · age ${escapeHtml(patient.age)}</span></span></div></td>
      <td><span class="procedure-name">${escapeHtml(patient.surgeryType)}</span><span class="sub-cell">${escapeHtml(patient.surgeryDuration)} min procedure${patient.requiresICU ? ' · ICU' : ''}</span></td>
      <td><span class="priority ${priorityClass(priority)}">${escapeHtml(priority[0] + priority.slice(1).toLowerCase())}</span></td>
      <td>${escapeHtml(timeLabel(patient.arrivalTime))}</td>
      <td>${escapeHtml(schedule)}</td>
      <td><span class="status-badge ${status}">${status[0].toUpperCase() + status.slice(1)}</span></td>
      <td>${action}</td>
    </tr>`;
  }).join('');
  rows.querySelectorAll('[data-discharge]').forEach((button) => button.addEventListener('click', () => openDischarge(button.dataset.discharge, button.dataset.name)));
}

async function refreshData(showMessage = false) {
  try {
    const [hospitals, patients] = await Promise.all([api('/api/hospitals'), api('/api/patients')]);
    state.hospitals = hospitals;
    state.patients = patients;
    renderHospitals();
    renderPatients();
    if (showMessage) showNotice('Data refreshed from the scheduling service.');
  } catch (error) {
    showNotice(`Could not load dashboard data: ${error.message}. Check that the Spring Boot server is running.`, true);
    $('#hospital-grid').innerHTML = '<div class="loading-card">The API is unavailable. Start the web app and refresh.</div>';
    $('#patient-rows').innerHTML = '<tr><td colspan="7" class="empty-state">Could not connect to the scheduler API.</td></tr>';
  }
}

function setBusy(button, busy, label) {
  if (busy) {
    button.dataset.originalLabel = button.textContent;
    button.textContent = label;
    button.disabled = true;
  } else {
    button.textContent = button.dataset.originalLabel || label;
    button.disabled = false;
  }
}

async function runSchedule() {
  const button = $('#run-schedule');
  setBusy(button, true, 'Scheduling…');
  try {
    state.patients = await api('/api/schedules/run', { method: 'POST' });
    await refreshData();
    showNotice('Scheduling complete. Patient assignments and bed capacity are updated.');
  } catch (error) {
    showNotice(`Scheduling failed: ${error.message}`, true);
  } finally {
    setBusy(button, false);
  }
}

function localClockTime(value) {
  if (!value) return '';
  const [hours, minutes] = value.split(':').map(Number);
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
}

async function submitEmergency(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const fields = new FormData(form);
  const payload = {
    name: fields.get('name'),
    surgeryType: fields.get('surgeryType'),
    durationMinutes: Number(fields.get('durationMinutes')),
    requiresICU: fields.get('requiresICU') === 'on',
    arrivalTime: localClockTime(fields.get('arrivalTime'))
  };
  setBusy(button, true, 'Adding patient…');
  try {
    const patient = await api('/api/emergencies', { method: 'POST', body: JSON.stringify(payload) });
    form.reset();
    await refreshData();
    showNotice(`Emergency patient ${patient.name} (P${patient.id}) added and the schedule was re-evaluated.`);
    $('#patients').scrollIntoView({ behavior: 'smooth' });
  } catch (error) {
    showNotice(`Emergency intake failed: ${error.message}`, true);
  } finally {
    setBusy(button, false);
  }
}

function openDischarge(id, name) {
  const dialog = $('#discharge-dialog');
  $('#discharge-patient-label').textContent = `Discharge ${name} (P${id}) and release their assigned bed. The time must be after surgery completion.`;
  $('#discharge-form [name="patientId"]').value = id;
  dialog.showModal();
}

async function submitDischarge(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const patientId = form.elements.patientId.value;
  const dischargeTime = localClockTime(form.elements.dischargeTime.value);
  const submit = form.querySelector('[type="submit"]');
  setBusy(submit, true, 'Releasing bed…');
  try {
    const patient = await api(`/api/patients/${patientId}/discharge`, { method: 'POST', body: JSON.stringify({ dischargeTime }) });
    $('#discharge-dialog').close();
    form.reset();
    await refreshData();
    showNotice(`${patient.name} (P${patient.id}) discharged. Their bed is available again.`);
  } catch (error) {
    showNotice(`Discharge failed: ${error.message}`, true);
  } finally {
    setBusy(submit, false);
  }
}

$('#today-label').textContent = new Intl.DateTimeFormat(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }).format(new Date());
$('#run-schedule').addEventListener('click', runSchedule);
$('#refresh-button').addEventListener('click', () => refreshData(true));
$('#patient-search').addEventListener('input', renderPatients);
$('#status-filter').addEventListener('change', renderPatients);
$('#emergency-form').addEventListener('submit', submitEmergency);
$('#discharge-form').addEventListener('submit', submitDischarge);
document.querySelectorAll('.close-button').forEach((button) => button.addEventListener('click', () => $('#discharge-dialog').close()));
refreshData();

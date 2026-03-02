/**
 * Insurer Portal – Public API client
 * Handles upload, process, and status for the Data Pipeline Service
 */

const API_BASE = ''; // Same origin when served by Spring Boot

// DOM Elements
const uploadForm = document.getElementById('upload-form');
const fileInput = document.getElementById('file');
const fileLabel = document.getElementById('file-label');
const uploadBtn = document.getElementById('upload-btn');
const jobSection = document.getElementById('job-section');
const jobIdEl = document.getElementById('job-id');
const statusBadge = document.getElementById('status-badge');
const statusDetail = document.getElementById('status-detail');
const processBtn = document.getElementById('process-btn');
const refreshStatusBtn = document.getElementById('refresh-status-btn');
const resultSection = document.getElementById('result-section');
const totalRecordsEl = document.getElementById('total-records');
const processedRecordsEl = document.getElementById('processed-records');
const finalStatusEl = document.getElementById('final-status');
const failureReasonEl = document.getElementById('failure-reason');
const verificationFailuresEl = document.getElementById('verification-failures');
const verificationFailuresListEl = document.getElementById('verification-failures-list');
const copyJobIdBtn = document.getElementById('copy-job-id');
const toast = document.getElementById('toast');

let currentJobId = null;

// ----- File input UI -----
fileInput.addEventListener('change', () => {
  const file = fileInput.files[0];
  fileLabel.textContent = file ? file.name : 'Choose CSV or Excel file';
  fileInput.closest('.file-input-wrapper').classList.toggle('has-file', !!file);
});

// ----- Toast notifications -----
function showToast(message, type = 'info') {
  toast.textContent = message;
  toast.className = 'toast show ' + type;
  setTimeout(() => toast.classList.remove('show'), 3500);
}

// ----- API: Upload -----
uploadForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const file = fileInput.files[0];
  if (!file) {
    showToast('Please select a file', 'error');
    return;
  }

  const insurerId = document.getElementById('insurerId').value;
  const uploadedBy = document.getElementById('uploadedBy').value.trim();
  if (!insurerId || !uploadedBy) {
    showToast('Please fill all required fields', 'error');
    return;
  }

  setLoading(uploadBtn, true);
  const formData = new FormData();
  formData.append('file', file);
  formData.append('insurerId', insurerId);
  formData.append('uploadedBy', uploadedBy);

  try {
    const res = await fetch(`${API_BASE}/api/public/v1/ingestion/upload`, {
      method: 'POST',
      body: formData,
    });

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
      const errMsg = data.error?.details || data.error?.message || data.message || data.error || `Upload failed (${res.status})`;
      throw new Error(errMsg);
    }

    currentJobId = data.jobId || data.data?.jobId;
    showJobSection(data.jobId);
    resultSection.style.display = 'none';
    showToast('File uploaded successfully', 'success');
    await refreshStatus();
  } catch (err) {
    showToast(err.message || 'Upload failed', 'error');
    console.error(err);
  } finally {
    setLoading(uploadBtn, false);
  }
});

// ----- API: Process -----
processBtn.addEventListener('click', async () => {
  if (!currentJobId) return;

  setLoading(processBtn, true);
  const policyType = getPolicyTypeForInsurer(document.getElementById('insurerId').value);
  const url = policyType
    ? `${API_BASE}/api/public/v1/ingestion/process/${currentJobId}?policyType=${encodeURIComponent(policyType)}`
    : `${API_BASE}/api/public/v1/ingestion/process/${currentJobId}`;
  try {
    const res = await fetch(url, {
      method: 'POST',
    });

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
      const errMsg = data.error?.details || data.error?.message || data.message || data.error || (typeof data.error === 'string' ? data.error : null) || `Process failed (${res.status})`;
      throw new Error(errMsg);
    }

    showToast('Processing started', 'success');
    statusBadge.textContent = 'PROCESSING';
    statusBadge.className = 'status-badge processing';

    // Poll for completion
    pollStatus();
  } catch (err) {
    showToast(err.message || 'Failed to start processing', 'error');
    console.error(err);
  } finally {
    setLoading(processBtn, false);
  }
});

function getPolicyTypeForInsurer(insurerId) {
  const map = {
    HEALTH_INSURER: 'HEALTH',
    AUTO_INSURER: 'MOTOR',
    LIFE_INSURER: 'TERM_LIFE',
  };
  return map[insurerId] || null;
}

// ----- API: Status -----
refreshStatusBtn.addEventListener('click', () => refreshStatus());

async function refreshStatus() {
  if (!currentJobId) return;

  try {
    const data = await fetchStatus(currentJobId);
    updateJobUI(data);
  } catch (err) {
    showToast('Failed to fetch status', 'error');
  }
}

async function fetchStatus(jobId) {
  const res = await fetch(`${API_BASE}/api/public/v1/ingestion/status/${jobId}`);
  if (!res.ok) throw new Error(`Status failed (${res.status})`);
  return res.json();
}

function pollStatus() {
  const interval = setInterval(async () => {
    try {
      const data = await fetchStatus(currentJobId);
      updateJobUI(data);

      if (data.status === 'COMPLETED' || data.status === 'FAILED') {
        clearInterval(interval);
        showResultSection(data);
        if (data.status === 'COMPLETED') {
          showToast(`Processing completed: ${data.processedRecords || 0} records`, 'success');
        } else if (data.status === 'FAILED') {
          showToast('Processing failed', 'error');
        }
      }
    } catch {
      clearInterval(interval);
    }
  }, 2000);
}

// ----- UI updates -----
function showJobSection(jobId) {
  jobSection.style.display = 'block';
  jobIdEl.textContent = jobId;
  statusBadge.textContent = 'UPLOADED';
  statusBadge.className = 'status-badge uploaded';
  statusDetail.textContent = 'File is ready. Click "Start Processing" to run the pipeline.';
}

function updateJobUI(data) {
  statusBadge.textContent = data.status;
  statusBadge.className = 'status-badge ' + (data.status || '').toLowerCase();

  if (data.status === 'UPLOADED') {
    statusDetail.textContent = 'File is ready. Click "Start Processing" to run the pipeline.';
  } else if (data.status === 'PROCESSING') {
    statusDetail.textContent = `Processing... ${data.processedRecords || 0} / ${data.totalRecords || '?'} records`;
  } else if (data.status === 'COMPLETED') {
    statusDetail.textContent = `Done: ${data.processedRecords || 0} records processed`;
  } else if (data.status === 'FAILED') {
    statusDetail.textContent = data.failureReason || 'Processing failed';
  }
}

function showResultSection(data) {
  resultSection.style.display = 'block';
  totalRecordsEl.textContent = data.totalRecords ?? '–';
  processedRecordsEl.textContent = data.processedRecords ?? '–';
  finalStatusEl.textContent = data.status;
  finalStatusEl.className = 'result-value status-text ' + (data.status || '').toLowerCase();

  if (data.status === 'FAILED' && data.failureReason) {
    failureReasonEl.textContent = data.failureReason;
    failureReasonEl.style.display = 'block';
  } else {
    failureReasonEl.style.display = 'none';
  }

  if (data.verificationFailures && data.verificationFailures.length > 0) {
    verificationFailuresEl.style.display = 'block';
    verificationFailuresListEl.innerHTML = data.verificationFailures.map(f =>
      `<div class="verification-failure-item"><code>${escapeHtml(f.policyNumber || '?')}</code>: ${escapeHtml(f.reason || 'Unknown')}</div>`
    ).join('');
  } else {
    verificationFailuresEl.style.display = 'none';
    verificationFailuresListEl.innerHTML = '';
  }
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function setLoading(btn, loading) {
  btn.classList.toggle('loading', loading);
  btn.disabled = loading;
}

// ----- Copy Job ID -----
copyJobIdBtn.addEventListener('click', () => {
  if (!currentJobId) return;
  navigator.clipboard.writeText(currentJobId).then(
    () => showToast('Job ID copied', 'success'),
    () => showToast('Copy failed', 'error')
  );
});

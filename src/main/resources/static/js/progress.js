(() => {
    const panel = document.querySelector('.progress-panel');
    if (!panel) {
        return;
    }
    const jobId = panel.getAttribute('data-job-id');
    const statusText = document.getElementById('status-text');
    const phaseText = document.getElementById('phase-text');
    const progressFill = document.getElementById('progress-fill');
    const progressPercent = document.getElementById('progress-percent');
    const rowCount = document.getElementById('row-count');
    const errorText = document.getElementById('error-text');
    const downloadLink = document.getElementById('download-link');

    const update = (payload) => {
        statusText.textContent = payload.status;
        phaseText.textContent = payload.phase;
        progressFill.style.width = `${payload.progressPercent}%`;
        progressPercent.textContent = payload.progressPercent;
        rowCount.textContent = payload.rowCount ?? 0;
        if (payload.errorMessage) {
            errorText.textContent = payload.errorMessage;
            errorText.style.display = 'block';
        }
        if (payload.status === 'COMPLETED') {
            downloadLink.style.display = 'block';
        }
    };

    const poll = async () => {
        try {
            const response = await fetch(`/exports/${jobId}/status`, { cache: 'no-store' });
            if (!response.ok) {
                return;
            }
            const payload = await response.json();
            update(payload);
            if (payload.status === 'COMPLETED' || payload.status === 'FAILED') {
                clearInterval(intervalId);
            }
        } catch (err) {
            // ignore transient polling errors
        }
    };

    poll();
    const intervalId = setInterval(poll, 2000);
})();

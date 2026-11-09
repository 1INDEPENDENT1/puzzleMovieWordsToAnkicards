(() => {
    const panel = document.querySelector('.progress-panel');
    if (!panel) return;
    const jobId = panel.getAttribute('data-job-id');
    const statusText = document.getElementById('status-text');
    const phaseText = document.getElementById('phase-text');
    const progressFill = document.getElementById('progress-fill');
    const progressPercent = document.getElementById('progress-percent');
    const rowCount = document.getElementById('row-count');
    const errorText = document.getElementById('error-text');
    const warningText = document.getElementById('warning-text');
    const warningMessage = document.getElementById('warning-message');
    const unparsedCount = document.getElementById('unparsed-count');
    const downloadLink = document.getElementById('download-link');
    const reviewCreate = document.getElementById('review-create');
    const reviewUnavailable = document.getElementById('review-unavailable');
    let stopped = false;
    let timer;
    let redirecting = false;

    const show = (node, visible) => { if (node) node.style.display = visible ? '' : 'none'; };
    const update = (payload) => {
        const partial = payload.completeness === 'PARTIAL';
        statusText.textContent = partial ? 'COMPLETED (partial export)' : payload.status;
        phaseText.textContent = payload.phase;
        progressFill.style.width = `${payload.progressPercent}%`;
        progressPercent.textContent = payload.progressPercent;
        rowCount.textContent = payload.rowCount ?? 0;
        errorText.textContent = payload.errorMessage ?? '';
        show(errorText, !!payload.errorMessage);
        warningMessage.textContent = payload.warningMessage ?? '';
        unparsedCount.textContent = payload.unparsedRecordCount ?? 0;
        show(warningText, partial);
        // Only server rendering registers presentation in the session. Polling never does.
        const presented = panel.getAttribute('data-warning-presented') === 'true';
        show(downloadLink, payload.downloadAvailable && (!partial || presented));
        show(reviewCreate, payload.reviewCreationAvailable && (!partial || presented));
        show(reviewUnavailable, !!payload.completeness && !payload.reviewCreationAvailable);
        if (partial && !presented && !redirecting) {
            redirecting = true;
            window.location.replace(`/exports/${jobId}`);
        }
    };

    const poll = async () => {
        if (stopped) return;
        try {
            const response = await fetch(`/exports/${jobId}/status`, { cache: 'no-store' });
            if (!response.ok) { stopped = true; return; }
            const payload = await response.json();
            update(payload);
            stopped = payload.status === 'COMPLETED' || payload.status === 'FAILED';
        } catch (err) {
            // Retry transient polling errors while the export is active.
        }
        if (!stopped && !redirecting) timer = setTimeout(poll, 2000);
    };

    if (statusText.textContent.trim().startsWith('COMPLETED') || statusText.textContent.trim() === 'FAILED') {
        stopped = true;
    } else {
        poll();
    }
    window.addEventListener('pagehide', () => { stopped = true; clearTimeout(timer); });
})();

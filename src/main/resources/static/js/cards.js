(() => {
    const get = (id) => document.getElementById(id);
    const dialog = get('card-editor');
    const form = get('card-edit-form');
    if (!dialog || !form) return;
    const feedback = get('edit-feedback');
    const status = get('library-status');
    const save = form.querySelector('[type="submit"]');
    const cancel = get('cancel-edit');
    const examplesDialog = get('library-examples');
    const examplesStatus = get('library-example-status');
    const exampleFields = get('library-example-fields');
    const exampleSelect = get('library-example-select');
    const exampleMovie = get('library-example-movie');
    const exampleForm = get('library-example-form');
    const exampleFeedback = get('library-example-feedback');
    const saveExample = exampleForm.querySelector('[type="submit"]');
    const previousPage = get('library-example-previous');
    const nextPage = get('library-example-next');
    const retryPage = get('library-example-retry');
    const closeExamples = get('library-example-close');

    let row = null;
    let trigger = null;
    let saving = false;
    let examplesPage = null;
    let pageRequest = 0;
    let failedPage = 0;

    const cardPath = () => `/cards/${encodeURIComponent(row.dataset.cardId)}`;
    const updateRequest = (form) => ({
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            originalText: form.elements.originalText.value,
            translationText: form.elements.translationText.value,
            version: Number(form.elements.version.value)
        })
    });

    const requireSuccessfulSave = (response, conflict) => {
        if (response.status === 409) throw new Error(conflict);
        if (response.status === 400) throw new Error('Check the text lengths and enter a nonblank original text.');
        if (response.status === 401) throw new Error('Sign in again before saving.');
        if (response.status === 404) throw new Error('This material is no longer available. Reload the page.');
        if (!response.ok) throw new Error('Changes were not saved. Try again.');
    };

    const requireOriginal = (form, feedback) => {
        if (form.elements.originalText.value.trim()) return true;
        feedback.textContent = 'Original text is required.';
        form.elements.originalText.focus();
        return false;
    };

    const selectExample = () => {
        const item = examplesPage.items[exampleSelect.selectedIndex];
        if (!item) return;
        exampleForm.elements.version.value = item.linkVersion;
        exampleForm.elements.originalText.value = item.originalText;
        exampleForm.elements.translationText.value = item.translationText ?? '';
        exampleMovie.textContent = item.movieTitle || 'Source not specified';
        exampleFeedback.textContent = '';
    };

    const exampleLabel = (item) => `${item.position}. ${item.originalText}`;

    const syncPages = () => {
        previousPage.disabled = saving || !examplesPage || examplesPage.page === 0;
        nextPage.disabled = saving || !examplesPage || (examplesPage.page + 1) * examplesPage.pageSize >= examplesPage.total;
    };

    const loadExamples = async (page) => {
        const request = ++pageRequest;
        const id = row.dataset.cardId;
        const isCurrent = () => examplesDialog.open && request === pageRequest && row.dataset.cardId === id;
        examplesStatus.textContent = 'Loading examples…';
        previousPage.disabled = nextPage.disabled = true;
        retryPage.hidden = true;
        exampleFields.hidden = true;
        try {
            const response = await fetch(`${cardPath()}/examples?page=${page}`);
            if (!response.ok) throw new Error();
            const payload = await response.json();
            if (!isCurrent() || payload.cardId !== id || payload.page !== page) return;
            examplesPage = payload;
            const items = payload.items;
            exampleSelect.replaceChildren(...items.map(item => new Option(exampleLabel(item), item.exampleId)));
            if (items.length) {
                examplesStatus.textContent = `Examples ${items[0].position}–${items.at(-1).position} of ${payload.total}`;
                selectExample();
                exampleFields.hidden = false;
            } else {
                examplesStatus.textContent = 'No examples saved.';
            }
            syncPages();
        } catch {
            if (!isCurrent()) return;
            examplesStatus.textContent = 'Examples could not be loaded.';
            failedPage = page;
            retryPage.hidden = false;
        }
    };

    document.querySelectorAll('[data-edit-card], [data-edit-examples]').forEach(button => {
        button.addEventListener('click', () => {
            if (saving || dialog.open || examplesDialog.open) return;
            row = button.closest('[data-card-id]');
            trigger = button;
            if (button.hasAttribute('data-edit-examples')) {
                examplesPage = null;
                examplesDialog.showModal();
                loadExamples(0);
            } else {
                form.elements.version.value = row.dataset.version;
                form.elements.originalText.value = row.querySelector('[data-field="originalText"]').textContent;
                form.elements.translationText.value = row.querySelector('[data-field="translationText"]').dataset.value || '';
                feedback.textContent = '';
                dialog.showModal();
                form.elements.originalText.focus();
            }
        });
    });

    for (const modal of [dialog, examplesDialog]) {
        modal.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
        modal.addEventListener('close', () => {
            if (dialog.open || examplesDialog.open) return;
            pageRequest++;
            trigger?.focus();
        });
    }
    cancel.addEventListener('click', () => { if (!saving) dialog.close(); });
    closeExamples.addEventListener('click', () => { if (!saving) examplesDialog.close(); });
    previousPage.addEventListener('click', () => loadExamples(examplesPage.page - 1));
    nextPage.addEventListener('click', () => loadExamples(examplesPage.page + 1));
    retryPage.addEventListener('click', () => loadExamples(failedPage));
    exampleSelect.addEventListener('change', selectExample);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !row || !dialog.open || !requireOriginal(form, feedback)) return;
        saving = true;
        save.disabled = cancel.disabled = true;
        feedback.textContent = 'Saving…';
        try {
            const response = await fetch(cardPath(), updateRequest(form));
            requireSuccessfulSave(response, 'This card has changed. Reload the page before editing again.');
            const { card } = await response.json();
            row.dataset.version = card.version;
            row.querySelector('[data-field="originalText"]').textContent = card.originalText;
            const translation = row.querySelector('[data-field="translationText"]');
            translation.dataset.value = card.translationText || '';
            translation.textContent = card.translationText?.trim() ? card.translationText : 'No translation saved';
            row.querySelector('[data-edit-card]').setAttribute('aria-label', `Edit ${card.originalText}`);
            row.querySelector('[data-edit-examples]').textContent = `Examples (${card.exampleCount})`;
            status.textContent = 'Card saved.';
            dialog.close();
        } catch (error) {
            feedback.textContent = error.message || 'Changes were not saved. Try again.';
        } finally {
            saving = false;
            save.disabled = cancel.disabled = false;
        }
    });

    exampleForm.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !examplesDialog.open || exampleFields.hidden || !requireOriginal(exampleForm, exampleFeedback)) return;
        const index = exampleSelect.selectedIndex;
        const item = examplesPage.items[index];
        if (!item) return;
        saving = true;
        exampleSelect.disabled = saveExample.disabled = closeExamples.disabled = true;
        syncPages();
        exampleFeedback.textContent = 'Saving…';
        try {
            const response = await fetch(`${cardPath()}/examples/${encodeURIComponent(item.exampleId)}`, updateRequest(exampleForm));
            requireSuccessfulSave(response, 'This example has changed. Close and reopen examples before editing again.');
            const { item: updated } = await response.json();
            examplesPage.items[index] = updated;
            exampleSelect.options[index].textContent = exampleLabel(updated);
            selectExample();
            exampleFeedback.textContent = 'Example saved.';
        } catch (error) {
            exampleFeedback.textContent = error.message || 'Changes were not saved. Try again.';
        } finally {
            saving = false;
            exampleSelect.disabled = saveExample.disabled = closeExamples.disabled = false;
            syncPages();
        }
    });
})();

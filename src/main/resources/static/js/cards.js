(() => {
    const dialog = document.getElementById('card-editor');
    const form = document.getElementById('card-edit-form');
    if (!dialog || !form) return;
    const feedback = document.getElementById('edit-feedback');
    const status = document.getElementById('library-status');
    const save = form.querySelector('[type="submit"]');
    const cancel = document.getElementById('cancel-edit');
    let row = null;
    let saving = false;

    document.querySelectorAll('[data-edit-card]').forEach(button => {
        button.addEventListener('click', () => {
            row = button.closest('[data-card-id]');
            form.elements.version.value = row.dataset.version;
            form.elements.originalText.value = row.querySelector('[data-field="originalText"]').textContent;
            for (const name of ['translationText', 'instanceText']) {
                form.elements[name].value = row.querySelector(`[data-field="${name}"]`).dataset.value || '';
            }
            feedback.textContent = '';
            dialog.showModal();
            form.elements.originalText.focus();
        });
    });
    cancel.addEventListener('click', () => { if (!saving) dialog.close(); });
    dialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !row) return;
        if (!form.elements.originalText.value.trim()) {
            feedback.textContent = 'Original text is required.';
            form.elements.originalText.focus();
            return;
        }
        saving = true;
        save.disabled = cancel.disabled = true;
        feedback.textContent = 'Saving…';
        try {
            const response = await fetch(`/cards/${row.dataset.cardId}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    originalText: form.elements.originalText.value,
                    translationText: form.elements.translationText.value,
                    instanceText: form.elements.instanceText.value,
                    version: Number(form.elements.version.value)
                })
            });
            if (response.status === 409) throw new Error('This card has changed. Reload the page before editing again.');
            if (response.status === 400) throw new Error('Check the text lengths and enter a nonblank original text.');
            if (response.status === 401) throw new Error('Sign in again before saving.');
            if (response.status === 404) throw new Error('This card is no longer available. Reload the page.');
            if (!response.ok) throw new Error('Changes were not saved. Try again.');
            const { card } = await response.json();
            row.dataset.version = card.version;
            for (const name of ['originalText', 'translationText', 'instanceText']) {
                const cell = row.querySelector(`[data-field="${name}"]`);
                cell.dataset.value = card[name] || '';
                cell.textContent = card[name] || (name === 'translationText' ? 'No translation saved' : 'No example saved');
            }
            row.querySelector('[data-edit-card]').setAttribute('aria-label', `Edit ${card.originalText}`);
            status.textContent = 'Card saved. Answer statistics are unchanged.';
            dialog.close();
        } catch (error) {
            feedback.textContent = error.message || 'Changes were not saved. Try again.';
        } finally {
            saving = false;
            save.disabled = cancel.disabled = false;
        }
    });
})();

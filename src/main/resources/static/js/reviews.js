(() => {
    const get = (id) => document.getElementById(id);
    const card = get('review-card');
    if (!card) return;

    const revealButton = get('reveal-button');
    const answerButtons = Array.from(document.querySelectorAll('.answer-button'));
    const status = get('review-status');
    const refreshButton = get('refresh-review');
    const editor = get('review-editor');
    const editForm = get('review-edit-form');
    const editFeedback = get('review-edit-feedback');
    const cancelEdit = get('cancel-review-edit');
    const examples = get('review-examples');
    const examplePosition = get('example-position');
    const exampleOriginal = get('example-original');
    const exampleMovie = get('example-movie');
    const exampleTranslation = get('example-translation');
    const exampleGate = get('example-gate');
    const exampleStatus = get('example-status');
    const previousExample = get('example-previous');
    const nextExample = get('example-next');
    const retryExample = get('example-retry');
    const translateExample = get('example-reveal');
    const lookupExample = get('example-lookup');
    const pageSize = 25;

    let revealedAt = null;
    let busy = false;
    let stale = false;
    let exampleIndex = 0;
    let exampleRevision = 0;
    let examplePage = null;
    let exampleItem = null;
    let exampleTrigger = null;
    let failedExampleIndex = 0;

    const setAnswerEnabled = (enabled) => {
        answerButtons.forEach(button => { button.disabled = !enabled; });
    };

    const presentation = () => ({
        cardId: card.dataset.cardId,
        presentationId: card.dataset.presentationId,
        effectiveContentVersion: card.dataset.contentVersion
    });

    const isCurrent = (view, payload = view) => view.cardId === card.dataset.cardId
        && view.presentationId === card.dataset.presentationId
        && view.effectiveContentVersion === card.dataset.contentVersion
        && payload.cardId === view.cardId
        && payload.presentationId === view.presentationId
        && !stale;

    const reviewPath = (cardId) => `/reviews/cards/${encodeURIComponent(cardId)}`;
    const json = (method, body) => ({
        method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    });

    const resetExampleTranslation = () => {
        exampleTranslation.hidden = true;
        exampleTranslation.textContent = '';
    };

    const syncExampleActions = () => {
        const itemAvailable = !!exampleItem && !stale;
        previousExample.disabled = !itemAvailable || exampleIndex === 0;
        nextExample.disabled = !itemAvailable || exampleIndex + 1 >= examplePage.total;
        translateExample.disabled = !itemAvailable || revealedAt === null;
        exampleGate.hidden = !itemAvailable || revealedAt !== null;
        lookupExample.hidden = !itemAvailable || revealedAt === null;
        lookupExample.removeAttribute('href');
        if (!lookupExample.hidden) {
            const query = new URLSearchParams({ sl: 'en', tl: 'ru', text: exampleItem.originalText, op: 'translate' });
            lookupExample.href = `https://translate.google.com/?${query}`;
        }
    };

    const showMovie = (item) => {
        exampleMovie.textContent = item.movieTitle || 'Source not specified';
        if (!item.movieUrl || !item.movieTitle) return;
        try {
            const url = new URL(item.movieUrl);
            if (url.protocol !== 'http:' && url.protocol !== 'https:') return;
            const link = document.createElement('a');
            link.href = url.href;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            link.textContent = item.movieTitle;
            exampleMovie.replaceChildren(link);
        } catch {
            // The film title remains visible when its URL is invalid.
        }
    };

    const hideAnswers = () => {
        revealedAt = null;
        const back = get('card-back');
        if (back) {
            back.hidden = true;
            get('card-translation').textContent = '';
        }
        setAnswerEnabled(false);
        resetExampleTranslation();
        syncExampleActions();
    };

    const expirePresentation = () => {
        stale = true;
        hideAnswers();
        revealButton.disabled = true;
        refreshButton.hidden = false;
        status.textContent = 'This card has changed. Show the card again.';
        if (examples.open) examples.close();
    };

    const renderExample = () => {
        if (!exampleItem) {
            exampleStatus.textContent = 'No examples saved.';
        } else {
            examplePosition.textContent = `${exampleItem.position} of ${examplePage.total}`;
            exampleOriginal.textContent = exampleItem.originalText;
            showMovie(exampleItem);
            exampleStatus.textContent = '';
        }
        syncExampleActions();
    };

    const loadExample = async (index) => {
        const revision = ++exampleRevision;
        const view = presentation();
        resetExampleTranslation();
        retryExample.hidden = true;
        const page = Math.floor(index / pageSize);
        if (examplePage?.page === page) {
            exampleIndex = index;
            exampleItem = examplePage.items[index % pageSize] || null;
            renderExample();
            return;
        }
        exampleStatus.textContent = 'Loading examples…';
        previousExample.disabled = nextExample.disabled = translateExample.disabled = true;
        lookupExample.hidden = true;
        try {
            const query = new URLSearchParams({ presentationId: view.presentationId, page });
            const response = await fetch(`${reviewPath(view.cardId)}/examples?${query}`);
            if (!examples.open || revision !== exampleRevision || !isCurrent(view)) return;
            if (response.status === 409) {
                expirePresentation();
                return;
            }
            if (!response.ok) throw new Error();
            const payload = await response.json();
            if (!examples.open || revision !== exampleRevision || !isCurrent(view, payload)
                || String(payload.effectiveContentVersion) !== view.effectiveContentVersion || payload.page !== page) return;
            examplePage = payload;
            exampleIndex = index;
            exampleItem = payload.items[index % pageSize] || null;
            renderExample();
        } catch {
            if (!examples.open || revision !== exampleRevision || !isCurrent(view)) return;
            exampleStatus.textContent = 'Examples could not be loaded.';
            failedExampleIndex = index;
            retryExample.hidden = false;
            syncExampleActions();
        }
    };

    const showExampleTranslation = async () => {
        if (revealedAt === null || !examples.open || !exampleItem || stale || translateExample.disabled) return;
        const revision = exampleRevision;
        const view = presentation();
        const { exampleId, linkVersion } = exampleItem;
        const stillVisible = () => examples.open && revision === exampleRevision && isCurrent(view)
            && revealedAt !== null && exampleItem?.exampleId === exampleId && exampleItem.linkVersion === linkVersion;
        translateExample.disabled = true;
        exampleStatus.textContent = 'Loading translation…';
        try {
            const response = await fetch(`${reviewPath(view.cardId)}/examples/${encodeURIComponent(exampleId)}/translation`,
                json('POST', { presentationId: view.presentationId, linkVersion }));
            if (!stillVisible()) return;
            if (response.status === 409) {
                expirePresentation();
                return;
            }
            if (!response.ok) throw new Error();
            const payload = await response.json();
            if (!stillVisible() || !isCurrent(view, payload) || payload.exampleId !== exampleId
                || payload.linkVersion !== linkVersion) return;
            exampleTranslation.textContent = payload.translationAvailable ? payload.translationText : 'No translation saved';
            exampleTranslation.hidden = false;
            exampleStatus.textContent = '';
        } catch {
            if (!stillVisible()) return;
            resetExampleTranslation();
            exampleStatus.textContent = 'Translation could not be loaded. Try again.';
        } finally {
            if (stillVisible()) translateExample.disabled = false;
        }
    };

    examples.addEventListener('close', () => {
        if (examples.open) return;
        exampleRevision++;
        resetExampleTranslation();
        exampleTrigger?.focus();
        exampleTrigger = null;
    });
    get('example-close').addEventListener('click', () => examples.close());
    previousExample.addEventListener('click', () => loadExample(exampleIndex - 1));
    nextExample.addEventListener('click', () => loadExample(exampleIndex + 1));
    retryExample.addEventListener('click', () => loadExample(failedExampleIndex));
    translateExample.addEventListener('click', showExampleTranslation);

    const renderCard = (nextCard) => {
        if (examples.open) examples.close();
        exampleRevision++;
        exampleIndex = 0;
        exampleItem = null;
        examplePage = null;
        examplePosition.textContent = '';
        exampleOriginal.textContent = '';
        exampleMovie.textContent = '';
        exampleStatus.textContent = '';
        hideAnswers();
        stale = false;
        refreshButton.hidden = true;
        get('answer-controls').hidden = !nextCard;
        if (!nextCard) {
            card.className = 'review-card is-empty';
            delete card.dataset.cardId;
            delete card.dataset.presentationId;
            card.innerHTML = `
                <div class="empty-state" id="empty-state">
                    <h2>No cards due</h2>
                    <p class="muted">Create cards from a completed export or come back when the next review is due.</p>
                </div>`;
            status.textContent = 'No cards due.';
            return;
        }

        card.className = 'review-card';
        card.dataset.cardId = nextCard.id;
        card.dataset.presentationId = nextCard.presentationId;
        card.dataset.contentVersion = nextCard.effectiveContentVersion;
        card.dataset.version = nextCard.version;
        card.dataset.original = nextCard.originalText;
        card.dataset.translation = nextCard.translationText || '';
        card.innerHTML = `
            <div class="review-content" id="review-content">
                <div class="card-face card-front">
                    <span class="eyebrow"></span>
                    <button type="button" class="button-secondary edit-current" data-edit-current>Edit card</button>
                    <h2 id="card-original"></h2>
                    <button type="button" class="button-secondary open-examples" data-open-examples
                            aria-haspopup="dialog"></button>
                </div>
                <div class="card-face card-back" id="card-back" hidden>
                    <h3>Answer</h3>
                    <p class="translation preserved-text" id="card-translation"></p>
                    <div class="lookup-actions" id="lookup-actions"></div>
                </div>
            </div>`;
        card.querySelector('.eyebrow').textContent = nextCard.state;
        get('card-original').textContent = nextCard.originalText;
        card.querySelector('[data-open-examples]').textContent = `Examples (${nextCard.exampleCount})`;
        const actions = (nextCard.lookupActions || []).filter(action =>
            action.type === 'DICTIONARY' || action.type === 'PRONUNCIATION');
        actions.forEach(action => {
            const link = document.createElement('a');
            link.href = action.url;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            link.textContent = action.label;
            get('lookup-actions').appendChild(link);
        });
        revealButton.disabled = false;
        status.textContent = '';
    };

    const updateCounts = (counts) => {
        get('reviewed-count').textContent = counts.reviewedCount;
        get('remaining-count').textContent = counts.remainingDueCount;
        get('next-due').textContent = counts.nextDueAt ? 'Next later' : '';
    };

    const loadNext = async () => {
        const response = await fetch('/reviews/next');
        if (!response.ok) throw new Error();
        const payload = await response.json();
        updateCounts(payload.counts);
        renderCard(payload.card);
    };

    const reveal = async () => {
        if (busy || stale || !card.dataset.cardId || editor.open || examples.open || revealedAt !== null) return;
        const view = presentation();
        busy = true;
        revealButton.disabled = true;
        try {
            const response = await fetch(`${reviewPath(view.cardId)}/reveal`, json('POST', { presentationId: view.presentationId }));
            if (!isCurrent(view)) return;
            if (response.status === 409) {
                expirePresentation();
                return;
            }
            if (!response.ok) throw new Error();
            const payload = await response.json();
            if (!isCurrent(view, payload) || payload.mainRevealed !== true) return;
            get('card-translation').textContent = payload.translationText?.trim() ? payload.translationText : 'No translation saved';
            get('card-back').hidden = false;
            revealedAt = performance.now();
            setAnswerEnabled(true);
            status.textContent = 'Answer revealed.';
        } catch {
            status.textContent = 'Answer could not be revealed. Try again.';
        } finally {
            busy = false;
            revealButton.disabled = stale || revealedAt !== null;
        }
    };

    const saveEdit = async (event) => {
        event.preventDefault();
        if (busy || !card.dataset.cardId) return;
        if (!editForm.elements.originalText.value.trim()) {
            editFeedback.textContent = 'Original text is required.';
            editForm.elements.originalText.focus();
            return;
        }
        busy = true;
        const saveButton = editForm.querySelector('[type="submit"]');
        saveButton.disabled = cancelEdit.disabled = true;
        editFeedback.textContent = 'Saving…';
        try {
            const response = await fetch(`/cards/${encodeURIComponent(card.dataset.cardId)}`, json('PATCH', {
                originalText: editForm.elements.originalText.value,
                translationText: editForm.elements.translationText.value,
                version: Number(editForm.elements.version.value)
            }));
            if (response.status === 409) {
                expirePresentation();
                throw new Error('This card has changed. Close the editor and show the card again.');
            }
            if (response.status === 400) throw new Error('Check the text lengths and enter a nonblank original text.');
            if (response.status === 401) throw new Error('Sign in again before saving.');
            if (response.status === 404) throw new Error('This card is no longer available. Reload the page.');
            if (!response.ok) throw new Error('Changes were not saved. Try again.');
            editor.close();
            expirePresentation();
            try {
                await loadNext();
                card.querySelector('[data-edit-current]')?.focus();
                status.textContent = card.dataset.cardId ? 'Card saved.' : 'Card saved. No cards due.';
            } catch {
                status.textContent = 'Card saved. Show the card again to continue.';
                refreshButton.focus();
            }
        } catch (error) {
            editFeedback.textContent = error.message || 'Changes were not saved. Try again.';
        } finally {
            busy = false;
            saveButton.disabled = cancelEdit.disabled = false;
        }
    };

    card.addEventListener('click', (event) => {
        if (busy || stale || editor.open || examples.open) return;
        const trigger = event.target.closest('[data-open-examples]');
        if (trigger) {
            exampleTrigger = trigger;
            examplePage = null;
            exampleItem = null;
            examplePosition.textContent = '';
            exampleOriginal.textContent = '';
            exampleMovie.textContent = '';
            syncExampleActions();
            examples.showModal();
            loadExample(exampleIndex);
        } else if (event.target.closest('[data-edit-current]')) {
            editForm.elements.version.value = card.dataset.version;
            editForm.elements.originalText.value = card.dataset.original;
            editForm.elements.translationText.value = card.dataset.translation;
            editFeedback.textContent = '';
            editor.showModal();
            editForm.elements.originalText.focus();
        }
    });

    const answer = async (button) => {
        if (busy || stale || editor.open || examples.open || revealedAt === null || !card.dataset.cardId) return;
        busy = true;
        setAnswerEnabled(false);
        const answerValue = button.dataset.answer;
        const className = answerValue === 'KNOWN' ? 'answered-known' : 'answered-unknown';
        try {
            const response = await fetch(`${reviewPath(card.dataset.cardId)}/answer`, json('POST', {
                answer: answerValue,
                responseMillis: Math.round(performance.now() - revealedAt)
            }));
            if (response.status === 409) {
                expirePresentation();
                return;
            }
            if (!response.ok) throw new Error();
            const payload = await response.json();
            hideAnswers();
            updateCounts(payload.counts);
            if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                card.classList.add(className);
                await new Promise(resolve => window.setTimeout(resolve, 240));
            }
            renderCard(payload.nextCard);
        } catch {
            status.textContent = 'Answer was not saved. Try again.';
            setAnswerEnabled(true);
        } finally {
            busy = false;
        }
    };

    refreshButton.addEventListener('click', async () => {
        if (busy) return;
        busy = true;
        refreshButton.disabled = true;
        try {
            await loadNext();
            if (card.dataset.cardId) revealButton.focus();
        } catch {
            status.textContent = 'The card could not be loaded. Try again.';
        } finally {
            busy = false;
            refreshButton.disabled = false;
        }
    });
    editForm.addEventListener('submit', saveEdit);
    cancelEdit.addEventListener('click', () => { if (!busy) editor.close(); });
    editor.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
    revealButton.addEventListener('click', reveal);
    answerButtons.forEach(button => button.addEventListener('click', () => answer(button)));
})();

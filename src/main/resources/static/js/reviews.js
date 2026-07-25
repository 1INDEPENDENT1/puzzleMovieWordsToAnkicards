(() => {
    const card = document.getElementById('review-card');
    const back = document.getElementById('card-back');
    const revealButton = document.getElementById('reveal-button');
    const answerButtons = Array.from(document.querySelectorAll('.answer-button'));
    const status = document.getElementById('review-status');
    const reviewedCount = document.getElementById('reviewed-count');
    const remainingCount = document.getElementById('remaining-count');
    const nextDue = document.getElementById('next-due');

    if (!card) {
        return;
    }

    let revealedAt = null;
    let submitting = false;

    const setStatus = (message) => {
        if (status) {
            status.textContent = message;
        }
    };

    const setAnswerEnabled = (enabled) => {
        answerButtons.forEach((button) => {
            button.disabled = !enabled;
        });
    };

    const reveal = () => {
        const activeBack = document.getElementById('card-back');
        if (!activeBack || !revealButton) {
            return;
        }
        activeBack.hidden = false;
        revealButton.disabled = true;
        revealedAt = performance.now();
        setAnswerEnabled(true);
        setStatus('Answer revealed.');
    };

    const text = (value, fallback = '') => value == null || value === '' ? fallback : value;

    const renderLookupActions = (actions) => {
        const container = document.getElementById('lookup-actions');
        if (!container) {
            return;
        }
        container.replaceChildren();
        (actions || []).forEach((action) => {
            const link = document.createElement('a');
            link.href = action.url;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            link.textContent = action.label;
            container.appendChild(link);
        });
    };

    const renderCard = (nextCard) => {
        if (!nextCard) {
            card.className = 'review-card is-empty';
            card.removeAttribute('data-card-id');
            card.innerHTML = `
                <div class="empty-state" id="empty-state">
                    <h2>No cards due</h2>
                    <p class="muted">Create cards from a completed export or come back when the next review is due.</p>
                </div>`;
            const controls = document.getElementById('answer-controls');
            if (controls) {
                controls.style.display = 'none';
            }
            setStatus('No cards due.');
            return;
        }

        card.className = 'review-card';
        card.setAttribute('data-card-id', nextCard.id);
        card.innerHTML = `
            <div class="review-content" id="review-content">
                <div class="card-face card-front">
                    <span class="eyebrow"></span>
                    <h2 id="card-original"></h2>
                    <p class="instance" id="card-instance"></p>
                </div>
                <div class="card-face card-back" id="card-back" hidden>
                    <h3>Answer</h3>
                    <p class="translation preserved-text" id="card-translation"></p>
                    <p class="source" id="card-source"></p>
                    <div class="lookup-actions" id="lookup-actions"></div>
                </div>
            </div>`;
        document.querySelector('.eyebrow').textContent = nextCard.state;
        document.getElementById('card-original').textContent = nextCard.originalText;
        const instance = document.getElementById('card-instance');
        instance.textContent = text(nextCard.instanceText);
        instance.hidden = !nextCard.instanceText;
        document.getElementById('card-translation').textContent = text(nextCard.translationText, 'No translation saved');
        const source = document.getElementById('card-source');
        source.textContent = text(nextCard.sourceContext);
        source.hidden = !nextCard.sourceContext;
        renderLookupActions(nextCard.lookupActions);
        const controls = document.getElementById('answer-controls');
        if (controls) {
            controls.style.display = 'grid';
        }
        if (revealButton) {
            revealButton.disabled = false;
        }
        setAnswerEnabled(false);
        revealedAt = null;
    };

    const updateCounts = (counts) => {
        reviewedCount.textContent = counts.reviewedCount;
        remainingCount.textContent = counts.remainingDueCount;
        nextDue.textContent = counts.nextDueAt ? 'Next later' : '';
    };

    const answer = async (button) => {
        if (submitting || !card.dataset.cardId) {
            return;
        }
        submitting = true;
        setAnswerEnabled(false);
        if (revealButton) {
            revealButton.disabled = true;
        }
        const answerValue = button.dataset.answer;
        const className = answerValue === 'KNOWN' ? 'answered-known' : 'answered-unknown';
        card.classList.add(className);
        try {
            const response = await fetch(`/reviews/cards/${card.dataset.cardId}/answer`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    answer: answerValue,
                    responseMillis: revealedAt ? Math.round(performance.now() - revealedAt) : null
                })
            });
            if (!response.ok) {
                throw new Error('Answer was not saved.');
            }
            const payload = await response.json();
            updateCounts(payload.counts);
            window.setTimeout(() => {
                card.classList.remove(className);
                renderCard(payload.nextCard);
                submitting = false;
            }, 220);
        } catch (err) {
            card.classList.remove(className);
            setStatus('Answer was not saved. Try again.');
            setAnswerEnabled(true);
            submitting = false;
        }
    };

    if (back) {
        back.hidden = true;
    }
    setAnswerEnabled(false);
    if (revealButton) {
        revealButton.addEventListener('click', reveal);
    }
    answerButtons.forEach((button) => {
        button.addEventListener('click', () => answer(button));
    });
})();

import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { join } from 'node:path';
import { setTimeout as pause } from 'node:timers/promises';

const [directory, executable] = process.argv.slice(2);
const examples = Array.from({ length: 100 }, (_, index) => ({
    exampleId: `example-${index + 1}`, linkId: `link-${index + 1}`, linkVersion: 1,
    originalText: `Example ${index + 1}`, position: index + 1,
    movieTitle: index === 0 ? 'A film' : null,
    movieUrl: index === 0 ? 'https://example.test/film' : 'javascript:alert(1)'
}));
let total = 0;
let presentationId = 'initial';
let revealed = false;
let delayTranslation = false;
let lateResponseSent = false;
let answerCount = 0;
const card = { id: 'card', originalText: 'Give up', translationText: 'сдаваться',
    version: 2, effectiveContentVersion: 2, state: 'NEW', exampleCount: 100, lookupActions: [] };
const server = createServer(async (request, response) => {
    const url = new URL(request.url, 'http://localhost');
    const send = (data, status = 200) => {
        response.writeHead(status, { 'Content-Type': 'application/json' });
        response.end(JSON.stringify(data));
    };
    if (url.pathname === '/0' || url.pathname === '/100') {
        total = Number(url.pathname.slice(1));
        revealed = false;
        presentationId = 'initial';
        response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        response.end(await readFile(join(directory, `review-${total}.html`)));
    } else if (url.pathname === '/cards') {
        response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        response.end(await readFile(join(directory, 'cards.html')));
    } else if (/^\/(css|js)\/(app\.css|reviews\.js|cards\.js)$/.test(url.pathname)) {
        response.writeHead(200, { 'Content-Type': url.pathname.endsWith('.css') ? 'text/css' : 'text/javascript' });
        response.end(await readFile(`src/main/resources/static${url.pathname}`));
    } else if (url.pathname === '/reviews/cards/card/examples') {
        if (url.searchParams.get('presentationId') !== presentationId) return send({}, 409);
        const page = Number(url.searchParams.get('page'));
        send({ cardId: 'card', presentationId, effectiveContentVersion: presentationId === 'initial' ? 1 : 2,
            page, pageSize: 25, total, items: examples.slice(page * 25, Math.min((page + 1) * 25, total)) });
    } else if (url.pathname === '/reviews/cards/card/reveal') {
        const body = JSON.parse(await readBody(request));
        if (body.presentationId !== presentationId) return send({}, 409);
        revealed = true;
        send({ cardId: 'card', presentationId, mainRevealed: true, translationText: 'сдаваться' });
    } else if (url.pathname.endsWith('/translation')) {
        const body = JSON.parse(await readBody(request));
        if (!revealed || body.presentationId !== presentationId || body.linkVersion !== 1) return send({}, 409);
        const exampleId = url.pathname.split('/').at(-2);
        const result = { cardId: 'card', presentationId, exampleId, linkVersion: 1, translationAvailable: true,
            translationText: `Translation of ${exampleId}` };
        if (delayTranslation) {
            delayTranslation = false;
            await pause(300);
            lateResponseSent = true;
        }
        send(result);
    } else if (url.pathname === '/cards/card' && request.method === 'PATCH') {
        const body = JSON.parse(await readBody(request));
        if ('instanceText' in body || body.version !== 1) return send({}, 400);
        send({ card });
    } else if (url.pathname === '/cards/card/examples') {
        const page = Number(url.searchParams.get('page'));
        send({ cardId: 'card', effectiveContentVersion: 2, page, pageSize: 25, total: 100,
            items: examples.slice(page * 25, (page + 1) * 25)
                .map(item => ({ ...item, translationText: `Saved translation of ${item.exampleId}` })) });
    } else if (url.pathname === '/cards/card/examples/example-2' && request.method === 'PATCH') {
        const body = JSON.parse(await readBody(request));
        if (body.version !== 1 || body.originalText !== 'Edited example' || body.translationText !== '') {
            return send({}, 400);
        }
        send({ cardId: 'card', effectiveContentVersion: 3,
            item: { ...examples[1], linkVersion: 2, originalText: body.originalText, translationText: '' } });
    } else if (url.pathname === '/reviews/next') {
        presentationId = 'replacement';
        revealed = false;
        send({ card: { ...card, presentationId }, counts: { reviewedCount: 0, remainingDueCount: 1 } });
    } else if (url.pathname.endsWith('/answer')) {
        answerCount++;
        send({}, 500);
    } else {
        send({}, 404);
    }
});
async function readBody(request) {
    let data = '';
    for await (const chunk of request) data += chunk;
    return data;
}
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const origin = `http://127.0.0.1:${server.address().port}`;

const browser = spawn(executable, ['--headless=new', '--disable-gpu', '--no-first-run',
    '--no-default-browser-check', '--remote-debugging-port=0', `--user-data-dir=${join(directory, 'chrome')}`,
    'about:blank'], { stdio: ['ignore', 'ignore', 'pipe'] });
let socket;
try {
    const endpoint = await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('Chrome DevTools did not start')), 10000);
        browser.on('error', error => { clearTimeout(timeout); reject(error); });
        browser.on('exit', code => { clearTimeout(timeout); reject(new Error(`Chrome exited with ${code}`)); });
        let output = '';
        browser.stderr.on('data', chunk => {
            output += chunk;
            const match = output.match(/DevTools listening on ws:\/\/([^/]+)\//);
            if (match) { clearTimeout(timeout); resolve(`http://${match[1]}`); }
        });
    });
    const targets = await (await fetch(`${endpoint}/json/list`)).json();
    socket = new WebSocket(targets.find(target => target.type === 'page').webSocketDebuggerUrl);
    await new Promise((resolve, reject) => {
        socket.addEventListener('open', resolve, { once: true });
        socket.addEventListener('error', reject, { once: true });
    });
    let sequence = 0;
    const pending = new Map();
    socket.addEventListener('message', event => {
        const message = JSON.parse(event.data);
        pending.get(message.id)?.(message);
    });
    const call = (method, params = {}) => new Promise((resolve, reject) => {
        const id = ++sequence;
        pending.set(id, message => {
            pending.delete(id);
            if (message.error) reject(new Error(message.error.message));
            else resolve(message.result);
        });
        socket.send(JSON.stringify({ id, method, params }));
    });
    const evaluate = async expression => {
        const { result, exceptionDetails } = await call('Runtime.evaluate', { expression, returnByValue: true });
        if (exceptionDetails) throw new Error(result.description);
        return result.value;
    };
    const wait = async expression => {
        for (let attempt = 0; attempt < 80; attempt++) {
            if (await evaluate(expression)) return;
            await pause(50);
        }
        const status = await evaluate(`document.querySelector('#example-status')?.textContent`);
        assert.fail(`Browser condition timed out: ${expression} (example status: ${status})`);
    };
    const click = selector => evaluate(`document.querySelector(${JSON.stringify(selector)}).click()`);
    const shown = selector => `!!document.querySelector('${selector}')?.getClientRects().length`;
    const hasText = (selector, value) =>
        `document.querySelector('${selector}')?.textContent.includes(${JSON.stringify(value)})`;
    const navigate = async path => {
        await call('Page.navigate', { url: origin + path });
        await wait(`location.pathname === '${path}' && document.readyState === 'complete'`);
    };
    const key = async (key, code) => {
        const windowsVirtualKeyCode = key === 'Escape' ? 27 : 9;
        await call('Input.dispatchKeyEvent', { type: 'rawKeyDown', key, code, windowsVirtualKeyCode });
        await call('Input.dispatchKeyEvent', { type: 'keyUp', key, code, windowsVirtualKeyCode });
    };

    await call('Emulation.setDeviceMetricsOverride', { width: 360, height: 740, deviceScaleFactor: 1, mobile: true });
    await call('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] });
    await call('Network.enable');
    await call('Network.setBlockedURLs', { urls: ['https://fonts.googleapis.com/*', 'https://fonts.gstatic.com/*'] });
    await navigate('/0');
    await wait(hasText('[data-open-examples]', 'Examples (0)'));
    await click('[data-open-examples]');
    await wait(hasText('#example-status', 'No examples'));
    assert.equal(await evaluate(`document.querySelector('#example-next').disabled`), true);

    await navigate('/100');
    await click('[data-open-examples]');
    await wait(hasText('#example-position', '1 of 100'));
    assert.equal(await evaluate(`document.querySelector('#example-movie a').href`), 'https://example.test/film');
    assert.equal(await evaluate(`document.querySelector('#example-reveal').disabled`), true);
    assert.equal(await evaluate(shown('#example-lookup')), false);
    assert.equal(await evaluate(`document.querySelector('#example-previous').disabled`), true);
    assert.equal(await evaluate(`document.documentElement.scrollWidth === 360 &&
        document.querySelector('#review-examples').getBoundingClientRect().width < 360 &&
        matchMedia('(prefers-reduced-motion: reduce)').matches`), true);
    assert.equal(await evaluate(`(() => {
        const review = document.querySelector('#review-card');
        review.classList.add('answered-known');
        const duration = parseFloat(getComputedStyle(review).animationDuration);
        review.classList.remove('answered-known');
        return duration < 0.001;
    })()`), true);
    for (let index = 0; index < 8; index++) {
        await key('Tab', 'Tab');
        assert.equal(await evaluate(`document.querySelector('#review-examples').contains(document.activeElement) ||
            document.activeElement === document.body`), true);
    }
    await key('Escape', 'Escape');
    await wait(`document.activeElement.matches('[data-open-examples]') && !document.querySelector('#review-examples').open`);
    await click('#reveal-button');
    await wait(shown('#card-back'));
    await click('[data-open-examples]');
    await wait(hasText('#example-position', '1 of 100'));
    assert.equal(await evaluate(shown('#example-translation')), false);
    await call('Network.emulateNetworkConditions', { offline: true, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
    await click('#example-reveal');
    await wait(hasText('#example-status', 'Try again'));
    assert.equal(await evaluate(shown('#example-translation')), false);
    await call('Network.emulateNetworkConditions', { offline: false, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
    await click('#example-reveal');
    await wait(hasText('#example-translation', 'Translation of example-1'));
    assert.equal(await evaluate(shown('#example-translation')), true);

    await click('#example-next');
    await wait(hasText('#example-position', '2 of 100'));
    assert.equal(await evaluate(`document.querySelector('#example-movie a') === null`), true);
    delayTranslation = true;
    await click('#example-reveal');
    await click('#example-next');
    await wait(hasText('#example-position', '3 of 100'));
    for (let attempt = 0; !lateResponseSent && attempt < 100; attempt++) await pause(20);
    assert.equal(lateResponseSent, true, 'The server sent the delayed translation');
    await pause(100);
    assert.equal(await evaluate(shown('#example-translation')), false);
    for (let position = 4; position <= 100; position++) {
        await click('#example-next');
        await wait(`document.querySelector('#example-position').textContent === '${position} of 100'`);
        assert.equal(await evaluate(`document.querySelector('#example-original').textContent`), `Example ${position}`);
    }
    assert.equal(await evaluate(`document.querySelector('#example-next').disabled`), true);
    await click('#example-reveal');
    await wait(hasText('#example-translation', 'Translation of example-100'));
    await key('Escape', 'Escape');
    await wait(`document.activeElement.matches('[data-open-examples]')`);
    await click('[data-open-examples]');
    await wait(hasText('#example-position', '100 of 100'));
    assert.equal(await evaluate(shown('#example-translation')), false);
    assert.equal(await evaluate(`document.querySelector('#example-reveal').disabled`), false);
    await key('Escape', 'Escape');
    await click('[data-edit-current]');
    await click('#review-edit-form button[type=submit]');
    await wait(hasText('#review-status', 'Card saved'));
    assert.equal(await evaluate(shown('#card-back')), false);
    await click('[data-open-examples]');
    await wait(hasText('#example-position', '1 of 100'));
    assert.equal(await evaluate(`document.querySelector('#example-reveal').disabled`), true);
    assert.equal(answerCount, 0);

    await navigate('/cards');
    await click('[data-edit-examples]');
    await wait(hasText('#library-example-select', '1. Example 1'));
    await evaluate(`document.querySelector('#library-example-select').selectedIndex = 1;
        document.querySelector('#library-example-select').dispatchEvent(new Event('change'))`);
    assert.equal(await evaluate(`document.querySelector('#library-example-form').elements.originalText.value`), 'Example 2');
    await evaluate(`const form = document.querySelector('#library-example-form');
        form.elements.originalText.value = 'Edited example';
        form.elements.translationText.value = ''`);
    await click('#library-example-form button[type=submit]');
    await wait(hasText('#library-example-feedback', 'Example saved'));
    assert.equal(await evaluate(`document.querySelector('#library-example-form').elements.version.value`), '2');
    assert.equal(await evaluate(`document.querySelector('[data-card-id]').children[3].textContent`), '4');
} finally {
    socket?.close();
    browser.kill();
    server.closeAllConnections();
    server.close();
}

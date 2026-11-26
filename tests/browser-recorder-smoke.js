// Run against a local Next.js server on port 3100 with NEXT_PUBLIC_API_URL=http://127.0.0.1:18080.
// Create /tmp/elearning-recorder-test first, then use a disposable Chrome session:
// playwright-cli -s=elearning-recorder open http://127.0.0.1:3100 --browser=chrome
// playwright-cli -s=elearning-recorder run-code --filename=tests/browser-recorder-smoke.js
// All API/storage calls are intercepted. Capture uses synthetic streams with real Chrome encoding.
async page => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const context = page.context();
  await context.addCookies([{ name: 'session', value: '1', url: 'http://127.0.0.1:3100' },
    { name: 'locale', value: 'en', url: 'http://127.0.0.1:3100' }]);
  const user = { id: '11111111-1111-1111-1111-111111111111', fullName: 'Pilot Teacher', email: 'teacher@example.com', roles: ['INSTRUCTOR'] };
  const classId = `22222222-2222-2222-2222-${String(Date.now() % 1000000000000).padStart(12, '0')}`;
  const recordings = [];
  let currentStatus = 'LIVE', failedPut = false, completeLost = false;
  const uploads = new Map();
  const cors = { 'access-control-allow-origin': 'http://127.0.0.1:3100', 'access-control-allow-credentials': 'true', 'access-control-allow-headers': '*', 'access-control-allow-methods': 'GET,POST,PUT,DELETE,OPTIONS' };
  await page.route('**/*', async route => {
    const request = route.request(), url = new URL(request.url()), path = url.pathname;
    const reply = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', headers: cors, body: JSON.stringify(body) });
    if (url.port === '18081') {
      if (request.method() === 'OPTIONS') return reply({});
      if (request.headers().authorization || request.headers().cookie) throw new Error('Credentials leaked to storage');
      if (!failedPut) { failedPut = true; return reply({}, 503); }
      const [id, part] = path.split('/').slice(-2);
      if (!uploads.has(id)) uploads.set(id, new Map());
      uploads.get(id).set(Number(part), request.postDataBuffer());
      return route.fulfill({ status: 200, headers: { ...cors, ETag: 'part-etag' }, body: '' });
    }
    if (path === '/api/auth/refresh') return reply({ accessToken: 'pilot-test-token' });
    if (url.port !== '18080') {
      if (url.hostname === '127.0.0.1' && url.port === '3100') return route.continue();
      return route.abort();
    }
    if (request.method() === 'OPTIONS') return reply({});
    if (path === '/me') return reply(user);
    if (path === '/instructor/dashboard') return reply({ instructorName: 'Pilot Teacher', upcomingLiveClasses: [{ liveClassId: classId, title: 'Arabic reading — pilot class', startsAt: new Date().toISOString(), status: currentStatus, startUrl: 'https://meet.google.com/abc-defg-hij' }] });
    if (path === '/instructor/google-meet/connection') return reply({ connected: true, email: 'teacher@example.com', sharedOrganizerEnabled: false, calendarSyncEnabled: true });
    if (path.endsWith('/recordings') && request.method() === 'GET') return reply({ enabled: true, title: 'Arabic reading — pilot class', meetingUrl: 'https://meet.google.com/abc-defg-hij', classStatus: currentStatus, recordings });
    if (path.endsWith('/recordings') && request.method() === 'POST') {
      if (JSON.parse(request.postData()).method !== 'BROWSER') throw new Error('Recorder must use BROWSER method');
      const id = `33333333-3333-3333-3333-${String(recordings.length + 1).padStart(12, '0')}`;
      recordings.push({ id, status: 'UPLOADING', method: 'BROWSER', createdAt: new Date().toISOString(), durationSeconds: null, failureMessage: null });
      return reply({ id, uploadToken: 'scoped-test-capability', expiresAt: new Date(Date.now() + 86400000).toISOString(), mimeType: JSON.parse(request.postData()).mimeType, partSizeBytes: 4096, maxBytes: 8589934592 });
    }
    if (path.endsWith('/resume')) {
      const id = path.split('/').at(-2), recording = recordings.find(item => item.id === id);
      if (recording.status !== 'UPLOADING') return reply({ detail: 'Already finalized' }, 409);
      return reply({ id, uploadToken: 'scoped-test-capability', expiresAt: new Date(Date.now() + 86400000).toISOString(), mimeType: 'video/mp4;codecs=avc1,mp4a.40.2', partSizeBytes: 4096, maxBytes: 8589934592 });
    }
    if (path.startsWith('/recording-uploads/')) {
      if (request.headers().authorization || request.headers().cookie) throw new Error('LMS credentials leaked into capability requests');
      if (request.headers()['x-recording-token'] !== 'scoped-test-capability') throw new Error('Missing recording capability');
      const id = path.split('/')[2];
      if (path.includes('/parts/')) return reply({ url: `http://127.0.0.1:18081/upload/${id}/${path.split('/').at(-1)}` });
      if (path.endsWith('/complete')) {
        const parts = uploads.get(id), ordered = [...parts].sort((a,b) => a[0] - b[0]);
        const size = ordered.reduce((sum, pair) => sum + pair[1].length, 0);
        if (size !== Number(url.searchParams.get('bytes')) || ordered.length !== Number(url.searchParams.get('parts'))) throw new Error('Wrong multipart byte accounting');
        if (ordered.slice(0, -1).some(pair => pair[1].length !== 4096)) throw new Error('Non-final part had wrong size');
        recordings.find(item => item.id === id).status = 'QUEUED';
        // Simulate S3+DB completion succeeding just before the browser loses its response.
        if (!completeLost) { completeLost = true; return route.abort(); }
        return reply(recordings.find(item => item.id === id));
      }
    }
    if (path.endsWith('/start')) { currentStatus = 'LIVE'; return reply({}); }
    return reply({});
  });
  await page.addInitScript(user => {
    localStorage.setItem('lms-auth', JSON.stringify({ state: { user, isAuthenticated: true }, version: 0 }));
    // Observe the real warning handler, then suppress the native dialog only for automated reload.
    window.pilotUnloadHandlers = [];
    const addListener = window.addEventListener.bind(window);
    window.addEventListener = (type, listener, options) => {
      if (type === 'beforeunload') window.pilotUnloadHandlers.push(listener);
      return addListener(type, listener, options);
    };
    // Retain the real encoder output to compare recovery downloads byte-for-byte.
    window.pilotTrackBlobs = false;
    window.pilotRecordedBlobs = [];
    window.pilotDownloads = [];
    const recorderStart = MediaRecorder.prototype.start;
    MediaRecorder.prototype.start = function (...args) {
      if (window.pilotTrackBlobs) this.addEventListener('dataavailable', event => {
        if (event.data.size) window.pilotRecordedBlobs.push(event.data);
      });
      return recorderStart.apply(this, args);
    };
    const createURL = URL.createObjectURL.bind(URL);
    URL.createObjectURL = blob => {
      if (window.pilotTrackBlobs) window.pilotDownloads.push(blob);
      return createURL(blob);
    };
    const sources = [];
    const audio = frequency => {
      const context = new AudioContext(), oscillator = context.createOscillator(), destination = context.createMediaStreamDestination();
      oscillator.frequency.value = frequency; oscillator.connect(destination); oscillator.start(); sources.push(context);
      return destination.stream.getAudioTracks()[0];
    };
    window.pilotCaptureMode = 'normal';
    navigator.mediaDevices.getDisplayMedia = async () => {
      if (window.pilotCaptureMode === 'denied') throw new DOMException('Permission denied', 'NotAllowedError');
      const canvas = document.createElement('canvas'); canvas.width = 1280; canvas.height = 720;
      const ctx = canvas.getContext('2d'); let frame = 0;
      const paint = () => { ctx.fillStyle = '#1c3873'; ctx.fillRect(0,0,1280,720); ctx.fillStyle = '#fff'; ctx.font = '48px sans-serif'; ctx.fillText('Synthetic class capture ' + frame++, 100, 300); };
      paint(); setInterval(paint, 60);
      const stream = canvas.captureStream(15), track = stream.getVideoTracks()[0], original = track.getSettings.bind(track);
      track.getSettings = () => ({ ...original(), displaySurface: 'browser' });
      if (window.pilotCaptureMode !== 'no-audio') stream.addTrack(audio(440));
      window.pilotDisplayTrack = track;
      return stream;
    };
    navigator.mediaDevices.getUserMedia = async () => {
      if (window.pilotCaptureMode === 'mic-denied') throw new DOMException('Microphone permission denied', 'NotAllowedError');
      return new MediaStream([audio(880)]);
    };
  }, user);
  await page.goto('http://127.0.0.1:3100/instructor/live-classes');
  await page.getByRole('link', { name: 'Record class', exact: true }).waitFor();
  if (await page.evaluate(() => document.featurePolicy.allowsFeature('microphone'))) throw new Error('Capture policy unexpectedly enabled outside recorder');
  await page.screenshot({ path: '/tmp/elearning-recorder-test/live-classes.png', fullPage: true });
  await page.getByRole('link', { name: 'Record class', exact: true }).click();
  await page.getByRole('button', { name: 'Select Meet tab', exact: true }).waitFor();
  if (!await page.evaluate(() => document.featurePolicy.allowsFeature('microphone'))) throw new Error('Recorder navigation did not apply capture policy');
  const capabilities = await page.evaluate(() => ['video/mp4;codecs=avc1.42E01F,mp4a.40.2', 'video/mp4;codecs=avc1,mp4a.40.2'].map(type => [type, MediaRecorder.isTypeSupported(type)]));
  if (!capabilities.some(pair => pair[1])) throw new Error('Test Chrome lacks H.264/AAC encoding: ' + JSON.stringify(capabilities));
  const select = page.getByRole('button', { name: 'Select Meet tab', exact: true });
  for (const [mode, error] of [['no-audio', 'Share tab audio'], ['denied', 'Permission denied'], ['mic-denied', 'Microphone permission denied']]) {
    await page.evaluate(mode => { window.pilotCaptureMode = mode; }, mode);
    await select.click();
    await page.getByRole('alert').filter({ hasText: error }).waitFor();
    if (recordings.length) throw new Error('Created a session for failed capture');
    if (mode !== 'denied' && await page.evaluate(() => window.pilotDisplayTrack.readyState !== 'ended')) throw new Error('Display capture leaked after error');
  }
  await page.evaluate(() => { window.pilotCaptureMode = 'normal'; window.pilotMimeSupported = MediaRecorder.isTypeSupported; MediaRecorder.isTypeSupported = () => false; });
  await select.click();
  await page.getByRole('alert').filter({ hasText: 'cannot record H.264/AAC' }).waitFor();
  await page.evaluate(() => { MediaRecorder.isTypeSupported = window.pilotMimeSupported; });
  await select.click();
  await page.getByRole('button', { name: 'Start recording', exact: true }).waitFor();
  await page.getByRole('button', { name: 'Cancel selection', exact: true }).click();
  await select.click();
  await page.getByRole('button', { name: 'Start recording', exact: true }).waitFor();
  await page.getByRole('button', { name: 'Test audio sample', exact: true }).click();
  await page.getByRole('heading', { name: 'Audio sample: check your voice and the class audio' }).waitFor({ timeout: 15000 });
  await page.getByRole('button', { name: 'Start recording', exact: true }).click();
  await page.getByRole('button', { name: 'Stop and finish upload', exact: true }).waitFor();
  await page.waitForTimeout(6500);
  await page.getByRole('button', { name: 'Stop and finish upload', exact: true }).click();
  await page.getByRole('button', { name: 'Retry upload', exact: true }).waitFor({ timeout: 20000 });
  // First retry sends all parts, but its completion response is lost.
  await page.getByRole('button', { name: 'Retry upload', exact: true }).click();
  await page.getByText('Your local recording is kept.', { exact: false }).waitFor({ timeout: 20000 });
  const warned = await page.evaluate(() => {
    const event = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(event);
    window.pilotUnloadHandlers.forEach(listener => window.removeEventListener('beforeunload', listener));
    return event.defaultPrevented;
  });
  if (!warned) throw new Error('Missing unsaved recording warning');
  await page.reload();
  await page.getByRole('button', { name: 'Resume upload', exact: true }).waitFor();
  await page.getByRole('button', { name: 'Resume upload', exact: true }).click();
  await page.getByText('Uploaded — validating', { exact: true }).waitFor({ timeout: 20000 });
  await page.evaluate(() => document.querySelectorAll('*').forEach(element => { element.scrollTop = 0; }));
  await page.screenshot({ path: '/tmp/elearning-recorder-test/desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: '/tmp/elearning-recorder-test/mobile.png', fullPage: true });
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth);
  if (overflow || errors.length) throw new Error(JSON.stringify({ overflow, errors }));
  const parts = [...uploads.values()][0];
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Download local copy', exact: true }).click(),
  ]);
  await download.saveAs('/tmp/elearning-recorder-test/captured.mp4');
  const recoveryCases = [];
  const saveRecovery = async (button, expected, filename) => {
    const [saved] = await Promise.all([page.waitForEvent('download'), button.click()]);
    await saved.saveAs(`/tmp/elearning-recorder-test/${filename}.mp4`);
    const hash = await page.evaluate(async () => {
      const blob = window.pilotDownloads.at(-1);
      return [...new Uint8Array(await crypto.subtle.digest('SHA-256', await blob.arrayBuffer()))].map(byte => byte.toString(16).padStart(2, '0')).join('');
    });
    if (hash !== expected) throw new Error('Recovery download lost recording bytes: ' + filename);
  };
  // Exercise both a failure on the first chunk and after a durable prefix exists.
  for (const failAt of [1, 2]) {
    if (await page.getByRole('button', { name: 'Record another', exact: true }).count()) {
      await page.getByRole('button', { name: 'Record another', exact: true }).click();
    }
    await page.evaluate(failAt => {
      window.pilotTrackBlobs = true;
      window.pilotRecordedBlobs = [];
      window.pilotDownloads = [];
      let writes = 0;
      const put = IDBObjectStore.prototype.put;
      window.pilotRestoreWrites = () => { IDBObjectStore.prototype.put = put; };
      IDBObjectStore.prototype.put = function (value, key) {
        if (this.transaction.db.name === 'lms-class-recordings' && value instanceof Blob && ++writes >= failAt) {
          this.transaction.abort();
          throw new DOMException('Injected full IndexedDB', 'QuotaExceededError');
        }
        return put.call(this, value, key);
      };
    }, failAt);
    await select.click();
    await page.getByRole('button', { name: 'Start recording', exact: true }).click();
    await page.getByText('Recording stopped', { exact: true }).waitFor({ timeout: 20000 });
    await page.getByRole('alert').filter({ hasText: 'Browser storage failed' }).waitFor();
    if (recordings.at(-1).status !== 'UPLOADING') throw new Error('Storage-failed recording was published');
    const { bytes: expectedBytes, hash: expectedHash } = await page.evaluate(async () => {
      const blob = new Blob(window.pilotRecordedBlobs);
      return { bytes: blob.size, hash: [...new Uint8Array(await crypto.subtle.digest('SHA-256', await blob.arrayBuffer()))].map(byte => byte.toString(16).padStart(2, '0')).join('') };
    });
    if (!expectedBytes) throw new Error('Missing captured encoder bytes');
    const newestId = recordings.at(-1).id;
    const durable = await page.evaluate(async recordingId => {
      const db = await new Promise((resolve, reject) => {
        const request = indexedDB.open('lms-class-recordings');
        request.onsuccess = () => resolve(request.result); request.onerror = () => reject(request.error);
      });
      try {
        const store = db.transaction('recordings').objectStore('recordings');
        const read = request => new Promise((resolve, reject) => {
          request.onsuccess = () => resolve(request.result); request.onerror = () => reject(request.error);
        });
        const [rows, keys] = await Promise.all([read(store.getAll()), read(store.getAllKeys())]);
        const meta = rows.find(row => row.recordingId === recordingId);
        return rows.reduce((total, row, index) => total + (typeof keys[index] === 'string' && keys[index].startsWith(`chunk:${meta.localId}:`) ? row.size : 0), 0);
      } finally { db.close(); }
    }, newestId);
    if (durable >= expectedBytes || failAt === 1 && durable !== 0 || failAt === 2 && !durable) {
      throw new Error('Fault injection did not leave an unwritten recording suffix');
    }
    await saveRecovery(page.getByRole('button', { name: 'Download local copy', exact: true }), expectedHash, `recovery-${failAt}-before-reset`);
    await page.getByRole('button', { name: 'Record another', exact: true }).click();
    const row = page.locator(`[data-recording-id="${newestId}"]`);
    await row.getByText('Part of this copy is held only in this page. Download before closing it.', { exact: true }).waitFor();
    if (await row.getByRole('button', { name: 'Resume upload', exact: true }).count()) throw new Error('Offered IndexedDB-only recovery for volatile recording');
    await saveRecovery(row.getByRole('button', { name: 'Download', exact: true }), expectedHash, `recovery-${failAt}-after-reset`);
    const warned = await page.evaluate(() => { const event = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(event); return event.defaultPrevented; });
    if (!warned) throw new Error('Lost unsaved-data warning after replacing active recorder');
    await page.evaluate(() => window.pilotRestoreWrites());
    recoveryCases.push({ id: newestId, expectedHash, bytes: expectedBytes, durable });
  }
  // Starting another capture must not replace either older in-memory recovery copy.
  await select.click();
  await page.getByRole('button', { name: 'Cancel selection', exact: true }).click();
  for (const [index, saved] of recoveryCases.entries()) {
    await saveRecovery(page.locator(`[data-recording-id="${saved.id}"]`).getByRole('button', { name: 'Download', exact: true }), saved.expectedHash, `recovery-${index}-after-next-capture`);
  }
  if (errors.length) throw new Error(JSON.stringify(errors));
  return { capabilities, parts: parts.size, recovery: true, overflow, storageRecovery: recoveryCases, errors };
}

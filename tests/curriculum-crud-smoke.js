// Local production frontend; API calls are mocked. Backend persistence is covered by CurriculumCrudApiIt.
// playwright-cli -s=elearning-recorder run-code --filename=tests/curriculum-crud-smoke.js
async page => {
  await page.unrouteAll({ behavior: 'ignoreErrors' });
  page.removeAllListeners('pageerror');
  page.removeAllListeners('dialog');
  await page.addInitScript(() => { window.confirm = () => true; });
  const origin = 'http://127.0.0.1:3024', id = '11111111-1111-4111-8111-111111111111';
  let seq = 1, changes = [], errors = [], definitions = new Map(), slots = new Map();
  const uuid = () => `22222222-2222-4222-8222-${String(seq++).padStart(12, '0')}`;
  const course = { courseId: id, title: 'Curriculum verification', slug: 'curriculum-check', status: 'DRAFT', sections: [], instructors: [], credits: 3 };
  const items = () => course.sections.flatMap(s => s.items);
  const find = (type, key) => items().find(i => i.itemType === type && i.itemId === key);
  page.on('pageerror', e => errors.push(e.message));
  await page.route('**/*', async route => {
    const r = route.request(), url = new URL(r.url()), p = url.pathname, m = r.method();
    const reply = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', headers: { 'access-control-allow-origin': origin, 'access-control-allow-credentials': 'true', 'access-control-allow-headers': '*', 'access-control-allow-methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS' }, body: JSON.stringify(body) });
    if (m === 'OPTIONS') return reply({});
    if (p === '/api/auth/refresh') return reply({ accessToken: 'curriculum-ui-token' });
    if (p === '/me') return reply({ id: uuid(), fullName: 'Curriculum Admin', roles: ['ADMIN'] });
    if (r.resourceType() === 'document' || r.headers().rsc || r.headers()['next-router-prefetch']) return route.continue();
    if (p === `/admin/courses/${id}` && m === 'GET') return reply(course);
    const b = r.postData() ? JSON.parse(r.postData()) : {};
    if (m !== 'GET' && (p.startsWith('/admin/') || p.startsWith('/live-classes/'))) changes.push({ p, m, b });
    if (p === `/admin/courses/${id}/sections`) {
      const s = { ...b, sectionId: uuid(), status: 'DRAFT', items: [] }; course.sections.push(s); return reply(s);
    }
    let match = p.match(/^\/admin\/sections\/([^/]+)(?:\/(lessons|quizzes))?$/);
    if (match) {
      const s = course.sections.find(s => s.sectionId === match[1]);
      if (match[2]) {
        const quiz = match[2] === 'quizzes', key = uuid(), data = { ...b, sectionId: s.sectionId, status: 'DRAFT', [quiz ? 'quizId' : 'lessonId']: key, resources: [] };
        s.items.push({ itemId: key, itemType: quiz ? 'QUIZ' : 'LESSON', position: b.position, [quiz ? 'quiz' : 'lesson']: data });
        if (quiz) definitions.set(key, data); return reply(data);
      }
      if (m === 'DELETE') course.sections = course.sections.filter(v => v !== s); else Object.assign(s, b);
      return reply(s ?? {});
    }
    match = p.match(/^\/admin\/(lessons|quizzes)\/([^/]+)$/);
    if (match) {
      const quiz = match[1] === 'quizzes', i = find(quiz ? 'QUIZ' : 'LESSON', match[2]), data = i?.[quiz ? 'quiz' : 'lesson'];
      if (m === 'GET') return reply(quiz ? definitions.get(match[2]) : data);
      if (m === 'DELETE') course.sections.forEach(s => s.items = s.items.filter(v => v !== i));
      else { Object.assign(data, b); i.position = b.position ?? i.position; if (quiz) definitions.set(match[2], data); }
      return reply(data ?? {});
    }
    if (p === `/admin/courses/${id}/live-classes` && m === 'POST') {
      const key = uuid(), slot = { ...b, liveClassItemId: key, scheduled: false }; slots.set(key, slot);
      course.sections.find(s => s.sectionId === b.sectionId).items.push({ itemId: key, itemType: 'LIVE_CLASS', position: b.position, liveClass: { ...slot } }); return reply(slot);
    }
    match = p.match(/^\/admin\/courses\/[^/]+\/live-classes\/([^/]+)(\/schedule-with-url)?$/);
    if (match) {
      const key = match[1], slot = slots.get(key), i = find('LIVE_CLASS', key);
      if (match[2]) { Object.assign(i.liveClass, b, { liveClassId: uuid(), scheduled: true, status: 'SCHEDULED' }); return reply(i.liveClass); }
      if (m === 'GET') return reply(slot);
      if (m === 'DELETE') {
        if (i.liveClass.status === 'SCHEDULED') return reply({ detail: 'Cancel scheduled meetings before removing this item' }, 409);
        course.sections.forEach(s => s.items = s.items.filter(v => v !== i)); return reply({});
      }
      Object.assign(slot, b); Object.assign(i.liveClass, b); i.position = b.position; return reply(slot);
    }
    if (p.startsWith('/live-classes/')) {
      const i = items().find(i => i.liveClass?.liveClassId === p.split('/').pop());
      if (!i) throw Error('Wrong meeting ID in ' + p);
      Object.assign(i.liveClass, m === 'DELETE' ? { status: 'CANCELLED' } : b); return reply(i.liveClass);
    }
    if (p === `/admin/courses/${id}/structure/reorder`) {
      b.sections.forEach(s => { const section = course.sections.find(v => v.sectionId === s.sectionId); section.position = s.newPosition; s.items.forEach(v => { section.items.find(i => i.itemId === v.itemId).position = v.newPosition; }); section.items.sort((a, b) => a.position - b.position); }); return reply({});
    }
    if (p === '/public/settings') return reply([]);
    if (url.origin === origin) return route.continue();
    return route.abort();
  });
  await page.context().addCookies([{ name: 'session', value: '1', url: origin }, { name: 'locale', value: 'en', url: origin }, { name: 'user_roles', value: encodeURIComponent('["ADMIN"]'), url: origin }]);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(origin + '/admin/courses/' + id);
  await page.getByRole('tab', { name: 'Curriculum', exact: true }).click();
  const dialog = () => page.getByRole('dialog');
  const row = title => page.locator('li').filter({ has: page.getByText(title, { exact: true }) });
  async function save(label) { await dialog().getByRole('button', { name: label, exact: true }).click(); await dialog().waitFor({ state: 'hidden' }); }
  await page.getByRole('button', { name: 'Section', exact: true }).click();
  await dialog().getByLabel('Title (Bangla)', { exact: false }).fill('Section one'); await save('Create section');
  await page.getByRole('button', { name: 'Edit section', exact: true }).click();
  await dialog().getByLabel('Title (Bangla)', { exact: false }).fill('Edited section'); await save('Save changes');
  for (const type of ['VIDEO', 'PDF']) {
    await page.getByRole('button', { name: 'Add lesson', exact: true }).click();
    await dialog().getByLabel('Title (Bangla)', { exact: false }).fill(type + ' lesson');
    await dialog().getByRole('combobox', { name: 'Lesson type', exact: false }).selectOption(type);
    await save('Create lesson');
    await row(type + ' lesson').getByRole('button', { name: 'Edit', exact: true }).click();
    await dialog().getByLabel('Title (Bangla)', { exact: false }).fill(type + ' edited'); await save('Save changes');
  }
  await page.getByRole('button', { name: 'Add quiz', exact: true }).click();
  await dialog().getByLabel('Quiz title (Bangla)', { exact: false }).fill('Course quiz');
  await dialog().getByLabel('Question (Bangla)', { exact: false }).fill('Question one');
  for (let n = 1; n <= 2; n++) await dialog().getByLabel(`Option ${n} (Bangla)`, { exact: true }).fill('Answer ' + n);
  await save('Create quiz');
  await row('Course quiz').getByRole('button', { name: 'Questions', exact: true }).click();
  await dialog().getByLabel('Question (Bangla)', { exact: false }).fill('Revised question');
  await dialog().getByRole('button', { name: 'Add question', exact: true }).click();
  await dialog().getByRole('button', { name: 'Remove question 2', exact: true }).click();
  await save('Save quiz questions');
  if (![...definitions.values()].some(q => q.questions[0].questionText === 'Revised question')) throw Error('Questions not saved');
  await row('Course quiz').getByRole('button', { name: 'Edit', exact: true }).click();
  await dialog().getByLabel('Title (Bangla)', { exact: false }).fill('Edited quiz'); await save('Save changes');
  await page.getByRole('button', { name: 'Live class', exact: true }).click();
  await dialog().getByLabel('Title (Bangla)', { exact: true }).fill('Live discussion');
  await dialog().getByLabel('Position in section', { exact: true }).fill('4'); await save('Create live class');
  await row('Live discussion').getByRole('button', { name: 'Edit live class Live discussion', exact: true }).click();
  await dialog().getByLabel('Title (Bangla)', { exact: true }).fill('Edited discussion');
  await dialog().getByLabel('Description (Bangla)', { exact: true }).fill('Discussion notes'); await save('Save live class');
  const reordered = page.waitForResponse(r => r.url().endsWith('/structure/reorder') && r.request().method() === 'POST');
  await row('Edited discussion').getByRole('button', { name: 'Move item up', exact: true }).click();
  await reordered;
  if (!changes.some(c => c.p.endsWith('/structure/reorder') && c.b.sections[0].items.some(i => i.itemType === 'LIVE_CLASS' && i.newPosition === 3))) throw Error('Live class not reordered with lessons and quizzes');
  await row('Edited discussion').getByRole('button', { name: 'Schedule', exact: true }).click();
  await dialog().getByRole('combobox', { name: 'Meeting mode', exact: false }).selectOption('EXTERNAL_URL');
  await dialog().getByLabel('Start time', { exact: false }).fill('2030-01-01T10:00');
  await dialog().getByLabel('End time', { exact: false }).fill('2030-01-01T11:00');
  await dialog().getByLabel('Participant join URL', { exact: false }).fill('https://meet.example.com/class'); await save('Schedule live class');
  const slotId = [...slots.keys()][0], meetingId = find('LIVE_CLASS', slotId).liveClass.liveClassId;
  await row('Edited discussion').getByRole('button', { name: 'Reschedule', exact: true }).click();
  await dialog().getByLabel('End time', { exact: false }).fill('2030-01-01T11:30'); await save('Save schedule');
  if (!changes.some(c => c.m === 'PATCH' && c.p === '/live-classes/' + meetingId)) throw Error('Reschedule did not use actual meeting ID');
  const layouts = [];
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 900 });
    if (await page.locator('main').evaluate(el => el.scrollWidth > el.clientWidth)) throw Error('Curriculum overflows at ' + width);
    await page.screenshot({ path: `/tmp/gii-curriculum-${width}.png`, fullPage: true }); layouts.push(width);
  }
  await row('Edited discussion').getByRole('button', { name: 'Remove live class Edited discussion', exact: true }).click();
  await page.getByText('Cancel scheduled meetings before removing this item', { exact: true }).waitFor();
  await row('Edited discussion').getByRole('button', { name: 'Cancel meeting', exact: true }).click();
  await row('Edited discussion').getByRole('button', { name: 'Cancel meeting', exact: true }).waitFor({ state: 'hidden' });
  await row('Edited discussion').getByRole('button', { name: 'Remove live class Edited discussion', exact: true }).click();
  await row('Edited discussion').waitFor({ state: 'hidden' });
  if (!changes.some(c => c.m === 'DELETE' && c.p.endsWith('/live-classes/' + slotId))) throw Error('Remove used meeting ID rather than slot ID');
  for (const title of ['VIDEO edited', 'PDF edited', 'Edited quiz']) { await row(title).getByRole('button', { name: 'Delete', exact: true }).click(); await row(title).waitFor({ state: 'hidden' }); }
  await page.getByRole('button', { name: 'Delete section', exact: true }).click();
  await page.getByRole('heading', { name: 'No sections yet', exact: true }).waitFor();
  if (course.sections.length || errors.length) throw Error(JSON.stringify({ sections: course.sections.length, errors }));
  return { mutations: changes.length, checked: ['sections', 'video', 'pdf', 'quiz settings and questions', 'live details, ordering, schedule, reschedule, cancel and removal'], layouts, browserErrors: errors };
}

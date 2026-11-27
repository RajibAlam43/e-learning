// Run with a local Next.js server on port 3024, after creating /tmp/gii-academic-ui.
// playwright-cli -s=elearning-recorder run-code --filename=tests/academic-foundation-smoke.js
// API calls are intercepted; this never changes a live environment.
async page => {
  const origin = 'http://127.0.0.1:3024';
  const user = { id: '11111111-1111-4111-8111-111111111111', fullName: 'Academic Admin', email: 'admin@example.com', roles: ['ADMIN'] };
  const courseId = '22222222-2222-4222-8222-222222222222';
  const nextId = '33333333-3333-4333-8333-333333333333';
  const semesterId = '44444444-4444-4444-8444-444444444444';
  const semesters = [], offerings = [], mutations = [], errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.context().addCookies([
    { name: 'session', value: '1', url: origin },
    { name: 'locale', value: 'en', url: origin },
    { name: 'user_roles', value: encodeURIComponent(JSON.stringify(['ADMIN'])), url: origin },
  ]);
  const course = { courseId, title: 'Arabic 1', slug: 'arabic-1-original', courseTemplateId: '55555555-5555-4555-8555-555555555555', credits: 3,
    status: 'DRAFT', studyMode: 'COHORT_BASED', priceBdt: 1500, isFree: false, timezone: 'Asia/Dhaka',
    categories: [], sections: [], instructors: [], createdAt: '2026-11-01T00:00:00Z' };
  await page.route('**/*', async route => {
    const request = route.request(), url = new URL(request.url()), path = url.pathname;
    const method = request.method();
    const reply = (body, status = 200) => route.fulfill({ status, contentType: 'application/json',
      headers: { 'access-control-allow-origin': origin, 'access-control-allow-credentials': 'true',
        'access-control-allow-headers': '*', 'access-control-allow-methods': 'GET,POST,PUT,PATCH,OPTIONS' }, body: JSON.stringify(body) });
    if (method === 'OPTIONS') return reply({});
    if (path === '/api/auth/refresh') return reply({ accessToken: 'academic-ui-test-token' });
    if (path === '/me') return reply(user);
    if (request.resourceType() === 'document' || request.headers().rsc || request.headers()['next-router-prefetch']) return route.continue();
    if (path === '/admin/semesters') {
      if (method === 'GET') return reply(semesters);
      const body = JSON.parse(request.postData()); mutations.push({ path, body });
      if (semesters.some(item => item.code === body.code)) return reply({ detail: 'Semester code is already in use' }, 409);
      semesters.push({ ...body, semesterId, status: 'PLANNED' });
      return reply(semesters[0], 201);
    }
    if (path === '/admin/semesters/' + semesterId && method === 'PUT') {
      const body = JSON.parse(request.postData()); mutations.push({ path, body });
      semesters[0] = { ...body, semesterId, status: 'PLANNED' }; return reply(semesters[0]);
    }
    if (path === '/admin/courses/' + courseId + '/offerings') {
      if (method === 'GET') return reply([{ ...course, semester: semesters[0] }, ...offerings]);
      const body = JSON.parse(request.postData()); mutations.push({ path, body });
      if (body.semesterId !== semesterId || body.studyMode !== 'COHORT_BASED' || body.capacity !== 20) throw new Error('Incorrect cohort request');
      offerings.push({ ...course, ...body, courseId: nextId, semester: semesters[0] });
      return reply(offerings[0], 201);
    }
    if (path === '/admin/courses/' + courseId) return reply({ ...course, semester: semesters[0] });
    if (path === '/admin/courses/' + nextId) return reply(offerings[0]);
    if (path === '/admin/categories') return reply([{ id: '66666666-6666-4666-8666-666666666666', name: 'General', nameEn: 'General' }]);
    if (path === '/public/settings' || path === '/admin/instructors' || path === '/admin/courses') return reply([]);
    if (path.startsWith('/public/courses/')) return reply({ detail: 'Course not found' }, 404);
    if (url.origin === origin) return route.continue();
    return route.abort();
  });

  await page.goto(origin + '/admin/semesters');
  await page.getByRole('heading', { name: 'Semesters', exact: true }).waitFor();
  await page.getByLabel('Name', { exact: true }).fill('Spring 2027');
  await page.getByLabel('Code', { exact: true }).fill('SPRING-2027');
  for (const [label, value] of [['Teaching starts','2027-01-01T08:00'], ['Teaching ends','2027-06-30T22:00'], ['Registration opens','2026-12-01T08:00'], ['Registration closes','2027-01-15T22:00']]) {
    await page.getByLabel(label, { exact: true }).fill(value);
  }
  await page.getByRole('button', { name: 'Create semester', exact: true }).click();
  await page.getByRole('heading', { name: 'Spring 2027 (SPRING-2027)', exact: true }).waitFor();
  if (new Date(mutations[0].body.startsAt).toISOString() !== '2027-01-01T02:00:00.000Z') throw new Error('Semester timezone conversion failed');
  await page.getByRole('button', { name: 'Edit Spring 2027', exact: true }).click();
  await page.getByLabel('Name', { exact: true }).fill('Spring 2027 revised');
  await page.getByRole('button', { name: 'Save changes', exact: true }).click();
  await page.getByRole('heading', { name: 'Spring 2027 revised (SPRING-2027)', exact: true }).waitFor();
  const layouts = [];
  for (const width of [1280,390]) {
    await page.setViewportSize({ width, height: 900 });
    const overflow = await page.locator('main').evaluate(main => main.scrollWidth > main.clientWidth);
    if (overflow) throw new Error('Semester content overflows at ' + width);
    await page.screenshot({ path: '/tmp/gii-academic-ui/semesters-' + width + '.png' });
    layouts.push({ screen: 'semesters', width, overflow });
  }
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(origin + '/admin/courses/' + courseId);
  await page.getByRole('tab', { name: 'Cohorts', exact: true }).click();
  await page.getByRole('heading', { name: 'Add cohort', exact: true }).waitFor();
  await page.getByLabel('Semester', { exact: false }).selectOption(semesterId);
  await page.getByLabel('Unique cohort slug', { exact: true }).fill('arabic-1-spring-2027-b');
  await page.getByLabel('Capacity', { exact: false }).fill('20');
  await page.getByRole('button', { name: 'Create cohort', exact: true }).click();
  await page.getByRole('link', { name: 'arabic-1-spring-2027-b', exact: true }).waitFor();
  if (offerings.length !== 1) throw new Error('Expected one cohort creation');
  for (const width of [1280,390]) {
    await page.setViewportSize({ width, height: 900 });
    const overflow = await page.locator('main').evaluate(main => main.scrollWidth > main.clientWidth);
    if (overflow) throw new Error('Cohort content overflows at ' + width);
    await page.screenshot({ path: '/tmp/gii-academic-ui/cohorts-' + width + '.png' });
    layouts.push({ screen: 'cohorts', width, overflow });
  }
  await page.goto(origin + '/admin/courses/create');
  await page.getByLabel('Course credits', { exact: false }).waitFor();
  if (await page.getByLabel('Course credits', { exact: false }).inputValue() !== '3') throw new Error('Credits must default to 3');
  await page.getByLabel('Course credits', { exact: false }).fill('4');
  await page.getByLabel('Semester', { exact: false }).selectOption(semesterId);
  if (errors.length) throw new Error(errors.join('\n'));
  return { mutations: mutations.map(item => ({ path: item.path, semesterId: item.body.semesterId })), layouts, defaultCredits: 3, browserErrors: errors };
}

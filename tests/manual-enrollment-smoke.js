// All API calls are mocked. Run against a local production build on port 3024.
// playwright-cli -s=elearning-recorder run-code --filename=tests/manual-enrollment-smoke.js
async page => {
  page.setDefaultTimeout(15000);
  await page.unrouteAll({ behavior: 'wait' });
  const origin = 'http://127.0.0.1:3024', studentId = '22222222-2222-4222-8222-222222222222';
  const courseId = '33333333-3333-4333-8333-333333333333', programId = '11111111-1111-4111-8111-111111111111';
  const base = `/admin/students/${studentId}/manual-enrollments`;
  const history = [], mutations = [], errors = [], layouts = [];
  let rejectOnce = true, hasProgram = false;
  page.on('pageerror', e => errors.push(e.message));
  await page.route('**/*', async route => {
    const req = route.request(), url = new URL(req.url()), path = url.pathname;
    const reply = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) });
    if (path === '/api/auth/refresh') return reply({ accessToken: 'manual-enrollment-test' });
    if (path === '/me') return reply({ id: '77777777-7777-4777-8777-777777777777', fullName: 'Academic Admin', roles: ['ADMIN'] });
    if (req.resourceType() === 'document' || req.headers().rsc || req.headers()['next-router-prefetch']) return route.continue();
    if (path === '/admin/courses') return reply([{ courseId, title: 'Islamic Studies', slug: 'islamic-studies-spring-2027', status: 'PUBLISHED', credits: 3 }]);
    if (path === '/admin/collections') return reply([{ collectionId: programId, title: 'Islamic Studies Diploma', status: 'PUBLISHED' }]);
    if (path === base) {
      if (req.method() === 'GET') return reply({ userId: studentId, fullName: 'Fatima Ahmed', email: null, phone: '+8801700000000',
        programs: hasProgram ? [{ programId, title: 'Islamic Studies Diploma', active: true, expiresAt: '2029-11-26T15:00:00Z' }] : [], history });
      const body = JSON.parse(req.postData()); mutations.push(body);
      if (rejectOnce) { rejectOnce = false; return reply({ detail: 'Complete the required courses or obtain a credit waiver: Arabic Foundation' }, 409); }
      if (body.programId && !body.programFunded) hasProgram = true;
      const result = { ...body, id: `audit-${history.length}`, changed: true, actorName: 'Academic Admin',
        courseTitle: body.courseId ? 'Islamic Studies · Spring 2027' : null,
        programTitle: body.programId ? 'Islamic Studies Diploma' : null,
        accessExpiresAt: body.expiresAt || '2029-11-26T15:00:00Z', requestedExpiresAt: body.expiresAt,
        createdAt: '2026-11-26T15:00:00Z' };
      history.unshift(result); return reply(result);
    }
    if (path === `/admin/students/${studentId}/program-registrations`) return reply([]);
    if (path === '/public/settings') return reply([]);
    return route.continue();
  });
  await page.context().addCookies([{ name: 'session', value: '1', url: origin }, { name: 'locale', value: 'en', url: origin },
    { name: 'user_roles', value: encodeURIComponent(JSON.stringify(['ADMIN'])), url: origin }]);
  await page.goto(origin + `/admin/students/${studentId}/enrollments`);
  const form = page.getByRole('form', { name: 'Manual enrollment', exact: true });
  await page.getByRole('heading', { name: 'Fatima Ahmed', exact: true }).waitFor();
  await page.getByText('+8801700000000', { exact: true }).waitFor();
  if (!await form.getByRole('button', { name: 'Save enrollment', exact: true }).isDisabled()) throw Error('Empty reason must disable submit');
  await form.getByRole('combobox', { name: 'Course offering', exact: true }).selectOption(courseId);
  await form.getByLabel('Enrollment reason', { exact: true }).fill('Admissions approved after external study review');
  await form.getByLabel('Access expiry (optional, your local time)', { exact: false }).fill('2031-01-15T10:30');
  await page.setViewportSize({ width: 390, height: 900 });
  await form.getByLabel('Access expiry (optional, your local time)', { exact: false }).scrollIntoViewIfNeeded();
  if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw Error('Custom expiry field overflows on mobile');
  await page.screenshot({ path: '/tmp/gii-step7-ui/manual-expiry-390.png', animations: 'disabled' });
  await page.setViewportSize({ width: 1280, height: 900 });
  await form.getByLabel('Override course capacity', { exact: true }).check();
  await form.getByRole('button', { name: 'Save enrollment', exact: true }).click();
  await page.getByRole('alert').filter({ hasText: 'Arabic Foundation' }).waitFor();
  await page.getByRole('link', { name: 'credit waiver', exact: true }).waitFor();
  await form.getByRole('button', { name: 'Save enrollment', exact: true }).click();
  await page.getByRole('status').filter({ hasText: 'Enrollment saved' }).waitFor();
  if (mutations[0].requestId !== mutations[1].requestId) throw Error('Unchanged retry must reuse request ID');
  if (!mutations[1].expiresAt || new Date(mutations[1].expiresAt).getFullYear() !== 2031) throw Error('Custom expiry missing from request');
  if (await form.getByLabel('Access expiry (optional, your local time)', { exact: false }).inputValue()) throw Error('Successful grant must reset the expiry field');
  await page.getByRole('region', { name: 'Manual enrollment history', exact: true }).getByText('Overrides: capacity', { exact: true }).waitFor();
  if (await form.getByLabel('Override course capacity', { exact: true }).isChecked()) throw Error('Successful action must reset exceptions');
  await form.getByRole('combobox', { name: 'Enrollment type', exact: true }).selectOption('program');
  await form.getByRole('combobox', { name: 'Program', exact: true }).selectOption(programId);
  await form.getByLabel('Enrollment reason', { exact: true }).fill('Scholarship program admission');
  if (await form.getByLabel('Override course capacity', { exact: true }).count()) throw Error('Program entitlement cannot override course seats');
  await form.getByRole('button', { name: 'Save enrollment', exact: true }).click();
  await page.getByRole('region', { name: 'Manual enrollment history', exact: true }).getByText('Scholarship program admission', { exact: true }).waitFor();
  await form.getByRole('combobox', { name: 'Enrollment type', exact: true }).selectOption('registration');
  await form.getByRole('combobox', { name: 'Program', exact: true }).selectOption(programId);
  await form.getByRole('combobox', { name: 'Course offering', exact: true }).selectOption(courseId);
  await form.getByLabel('Enrollment reason', { exact: true }).fill('Approved late semester registration');
  await form.getByLabel('Override semester and course registration dates', { exact: true }).check();
  await page.setViewportSize({ width: 390, height: 900 });
  await form.getByRole('button', { name: 'Save enrollment', exact: true }).click();
  await page.getByRole('region', { name: 'Manual enrollment history', exact: true }).getByText('Approved late semester registration', { exact: true }).waitFor();
  const last = mutations.at(-1);
  if (!last.programFunded || !last.overrideDates || last.overrideCapacity || last.allowUnpublished) throw Error('Incorrect funded registration overrides');
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 900 });
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth);
    if (overflow) throw Error('Manual enrollment overflow at ' + width);
    await page.screenshot({ path: `/tmp/gii-step5-ui/manual-${width}.png`, fullPage: true, animations: 'disabled' });
    layouts.push({ width, overflow });
  }
  await page.getByRole('link', { name: 'Program registrations', exact: true }).click();
  await page.getByRole('heading', { name: 'Program registrations', exact: true }).waitFor();
  await page.getByRole('link', { name: 'Manual enrollment', exact: true }).click();
  await page.getByRole('heading', { name: 'Manual enrollment', exact: true }).waitFor();
  if (errors.length) throw Error(errors.join('\n'));
  return { mutations, auditCount: history.length, layouts, browserErrors: errors };
}

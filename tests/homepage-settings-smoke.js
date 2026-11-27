// Local frontend checks with isolated API fixtures; no hosted data is changed.
// playwright-cli -s=elearning-recorder run-code --filename=tests/homepage-settings-smoke.js
async page => {
  const origin = 'http://127.0.0.1:3024', id = '55555555-5555-4555-8555-555555555555';
  const defaults = { showCoursesCount: true, showStudentsCount: true, showScholarsCount: true, showDiplomasCount: true, partners: [] };
  let config = structuredClone(defaults), regular = true, featured = true, programs = true, failFeatured = false, puts = [], errors = [];
  const course = { id, title: 'Regular course fixture', slug: 'regular-fixture', priceBdt: 100, level: 'BEGINNER' };
  page.on('pageerror', e => errors.push(e.message));
  await page.route('**/*', async route => {
    const r = route.request(), u = new URL(r.url()), p = u.pathname, m = r.method();
    const reply = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', headers: { 'access-control-allow-origin': origin, 'access-control-allow-credentials': 'true', 'access-control-allow-headers': 'Authorization, Content-Type, Accept-Language', 'access-control-allow-methods': 'GET,POST,PUT,OPTIONS' }, body: JSON.stringify(body) });
    if (m === 'OPTIONS') return reply({});
    if (p === '/api/auth/refresh') return reply({ accessToken: 'homepage-test', roles: ['ADMIN'] });
    if (p === '/me') return reply({ userId: id, fullName: 'Homepage Test Admin', roles: ['ADMIN'] });
    if (r.resourceType() === 'document' || r.headers().rsc || r.headers()['next-router-prefetch']) return route.continue();
    if (p === '/admin/settings' || p === '/public/settings') return reply([{ key: 'site.homepage', value: config, isPublic: true }]);
    if (p === '/admin/settings/site.homepage' && m === 'PUT') { const b = JSON.parse(r.postData()); if (!b.isPublic) throw Error('Homepage visibility must be readable publicly'); config = b.value; puts.push(structuredClone(config)); return reply({ key: 'site.homepage', value: config, isPublic: true }); }
    if (p === '/public/stats') return reply({ courses: 1, students: 12, instructors: 2, programs: 1 });
    if (p === '/public/courses/featured') return reply(failFeatured ? { detail: 'Fixture unavailable' } : featured ? [{ ...course, id: '66666666-6666-4666-8666-666666666666', title: 'Special course fixture', slug: 'special-fixture' }] : [], failFeatured ? 502 : 200);
    if (p === '/public/courses') return reply({ content: regular ? [course] : [], totalElements: regular ? 1 : 0, totalPages: regular ? 1 : 0 });
    if (p === '/public/collections') return reply({ content: programs ? [{ id, slug: 'program-fixture', title: 'Program fixture', collectionType: 'DIPLOMA' }] : [], totalElements: programs ? 1 : 0, totalPages: programs ? 1 : 0 });
    if (p === '/public/reviews') return reply({ content: [] });
    if (p.startsWith('/public/faculty') || p === '/public/instructors') return reply([]);
    if (u.origin === origin) return route.continue();
    return route.abort();
  });
  await page.context().addCookies([{ name: 'session', value: '1', url: origin }, { name: 'locale', value: 'en', url: origin }, { name: 'user_roles', value: encodeURIComponent('["ADMIN"]'), url: origin }]);
  await page.setViewportSize({ width: 1280, height: 900 });
  const stats = () => page.locator('section[aria-label="Platform statistics"]');
  async function home() { const loaded = page.waitForResponse(r => new URL(r.url()).pathname === '/public/settings'); await page.goto(origin + '/'); await loaded; await page.getByRole('heading', { name: 'Why Learn With Us?', exact: true }).waitFor(); }
  async function settings() { await page.goto(origin + '/admin/settings'); await page.getByRole('tab', { name: 'Homepage', exact: true }).click(); await page.getByRole('switch', { name: 'Show Courses count', exact: true }).waitFor(); }
  async function save() { const saved = page.waitForResponse(r => new URL(r.url()).pathname === '/admin/settings/site.homepage' && r.request().method() === 'PUT'); await page.getByRole('button', { name: 'Save changes', exact: true }).click(); await saved; await page.getByText('Saved', { exact: true }).waitFor(); }
  await home();
  await page.getByRole('heading', { name: 'Regular course fixture', exact: true }).waitFor();
  const regularSection = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Courses', exact: true }) });
  const specialSection = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Special Courses', exact: true }) });
  if (!await regularSection.getByRole('heading', { name: 'Regular course fixture', exact: true }).count()) throw Error('Courses is using the featured feed');
  await specialSection.getByRole('heading', { name: 'Special course fixture', exact: true }).waitFor();
  if (!await specialSection.getByRole('heading', { name: 'Special course fixture', exact: true }).count()) throw Error('Special Courses is using the regular feed');
  await stats().waitFor(); if (await stats().locator('article').count() !== 4) throw Error('Default stats should show four real counts');
  if (await page.getByRole('heading', { name: 'Our Partners', exact: true }).count()) throw Error('Empty partners section is visible');
  const toggles = [['showCoursesCount', 'Show Courses count', 'Courses'], ['showStudentsCount', 'Show Students count', 'Students'], ['showScholarsCount', 'Show Scholars count', 'Scholars'], ['showDiplomasCount', 'Show Diplomas count', 'Diplomas']];
  for (const [key, label, card] of toggles) {
    config = structuredClone(defaults); await settings(); await page.getByRole('switch', { name: label, exact: true }).uncheck(); await save();
    if (config[key] !== false || Object.entries(config).filter(([k]) => k !== key && k !== 'partners').some(([, v]) => v !== true)) throw Error('Stat switches are not independent');
    await home(); await stats().waitFor(); if (await stats().locator('article').count() !== 3 || await stats().getByText(card, { exact: true }).count()) throw Error('Wrong stat card hidden: ' + card);
  }
  await settings();
  for (const [, label] of toggles) await page.getByRole('switch', { name: label, exact: true }).uncheck();
  await save(); await home();
  if (await stats().count()) throw Error('All-off stats left an empty strip');
  await settings(); await page.getByRole('switch', { name: 'Show Students count', exact: true }).check();
  await page.getByRole('button', { name: 'Add partner', exact: true }).click();
  await page.getByLabel('Partner name', { exact: true }).fill('Partner fixture'); await page.getByLabel('Logo URL', { exact: true }).fill('/images/logo1.png'); await page.getByLabel('Website URL (optional)', { exact: true }).fill('javascript:invalid');
  const beforeInvalid = puts.length; await page.getByRole('button', { name: 'Save changes', exact: true }).click(); await page.getByRole('alert').filter({ hasText: 'valid optional HTTPS website' }).waitFor(); if (puts.length !== beforeInvalid) throw Error('Invalid partner was saved');
  await page.getByLabel('Website URL (optional)', { exact: true }).fill('https://example.com'); await save(); await settings();
  if (await page.getByLabel('Partner name', { exact: true }).inputValue() !== 'Partner fixture') throw Error('Partner settings did not survive navigation');
  await home(); await page.getByRole('heading', { name: 'Our Partners', exact: true }).waitFor(); if (await stats().locator('article').count() !== 1) throw Error('One enabled stat should fill the strip');
  await page.getByRole('img', { name: 'Partner fixture', exact: true }).waitFor();
  const layouts = [];
  for (const width of [1280, 390]) { await page.setViewportSize({ width, height: 900 }); if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw Error('Homepage overflow at ' + width); await settings(); if (await page.locator('main').evaluate(el => el.scrollWidth > el.clientWidth)) throw Error('Homepage settings overflow at ' + width); await page.screenshot({ path: `/tmp/gii-homepage-settings-${width}.png`, fullPage: true }); layouts.push(width); }
  await page.getByRole('button', { name: 'Remove partner 1', exact: true }).click(); await save(); await home(); if (await page.getByRole('heading', { name: 'Our Partners', exact: true }).count()) throw Error('Removing final partner did not hide the section');
  regular = false; featured = false; programs = false; await page.reload();
  for (const name of ['Courses', 'Programs', 'Special Courses']) { await page.getByRole('heading', { name, exact: true }).waitFor({ state: 'hidden' }); }
  failFeatured = true; await page.reload(); await page.getByRole('alert').filter({ hasText: 'Special courses could not be loaded' }).waitFor({ timeout: 20000 });
  if (errors.length) throw Error(errors.join('\n'));
  return { independentSwitches: 4, saves: puts.length, checked: ['feed mapping', 'all-off and single-card layouts', 'partner validation and persistence', 'removing the final partner', 'empty sections hidden', 'errors remain recoverable'], layouts, browserErrors: errors };
}

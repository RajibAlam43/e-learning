// Isolated fixtures; never writes to the API or changes the teacher preview.
// playwright-cli -s=elearning-recorder run-code --filename=tests/homepage-social-proof-smoke.js
async page => {
  const origin = 'http://127.0.0.1:3024', errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const cases = [[0, 'Join us'], [999, 'Join us'], [1000, 'Join 1k+ learners'], [1999, 'Join 1k+ learners'], [2000, 'Join 2k+ learners'], [3000, 'Join 3k+ learners'], [10999, 'Join 10k+ learners'], [null, 'Join us']];
  for (const [students, label] of cases) {
    const context = await page.context().browser().newContext({ viewport: { width: 1280, height: 900 } });
    await context.addCookies([{ name: 'locale', value: 'en', url: origin }]);
    const fixture = await context.newPage();
    fixture.on('pageerror', error => errors.push(error.message));
    await fixture.route('**/*', async route => {
      const request = route.request(), url = new URL(request.url()), path = url.pathname;
      const reply = body => route.fulfill({ contentType: 'application/json', headers: { 'access-control-allow-origin': origin, 'access-control-allow-credentials': 'true', 'access-control-allow-headers': 'Authorization, Content-Type, Accept-Language' }, body: JSON.stringify(body) });
      if (request.method() === 'OPTIONS') return reply({});
      if (request.resourceType() === 'document' || request.headers().rsc || request.headers()['next-router-prefetch']) return route.continue();
      if (path === '/api/auth/refresh') return reply({ accessToken: 'fixture', roles: [] });
      if (path === '/me') return reply({ roles: [] });
      if (path === '/public/stats') return reply(students === null ? {} : { students, courses: 0, instructors: 0, programs: 0 });
      if (path === '/public/settings' || path.startsWith('/public/faculty') || path === '/public/instructors' || path === '/public/courses/featured') return reply([]);
      if (path === '/public/reviews') return reply({ content: students === 3000 ? [{ reviewId: 'written-review', reviewText: 'Helpful lessons from my course.', studentName: 'Review fixture', rating: 3 }] : [{ reviewId: 'blank', reviewText: '   ', rating: 5 }] });
      if (path === '/public/courses' || path === '/public/collections') return reply({ content: [], totalElements: 0 });
      if (url.origin === origin) return route.continue();
      return route.abort();
    });
    await fixture.goto(origin);
    await fixture.getByText('Trusted by students worldwide', { exact: true }).waitFor();
    const section = fixture.getByRole('region', { name: 'Trusted by devoted Muslims', exact: true });
    await section.waitFor();
    await section.getByRole('link', { name: label, exact: true }).waitFor();
    if (await section.getByRole('link', { name: label, exact: true }).getAttribute('href') !== '/signup') throw Error('Join link has wrong destination');
    const carousel = section.getByRole('region', { name: 'Student testimonials', exact: true });
    if (students === 3000) {
      await carousel.waitFor();
      await carousel.getByText('Helpful lessons from my course.', { exact: true }).first().waitFor();
      await carousel.getByRole('img', { name: '3 out of 5 stars', exact: true }).first().waitFor();
    } else if (await carousel.count()) throw Error('Empty written reviews rendered a carousel');
    await fixture.getByRole('heading', { name: 'Your Learning Journey', exact: true }).waitFor();
    for (const width of [1280, 390]) {
      await fixture.setViewportSize({ width, height: 900 });
      if (await fixture.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw Error('Horizontal overflow at ' + width);
    }
    await context.close();
  }
  if (errors.length) throw Error(JSON.stringify(errors));
  return { cases: cases.length, layouts: [1280, 390], errors };
}

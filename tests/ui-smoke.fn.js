async (page) => {
  const routes = [
    ['/dashboard', '欢迎使用OA办公系统'],
    ['/oa/announcement', '公告标题'],
    ['/oa/schedule', '日程标题'],
    ['/oa/leave', '请假类型'],
    ['/oa/reimbursement', '报销类型'],
    ['/oa/incoming-doc', '收文标题'],
    ['/oa/outgoing-doc', '发文标题'],
    ['/oa/doc-flow', '流程名称'],
    ['/oa/todo', '任务标题'],
    ['/oa/notice', '公告标题'],
    ['/system/user', '用户名称'],
    ['/system/role', '角色名称'],
    ['/system/dept', '部门名称']
  ];
  const errors = [];
  const onConsole = msg => {
    if (msg.type() === 'error') errors.push(`console: ${msg.text()}`);
  };
  const onPageError = err => errors.push(`pageerror: ${err.message}`);
  page.on('console', onConsole);
  page.on('pageerror', onPageError);
  const results = [];
  for (const [route, expected] of routes) {
    try {
      await page.goto(`http://localhost:18081${route}`, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await page.waitForTimeout(800);
      const body = await page.locator('body').innerText({ timeout: 5000 });
      results.push({ route, title: await page.title(), ok: body.includes(expected), expected, url: page.url() });
    } catch (error) {
      results.push({ route, ok: false, error: error.message, url: page.url() });
    }
  }
  page.off('console', onConsole);
  page.off('pageerror', onPageError);
  return { results, errors };
}
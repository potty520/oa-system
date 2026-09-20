async (page) => {
  const errors = [];
  const onConsole = msg => {
    if (msg.type() === 'error' && !msg.text().includes('favicon.ico')) {
      errors.push(`console: ${msg.text()}`);
    }
  };
  const onPageError = err => errors.push(`pageerror: ${err.message}`);
  page.on('console', onConsole);
  page.on('pageerror', onPageError);

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

  await page.goto('http://localhost:8081/login', { waitUntil: 'domcontentloaded', timeout: 20000 });
  await page.getByRole('textbox', { name: '用户名' }).fill('admin');
  await page.getByRole('textbox', { name: '密码' }).fill('admin123');
  await page.getByRole('button', { name: '登 录' }).click();
  await page.waitForURL('**/dashboard', { timeout: 20000 });
  const loggedIn = (await page.locator('body').innerText()).includes('欢迎使用OA办公系统');

  const routeResults = [];
  for (const [route, expected] of routes) {
    try {
      await page.goto(`http://localhost:8081${route}`, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await page.waitForTimeout(600);
      const body = await page.locator('body').innerText({ timeout: 5000 });
      routeResults.push({ route, ok: body.includes(expected), url: page.url() });
    } catch (error) {
      routeResults.push({ route, ok: false, error: error.message, url: page.url() });
    }
  }

  const title = `UI功能测试公告${Date.now()}`;
  let announcementCreated = false;
  let announcementDeleted = false;
  await page.goto('http://localhost:8081/oa/announcement', { waitUntil: 'domcontentloaded', timeout: 20000 });
  await page.getByRole('button', { name: /新增/ }).click();
  const dialog = page.locator('.el-dialog:visible');
  await dialog.getByPlaceholder('请输入公告标题').fill(title);
  await dialog.getByPlaceholder('请输入公告内容').fill('Playwright 功能测试');
  await dialog.getByRole('button', { name: /确 定/ }).click();
  await page.waitForTimeout(1000);
  const row = page.locator('.el-table__row').filter({ hasText: title });
  announcementCreated = (await row.count()) === 1;
  if (announcementCreated) {
    await row.getByRole('button', { name: /删除/ }).click();
    await page.locator('.el-message-box:visible').getByRole('button', { name: '确定' }).click();
    await page.waitForTimeout(1000);
    announcementDeleted = (await page.getByText(title, { exact: true }).count()) === 0;
  }

  page.off('console', onConsole);
  page.off('pageerror', onPageError);
  return { loggedIn, routeResults, announcementCreated, announcementDeleted, errors };
}
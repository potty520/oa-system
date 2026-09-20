async (page) => {
  const title = `UI自动化公告${Date.now()}`;
  const errors = [];
  const onConsole = msg => { if (msg.type() === 'error') errors.push(msg.text()); };
  const onPageError = err => errors.push(err.message);
  page.on('console', onConsole);
  page.on('pageerror', onPageError);
  await page.goto('http://localhost:18081/oa/announcement', { waitUntil: 'domcontentloaded', timeout: 20000 });
  await page.getByRole('button', { name: /新增/ }).click();
  const dialog = page.locator('.el-dialog:visible');
  await dialog.getByPlaceholder('请输入公告标题').fill(title);
  await dialog.getByPlaceholder('请输入公告内容').fill('Playwright 页面自动化测试');
  await dialog.getByRole('button', { name: /确 定/ }).click();
  await page.waitForTimeout(1200);
  const row = page.locator('.el-table__row').filter({ hasText: title });
  const created = (await row.count()) === 1;
  let deleted = false;
  if (created) {
    await row.getByRole('button', { name: /删除/ }).click();
    await page.locator('.el-message-box:visible').getByRole('button', { name: '确定' }).click();
    await page.waitForTimeout(1200);
    deleted = (await page.getByText(title, { exact: true }).count()) === 0;
  }
  page.off('console', onConsole);
  page.off('pageerror', onPageError);
  return { title, created, deleted, errors };
}
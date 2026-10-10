// UI 功能测试运行器：执行本目录下的 ui-*.fn.js。
//
// 这三个脚本是 `async (page) => {...}` 形式的表达式（不是模块），因此这里用 eval 取出函数。
// 其中 ui-functional.fn.js 会自己走登录表单并处理"首次登录强制改密"弹窗；
// ui-smoke.fn.js 与 ui-announcement-crud.fn.js 不自登录，复用同一浏览器会话依次执行。
//
// 前置条件：
//   1) 后端与前端 dev server 都已启动（前端默认 http://localhost:18081）
//   2) 已安装 playwright： npm i -D playwright
//   3) 浏览器：优先使用本机已安装的 Google Chrome（无需额外下载）；
//      也可设置 OA_CHROME_PATH 指定路径，或先执行 npx playwright install chromium
//
// 用法：
//   set OA_INIT_ADMIN_PASSWORD=<当前 admin 口令>
//   node tests/run-ui.js
//
// 可用环境变量：
//   OA_UI_URL       前端地址，默认 http://localhost:18081（脚本内硬编码的地址会按此替换）
//   OA_CHROME_PATH  指定浏览器可执行文件
//   OA_TEST_NEW_PASSWORD  仅在账号仍处于强制改密状态时需要

const fs = require('fs');
const path = require('path');

// playwright 可能装在 ruoyi-ui/node_modules（README 建议的位置）而不是仓库根目录，
// 而 Node 只会从脚本所在目录逐级向上查找，因此这里按若干候选位置依次解析，
// 避免"装是装了但跑不起来"。
function loadPlaywright() {
  const candidates = [
    'playwright',
    path.join(__dirname, '..', 'ruoyi-ui', 'node_modules', 'playwright'),
    path.join(__dirname, '..', 'node_modules', 'playwright'),
    'playwright-core'
  ];
  const errors = [];
  for (const c of candidates) {
    try {
      return require(c);
    } catch (e) {
      errors.push('  ' + c + ' -> ' + String(e.message).split('\n')[0]);
    }
  }
  console.error('无法加载 playwright。请先安装（任选其一，均无需写入 package.json）：');
  console.error('  cd ruoyi-ui && npm i --no-save playwright    （推荐；浏览器用本机 Chrome 即可）');
  console.error('  npm i -g playwright');
  console.error('  或设置 NODE_PATH 指向已安装的 node_modules');
  console.error(errors.join('\n'));
  process.exit(2);
}
const { chromium } = loadPlaywright();

const SPEC_DIR = __dirname;
const UI_URL = process.env.OA_UI_URL || 'http://localhost:18081';
const SPEC_DEFAULT_URL = 'http://localhost:18081';

const CHROME_CANDIDATES = [
  process.env.OA_CHROME_PATH,
  process.env.ProgramFiles && path.join(process.env.ProgramFiles, 'Google/Chrome/Application/chrome.exe'),
  process.env['ProgramFiles(x86)'] && path.join(process.env['ProgramFiles(x86)'], 'Google/Chrome/Application/chrome.exe'),
  process.env.LOCALAPPDATA && path.join(process.env.LOCALAPPDATA, 'Google/Chrome/Application/chrome.exe')
].filter(Boolean);

function resolveChrome() {
  for (const c of CHROME_CANDIDATES) {
    try {
      if (fs.existsSync(c)) return c;
    } catch (e) { /* ignore */ }
  }
  return undefined; // 交给 Playwright 使用自带浏览器
}

function loadSpec(file) {
  let text = fs.readFileSync(path.join(SPEC_DIR, file), 'utf8');
  // 脚本里硬编码了前端地址，这里统一替换成 OA_UI_URL
  if (UI_URL !== SPEC_DEFAULT_URL) {
    text = text.split(SPEC_DEFAULT_URL).join(UI_URL);
  }
  // eslint-disable-next-line no-eval
  return eval('(' + text + ')');
}

function summarize(name, r) {
  const lines = ['--- ' + name];
  if (!r) return lines.join('\n');
  if (r.loggedIn !== undefined) lines.push('    登录成功: ' + r.loggedIn);
  const routes = r.routeResults || r.results;
  if (Array.isArray(routes)) {
    const bad = routes.filter(x => !x.ok);
    lines.push('    路由巡检: ' + (routes.length - bad.length) + '/' + routes.length + ' 通过');
    for (const b of bad) {
      lines.push('      未通过 ' + b.route + (b.error ? (' — ' + b.error) : ' — 缺少预期文本') + ' url=' + b.url);
    }
  }
  if (r.created !== undefined) lines.push('    公告创建: ' + r.created + '  删除: ' + r.deleted);
  if (r.announcementCreated !== undefined) {
    lines.push('    公告创建: ' + r.announcementCreated + '  删除: ' + r.announcementDeleted);
  }
  if (Array.isArray(r.errors)) {
    lines.push('    控制台/页面错误: ' + r.errors.length);
    for (const e of r.errors.slice(0, 8)) lines.push('      ' + e);
  }
  return lines.join('\n');
}

(async () => {
  const chrome = resolveChrome();
  console.log('前端地址: ' + UI_URL);
  console.log('浏览器  : ' + (chrome || '(Playwright 自带 chromium)'));
  const browser = await chromium.launch(chrome ? { executablePath: chrome, headless: true } : { headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();
  const out = {};
  let failed = false;

  for (const [key, file] of [
    ['functional', 'ui-functional.fn.js'],
    ['smoke', 'ui-smoke.fn.js'],
    ['announcementCrud', 'ui-announcement-crud.fn.js']
  ]) {
    try {
      out[key] = await loadSpec(file)(page);
      console.log(summarize('tests/' + file, out[key]));
    } catch (e) {
      failed = true;
      out[key + 'Error'] = e.message;
      console.log('--- tests/' + file + '\n    运行异常: ' + e.message);
    }
  }

  await browser.close();
  const resultPath = path.join(SPEC_DIR, 'ui-result.json');
  fs.writeFileSync(resultPath, JSON.stringify(out, null, 2));
  console.log('\n结果已写入 ' + resultPath);
  process.exit(failed ? 1 : 0);
})().catch(e => {
  console.error('RUNNER_ERROR: ' + e.message);
  process.exit(1);
});

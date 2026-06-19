# Wallpaper Engine 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `05-profile.html` 的 VIP 卡下方新增「壁纸引擎」状态卡（未启用 / 已启用两态），点击推入新的 `08-wallpaper.html` 设置页，提供相册选择、切换方式、显示方式、高级过滤四组配置。引擎状态用 `localStorage` 持久化，两页联动。

**Architecture:**
- 复用 05-profile.html 的设备框 / token / 卡片节奏（storage-big 同款 16px 圆角 surface 卡）
- 08-wallpaper.html 是新单文件页面（vanilla JS，无框架），自包含设备框 + 所有 CSS + 脚本
- Sheet 模块从 07-album.html **复制**（带 `duplicated from 07-album` 注释），原型阶段不做跨文件抽取
- 状态用 `localStorage` key `__wallpaperEngine` 存储 `{enabled, albumId, mode, display, adv, nextTime}`，05-profile.html 加载时读取渲染对应卡态
- 预览图用 CSS 渐变占位，不引外部资源

**Tech Stack:** HTML5 / CSS3 / vanilla JS / localStorage / Playwright (verify)

**Spec:** `docs/superpowers/specs/2026-06-13-wallpaper-engine-design.md`

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `05-profile.html` | 我的主页，新增壁纸引擎状态卡 | Modify（仅追加） |
| `08-wallpaper.html` | 新建：壁纸引擎设置页 | Create |
| `07-album.html` | 不动 | Read-only |
| `docs/superpowers/specs/2026-06-13-wallpaper-engine-design.md` | 设计规格 | Read-only |

`05-profile.html` 改动集中在两处：① `</style>` 前追加 wallpaper-card CSS；② VIP 卡 之后 / 「常用功能」之前插入状态卡 DOM；③ `<script>` 块末尾追加状态读取逻辑。

`08-wallpaper.html` 是新文件，包含完整 HTML / CSS / JS。

---

## Task 1: 我的页状态卡（未启用态）

**Files:**
- Modify: `05-profile.html:360`（`</style>` 前追加 CSS）
- Modify: `05-profile.html:443`（VIP 卡 之后 / 「常用功能」之前插入状态卡 DOM）

- [ ] **Step 1: 在 `</style>` 前追加 wallpaper-card CSS**

在 `05-profile.html` 第 360 行（`/* 退出 */` 之前）追加：

```css
/* 壁纸引擎卡 */
.wp-card {
  margin: 0 16px 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px 18px;
  display: flex; align-items: center; gap: 14px;
  cursor: pointer;
  transition: background 150ms;
  text-decoration: none;
  color: inherit;
}
.wp-card:hover { background: #F5F5F5; }
.wp-card .wp-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  background: rgba(47,111,235,0.10);
  color: var(--accent);
  display: grid; place-items: center;
  flex-shrink: 0;
}
.wp-card .wp-icon svg { width: 20px; height: 20px; }
.wp-card .wp-body { flex: 1; min-width: 0; }
.wp-card .wp-title-row {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 2px;
}
.wp-card .wp-title {
  font-size: 15px; font-weight: 600;
  color: var(--fg);
  letter-spacing: -0.01em;
}
.wp-card .wp-badge {
  font-size: 10px; font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}
.wp-card .wp-badge.off {
  background: rgba(0,0,0,0.04);
  color: var(--muted);
}
.wp-card .wp-badge.on {
  background: rgba(47,111,235,0.10);
  color: var(--accent);
}
.wp-card .wp-sub {
  font-size: 12px; color: var(--muted);
  font-weight: 400;
}
.wp-card .wp-chev { color: var(--muted); flex-shrink: 0; }
.wp-card .wp-chev svg { width: 14px; height: 14px; }

/* 已启用态：缩略图 + 状态行 */
.wp-card .wp-thumb {
  width: 64px; height: 64px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2F6FEB, #9333EA);
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}
.wp-card .wp-thumb::after {
  content: ""; position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.4), transparent 50%);
}
.wp-card .wp-detail {
  display: flex; flex-direction: column; gap: 2px;
  font-size: 12.5px;
}
.wp-card .wp-detail .wp-album {
  font-size: 14px; font-weight: 600;
  color: var(--fg);
}
.wp-card .wp-detail .wp-meta {
  font-size: 12px; color: var(--muted);
}
.wp-card .wp-detail .wp-next {
  font-size: 12px; color: var(--muted);
  font-variant-numeric: tabular-nums;
}
```

- [ ] **Step 2: 在 VIP 卡 之后 / 常用功能 之前插入状态卡 DOM**

在 `05-profile.html` 第 443 行（VIP 卡 `</div>` 结束）之后、第 446 行（`<!-- 常用功能 -->` 之前）插入：

```html
<!-- 壁纸引擎 -->
<a class="wp-card" id="wpCard" href="08-wallpaper.html">
  <div class="wp-icon">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
      <rect x="3" y="4" width="18" height="14" rx="2"/>
      <path d="M3 18l5-5 4 4 4-4 5 5"/>
      <circle cx="9" cy="10" r="1.5" fill="currentColor"/>
    </svg>
  </div>
  <div class="wp-body">
    <div class="wp-title-row">
      <span class="wp-title">壁纸引擎</span>
      <span class="wp-badge off" id="wpBadge">未启用</span>
    </div>
    <div class="wp-sub" id="wpSub">让相册成为会动的壁纸</div>
  </div>
  <span class="wp-chev">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"/></svg>
  </span>
</a>
```

**注意**：未启用态只渲染 `wp-icon` 模式（无 `wp-thumb`），CSS 已通过选择器区分两种 DOM 结构。

- [ ] **Step 3: 验证 DOM 与 CSS 注入**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
grep -c "wp-card" 05-profile.html
grep -c "wpBadge" 05-profile.html
grep -c "wpSub" 05-profile.html
```

期望：每个命令输出 `2`（CSS 1 次 + DOM 1 次）。

- [ ] **Step 4: 提交状态卡基础**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 05-profile.html
git commit -m "feat(profile): 我的页新增壁纸引擎状态卡 (未启用态)"
```

---

## Task 2: 08-wallpaper.html 基础结构 + 顶栏 + Hero 卡

**Files:**
- Create: `08-wallpaper.html`

- [ ] **Step 1: 创建新文件，写入基础结构**

```bash
touch "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266/08-wallpaper.html"
```

写入以下内容（**完整文件**，按 `cat > file <<'EOF'` 一次性写入或拆为多次 Edit）：

```html
<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>壁纸引擎 · StarVault</title>
<style>
  *, *::before, *::after { box-sizing: border-box; }
  html, body { margin: 0; padding: 0; }
  body {
    background: #EDECE8;
    color: #111;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'SF Pro Text', system-ui, sans-serif;
    -webkit-font-smoothing: antialiased;
    min-height: 100vh;
    display: grid; place-items: center;
    padding: 32px 16px;
  }
  p { margin: 0; }
  button { font: inherit; cursor: pointer; border: 0; background: none; padding: 0; color: inherit; }
  a { color: inherit; text-decoration: none; }
  :root {
    --bg: #FAFAFA;
    --surface: #FFFFFF;
    --fg: #111111;
    --muted: #6B6B6B;
    --border: #E5E5E5;
    --accent: #2F6FEB;
    --accent-soft: rgba(47,111,235,0.08);
  }
  .device {
    width: 412px; height: 900px;
    background: var(--bg);
    border-radius: 44px;
    border: 9px solid #1A1A1A;
    position: relative; overflow: hidden;
    box-shadow:
      0 0 0 1px rgba(0,0,0,0.04),
      0 30px 60px -20px rgba(0,0,0,0.25),
      0 18px 36px -18px rgba(0,0,0,0.3);
  }
  .device::before {
    content: ""; position: absolute;
    top: 14px; left: 50%;
    transform: translateX(-50%);
    width: 96px; height: 26px;
    background: #1A1A1A; border-radius: 13px;
    z-index: 30;
  }
  .status-bar {
    position: relative; z-index: 5;
    height: 38px; padding: 0 28px;
    display: flex; align-items: center; justify-content: space-between;
    font-size: 13px; font-weight: 600; color: #111;
    background: var(--bg);
  }
  .status-bar .status-icons { display: inline-flex; align-items: center; gap: 6px; }
  .screen {
    height: calc(900px - 38px - 64px);
    position: relative; overflow-y: auto;
    background: var(--bg);
  }
  .nav-bar {
    height: 64px;
    display: flex; align-items: center; justify-content: center; gap: 56px;
    background: var(--bg);
  }
  .nav-bar .nav-btn { width: 16px; height: 16px; border: 1.4px solid #1A1A1A; border-radius: 3px; }
  .nav-bar .nav-btn.pill { width: 38px; height: 16px; border-radius: 9px; }
  .nav-bar .nav-btn.dot { width: 16px; height: 16px; border-radius: 50%; background: #1A1A1A; border: 0; }

  /* 顶栏 */
  .topbar {
    height: 56px;
    padding: 0 16px;
    display: flex; align-items: center; justify-content: space-between;
    background: var(--bg);
    border-bottom: 1px solid var(--border);
    position: sticky; top: 0; z-index: 10;
  }
  .topbar .back {
    width: 40px; height: 40px;
    display: grid; place-items: center;
    border-radius: 50%;
    transition: background 150ms;
  }
  .topbar .back:hover { background: rgba(0,0,0,0.04); }
  .topbar .back svg { width: 20px; height: 20px; }
  .topbar .title { font-size: 16px; font-weight: 600; color: var(--fg); }
  .topbar .toggle-wrap { width: 40px; display: flex; justify-content: flex-end; }

  /* iOS toggle */
  .toggle {
    width: 44px; height: 26px;
    background: rgba(120,120,128,0.32);
    border-radius: 13px;
    position: relative;
    transition: background 200ms;
    cursor: pointer;
    flex-shrink: 0;
  }
  .toggle::after {
    content: ""; position: absolute;
    top: 2px; left: 2px;
    width: 22px; height: 22px;
    background: #fff;
    border-radius: 50%;
    box-shadow: 0 1px 3px rgba(0,0,0,0.2);
    transition: transform 200ms;
  }
  .toggle.on { background: var(--accent); }
  .toggle.on::after { transform: translateX(18px); }

  /* Hero 卡 */
  .hero {
    margin: 16px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 20px;
    display: flex; flex-direction: column; align-items: center;
  }
  .hero .preview {
    width: 240px; height: 320px;
    border-radius: 12px;
    background: linear-gradient(135deg, #2F6FEB 0%, #9333EA 50%, #EC4899 100%);
    position: relative;
    overflow: hidden;
    transition: opacity 200ms;
  }
  .hero .preview::after {
    content: ""; position: absolute;
    inset: 0;
    background: radial-gradient(circle at 30% 20%, rgba(255,255,255,0.4), transparent 50%);
  }
  .hero .dots {
    display: flex; gap: 6px;
    margin: 14px 0 12px;
  }
  .hero .dot {
    width: 6px; height: 6px;
    border-radius: 50%;
    background: var(--border);
  }
  .hero .dot.on { background: var(--accent); }
  .hero .src {
    font-size: 13px; font-weight: 500;
    color: var(--fg);
    text-align: center;
  }
  .hero .next {
    font-size: 13px; color: var(--muted);
    margin-top: 4px;
    font-variant-numeric: tabular-nums;
  }

  /* Section 通用 */
  .sec-h {
    margin: 20px 20px 10px;
    font-size: 11px; font-weight: 600;
    color: var(--muted);
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }
  .sec-card {
    margin: 0 16px 14px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 16px;
    overflow: hidden;
  }
  .sec-row {
    display: flex; align-items: center;
    padding: 14px 18px;
    gap: 12px;
    border-top: 1px solid var(--border);
  }
  .sec-row:first-of-type { border-top: 0; }
  .sec-row .l { flex: 1; font-size: 14px; font-weight: 500; color: var(--fg); }
  .sec-row .r { font-size: 12px; color: var(--muted); }

  /* 底部固定栏 */
  .footer-bar {
    position: sticky; bottom: 0;
    padding: 16px;
    background: var(--bg);
    border-top: 1px solid var(--border);
  }
  .btn-primary {
    width: 100%; height: 48px;
    background: var(--accent); color: #fff;
    border-radius: 100px;
    font-size: 15px; font-weight: 600;
    transition: opacity 150ms;
  }
  .btn-primary:hover { opacity: 0.9; }
  .btn-primary:disabled {
    background: rgba(120,120,128,0.32);
    color: #fff;
  }
</style>
</head>
<body>
  <div class="device">
    <div class="status-bar">
      <span>9:41</span>
      <span class="status-icons">
        <svg width="16" height="11" viewBox="0 0 16 11" fill="currentColor"><path d="M1 8h2v2H1zM5 6h2v4H5zM9 4h2v6H9zM13 1h2v9h-2z"/></svg>
        <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor"><path d="M7 0C4.4 0 2 1 0 2.7L1.4 4.4C3 3 4.9 2.2 7 2.2s4 .8 5.6 2.2L14 2.7C12 1 9.6 0 7 0zm0 4c-1.4 0-2.8.5-3.8 1.4L4.6 7C5.2 6.4 6 6 7 6s1.8.4 2.4 1L10.8 5.4C9.8 4.5 8.4 4 7 4zm0 4a1.5 1.5 0 100 3 1.5 1.5 0 000-3z"/></svg>
        <svg width="22" height="11" viewBox="0 0 22 11" fill="none"><rect x="0.5" y="0.5" width="18" height="10" rx="2" stroke="currentColor"/><rect x="2" y="2" width="15" height="7" rx="1" fill="currentColor"/><rect x="20" y="3.5" width="1.5" height="4" rx="0.5" fill="currentColor"/></svg>
      </span>
    </div>

    <div class="screen">
      <div class="topbar">
        <a class="back" href="05-profile.html" aria-label="返回">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        </a>
        <span class="title">壁纸引擎</span>
        <span class="toggle-wrap">
          <span class="toggle" id="engineToggle" role="switch" aria-checked="false"></span>
        </span>
      </div>

      <div class="hero">
        <div class="preview" id="preview"></div>
        <div class="dots" id="dots">
          <span class="dot on"></span>
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
        <div class="src" id="srcText">来自「旅行 2025」· 第 23 张</div>
        <div class="next" id="nextText">下次切换 14:30</div>
      </div>

      <div class="sec-h">相册来源</div>
      <div class="sec-card">
        <div class="sec-row" id="albumRow">
          <span class="l">📁 <span id="currentAlbum">旅行 2025</span></span>
          <span class="r">更换 ›</span>
        </div>
        <div class="sec-row">
          <span class="l" style="color:var(--muted); font-size:13px">包含 <b id="albumCount" style="color:var(--fg)">247</b> 张照片</span>
        </div>
      </div>

      <div class="footer-bar">
        <button class="btn-primary" id="switchNowBtn">立即切换下一张</button>
      </div>
    </div>

    <div class="nav-bar">
      <div class="nav-btn"></div>
      <div class="nav-btn pill"></div>
      <div class="nav-btn dot"></div>
    </div>
  </div>
</body>
</html>
```

- [ ] **Step 2: 验证 HTML 解析**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
node -e "const fs=require('fs'); const html=fs.readFileSync('08-wallpaper.html','utf8'); console.log('toggle:', html.includes('id=\"engineToggle\"')); console.log('preview:', html.includes('id=\"preview\"')); console.log('btn:', html.includes('id=\"switchNowBtn\"'));"
```

期望：三行 `true`。

- [ ] **Step 3: 启动本地服务 + 浏览器打开确认骨架**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
lsof -i :8765 -t 2>/dev/null || nohup python3 -m http.server 8765 --bind 127.0.0.1 > /tmp/wp-srv.log 2>&1 &
sleep 1
curl -s -m 3 -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8765/08-wallpaper.html
```

期望：`200`。在浏览器打开 `http://127.0.0.1:8765/08-wallpaper.html`（视口 1280×800），确认：顶栏 + Hero 卡 + 相册来源 + 底部按钮可见，布局正常。

- [ ] **Step 4: 提交基础结构**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 08-wallpaper.html
git commit -m "feat(wallpaper): 创建 08-wallpaper.html 基础结构 + 顶栏 + Hero 卡"
```

---

## Task 3: 状态模型 + 切换方式 Section

**Files:**
- Modify: `08-wallpaper.html`（在 `</body>` 前插入 `<script>` 块）
- Modify: `08-wallpaper.html`（追加 radio 列表 + 切换方式 section 的 CSS 与 DOM）

- [ ] **Step 1: 在 `</style>` 前追加切换方式相关 CSS**

找到 `08-wallpaper.html` 现有的 `</style>`（应在 `.btn-primary:disabled` 之后），在其前面追加：

```css
/* 切换方式 radio */
.sec-card.radio-card .sec-row {
  padding: 0 18px;
  cursor: pointer;
  height: 48px;
}
.radio {
  width: 22px; height: 22px;
  border-radius: 50%;
  border: 1.5px solid rgba(120,120,128,0.36);
  display: grid; place-items: center;
  flex-shrink: 0;
  transition: border-color 150ms;
}
.radio.on { border-color: var(--accent); }
.radio.on::after {
  content: ""; width: 14px; height: 14px;
  border-radius: 50%;
  background: var(--accent);
}
```

- [ ] **Step 2: 在 Hero 卡 与 相册来源 section 之间插入切换方式 DOM**

找到 `08-wallpaper.html` 中 `</div> <!-- end hero -->` 之后、`<div class="sec-h">相册来源</div>` 之前，插入：

```html
<div class="sec-h">切换方式</div>
<div class="sec-card radio-card">
  <div class="sec-row" data-mode="unlock"><div class="radio on"></div><span class="l">每次解锁</span></div>
  <div class="sec-row" data-mode="hourly"><div class="radio"></div><span class="l">每小时</span></div>
  <div class="sec-row" data-mode="six-hours"><div class="radio"></div><span class="l">每 6 小时</span></div>
  <div class="sec-row" data-mode="daily-9"><div class="radio"></div><span class="l">每天 09:00</span></div>
  <div class="sec-row" data-mode="manual"><div class="radio"></div><span class="l">仅手动</span></div>
</div>
```

- [ ] **Step 3: 插入状态模型 + radio 交互脚本**

在 `</body>` 前面插入：

```html
<script>
(function () {
  'use strict';

  // ───── 状态（localStorage 持久化）─────
  const STORAGE_KEY = '__wallpaperEngine';
  const DEFAULT_STATE = {
    enabled: false,
    albumId: 'travel',
    mode: 'unlock',
    display: 'fit',
    adv: { fav: false, recent: false, wifi: false, dedupe: false }
  };
  function loadState() {
    try {
      const s = localStorage.getItem(STORAGE_KEY);
      if (s) return Object.assign({}, DEFAULT_STATE, JSON.parse(s));
    } catch (e) {}
    return Object.assign({}, DEFAULT_STATE);
  }
  function saveState() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(STATE)); } catch (e) {}
  }
  let STATE = loadState();

  // ───── 模式 label ─────
  const MODE_LABELS = {
    'unlock': '每次解锁',
    'hourly': '每小时',
    'six-hours': '每 6 小时',
    'daily-9': '每天 09:00',
    'manual': '仅手动'
  };

  // ───── 渲染：顶栏 toggle ─────
  function renderToggle() {
    const t = document.getElementById('engineToggle');
    t.classList.toggle('on', STATE.enabled);
    t.setAttribute('aria-checked', STATE.enabled ? 'true' : 'false');
  }

  // ───── 渲染：切换方式 radio ─────
  function renderMode() {
    document.querySelectorAll('.radio-card .sec-row').forEach(row => {
      const on = row.dataset.mode === STATE.mode;
      row.querySelector('.radio').classList.toggle('on', on);
    });
  }

  // ───── 渲染：footer 按钮文字 ─────
  function renderFooter() {
    const btn = document.getElementById('switchNowBtn');
    btn.disabled = !STATE.enabled;
  }

  // ───── 事件：radio 选中 ─────
  document.querySelector('.radio-card').addEventListener('click', (e) => {
    const row = e.target.closest('.sec-row');
    if (!row) return;
    STATE.mode = row.dataset.mode;
    saveState();
    renderMode();
  });

  // ───── 事件：toggle 切换 ─────
  document.getElementById('engineToggle').addEventListener('click', () => {
    STATE.enabled = !STATE.enabled;
    saveState();
    renderToggle();
    renderFooter();
  });

  // ───── 首次渲染 ─────
  renderToggle();
  renderMode();
  renderFooter();

  // 暴露
  window.__wallpaperEngine = { getState: () => STATE };
})();
</script>
```

- [ ] **Step 4: 浏览器手动验证**

启动服务后浏览器打开 `08-wallpaper.html`：
1. 顶栏 toggle 初始灰（未启用），点击变蓝
2. 切换方式默认"每次解锁" radio 选中
3. 点"每小时" → 切换选中，状态持久化（刷新页面仍保持）
4. F12 console 运行 `__wallpaperEngine.getState()`，返回 `{enabled: false, mode: 'hourly', ...}`

- [ ] **Step 5: 提交状态 + radio**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 08-wallpaper.html
git commit -m "feat(wallpaper): 状态模型 + 切换方式 radio + 顶栏 toggle"
```

---

## Task 4: 相册来源 + 复用 sheet

**Files:**
- Modify: `08-wallpaper.html`（追加 sheet CSS + DOM + 切换逻辑）

- [ ] **Step 1: 追加 sheet 相关 CSS（与 07-album.html 保持一致）**

在 `08-wallpaper.html` 的 `</style>` 之前追加：

```css
/* ───────── Sheet（复用 07-album）───────── */
.sheet-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 50;
  opacity: 0; pointer-events: none;
  transition: opacity 200ms ease;
}
.sheet-overlay.open { opacity: 1; pointer-events: auto; }

.sheet-panel {
  position: absolute; left: 0; right: 0; bottom: 0;
  background: var(--surface);
  border-top-left-radius: 28px;
  border-top-right-radius: 28px;
  z-index: 51;
  transform: translateY(100%);
  transition: transform 280ms cubic-bezier(0.32, 0.72, 0, 1);
  max-height: 92vh;
  display: flex; flex-direction: column;
  padding: 12px 0 24px;
  box-shadow: 0 -8px 24px -8px rgba(0,0,0,0.18);
}
.sheet-panel.open { transform: translateY(0); }

.sheet-grabber {
  width: 32px; height: 4px;
  background: #C4C4C4;
  border-radius: 2px;
  margin: 0 auto 20px;
}

.sheet-title {
  font-size: 18px; font-weight: 500;
  letter-spacing: -0.01em;
  padding: 0 24px 16px;
}

.sheet-list { overflow-y: auto; -webkit-overflow-scrolling: touch; }

.folder-row {
  display: flex; align-items: center; gap: 16px;
  height: 56px; padding: 0 24px;
  position: relative;
  cursor: pointer;
  transition: background 150ms;
}
.folder-row:hover { background: rgba(0,0,0,0.03); }
.folder-row.current { background: rgba(47,111,235,0.08); }
.folder-row.current::before {
  content: ''; position: absolute; left: 0; top: 12px; bottom: 12px;
  width: 3px; background: var(--accent); border-radius: 0 2px 2px 0;
}
.folder-row .fr-icon {
  width: 24px; height: 24px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 6px;
  background: var(--accent);
  color: #fff;
}
.folder-row .fr-icon svg { width: 14px; height: 14px; }
.folder-row .fr-name { flex: 1; font-size: 14px; color: var(--fg); }
.folder-row .fr-chev { color: var(--muted); font-size: 14px; }
.folder-row .fr-check { color: var(--accent); font-size: 16px; font-weight: 600; }
```

- [ ] **Step 2: 在 `</div> <!-- end device -->` 之前插入 sheet DOM**

找到 `08-wallpaper.html` 中 `</div>` 紧接 `<div class="nav-bar">` 之前的行（即 `.screen` 关闭之后、`.nav-bar` 之前），插入：

```html
<!-- Sheet: 选择相册 -->
<div class="sheet-overlay" id="sheetOverlay" aria-hidden="true"></div>
<div class="sheet-panel" id="sheetPanel" role="dialog" aria-label="选择相册">
  <div class="sheet-grabber"></div>
  <div class="sheet-title">选择相册</div>
  <div class="sheet-list" id="sheetList"></div>
</div>
```

**注意**：Sheet 必须放在 `.device` 内部（与 07-album.html 修复后的位置一致），避免 width bug。

- [ ] **Step 3: 在 Task 3 的 `<script>` 块内、相册数据 + 渲染函数后追加**

将 Task 3 的脚本块末尾（在 `window.__wallpaperEngine = ...` 这一行**之前**）插入以下代码（完整数据 + 工具 + 渲染 + 打开/关闭 + 切换）：

```js
  // ───── ALBUMS 数据（与 07-album.html 保持一致）─────
  const ALBUMS = [
    { id: 'mine',    name: '我的相册',     color: '#2F6FEB', count: 1247, children: [
      { id: 'mine-2025', name: '2025',     color: '#2F6FEB', count: 328 },
      { id: 'mine-2024', name: '2024',     color: '#2F6FEB', count: 612 }
    ]},
    { id: 'travel',  name: '旅行 2025',   color: '#16A34A', count: 328,  children: [] },
    { id: 'family',  name: '家庭',        color: '#F59E0B', count: 156,  children: [
      { id: 'family-baby', name: '宝宝成长', color: '#EC4899', count: 89 }
    ]},
    { id: 'work',    name: '工作',        color: '#8B5CF6', count: 47,   children: [] },
    { id: 'fav',     name: '收藏',        color: '#EF4444', count: 23,   children: [] }
  ];

  // ───── 工具：HTML 转义（XSS 防御）─────
  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // ───── 工具：根据 id 找 album ─────
  function findAlbum(id) {
    for (const a of ALBUMS) {
      if (a.id === id) return a;
      for (const c of a.children) if (c.id === id) return c;
    }
    return ALBUMS[0];
  }

  // ───── 渲染：sheet 列表（与 07-album 一致）─────
  function renderSheet() {
    const list = document.getElementById('sheetList');
    const current = findAlbum(STATE.albumId);
    const folderSvg = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"/></svg>';

    let html = '';
    ALBUMS.forEach(a => {
      const isCurrent = a.id === STATE.albumId;
      const hasChildren = a.children && a.children.length > 0;
      html += `
        <div class="folder-row${isCurrent ? ' current' : ''}" data-id="${escapeHtml(a.id)}">
          <span class="fr-icon" style="background:${escapeHtml(a.color)}">${folderSvg}</span>
          <span class="fr-name">${escapeHtml(a.name)}</span>
          ${hasChildren ? '<span class="fr-chev">›</span>' : (isCurrent ? '<span class="fr-check">✓</span>' : '')}
        </div>
      `;
    });

    list.innerHTML = html;
  }

  // ───── 渲染：当前相册显示 ─────
  function renderAlbum() {
    const a = findAlbum(STATE.albumId);
    document.getElementById('currentAlbum').textContent = a.name;
    document.getElementById('albumCount').textContent = a.count.toLocaleString();
  }

  // ───── 打开 / 关闭 sheet ─────
  function openSheet() {
    renderSheet();
    document.getElementById('sheetOverlay').classList.add('open');
    document.getElementById('sheetPanel').classList.add('open');
    document.getElementById('sheetOverlay').setAttribute('aria-hidden', 'false');
  }
  function closeSheet() {
    document.getElementById('sheetOverlay').classList.remove('open');
    document.getElementById('sheetPanel').classList.remove('open');
    document.getElementById('sheetOverlay').setAttribute('aria-hidden', 'true');
  }

  // ───── 切换相册 ─────
  function switchTo(albumId) {
    STATE.albumId = albumId;
    saveState();
    renderAlbum();
    closeSheet();
  }

  // ───── 事件：点相册行 → 弹 sheet ─────
  document.getElementById('albumRow').addEventListener('click', openSheet);

  // ───── 事件：点 sheet 遮罩 / 列表项 ─────
  document.getElementById('sheetOverlay').addEventListener('click', closeSheet);
  document.getElementById('sheetPanel').addEventListener('click', (e) => {
    const row = e.target.closest('.folder-row');
    if (row) switchTo(row.dataset.id);
  });

  // ───── 首次渲染 ─────
  renderAlbum();
```

并在 Task 3 的 `window.__wallpaperEngine = { getState: () => STATE }` 那一行**之后**追加（暴露切换函数）：

```js
  window.__wallpaperEngine.open = openSheet;
  window.__wallpaperEngine.switchTo = switchTo;
```

- [ ] **Step 4: 浏览器手动验证**

启动服务后浏览器打开 `08-wallpaper.html`：
1. 点相册行 → sheet 从底滑入
2. 看到"旅行 2025"行有蓝条 + 浅蓝底 + ✓（因为 STATE.albumId 初始 = 'travel'）
3. 点"收藏" → sheet 关闭，相册名变为"收藏"，数量 23
4. 验证 XSS：F12 console 跑 `__wallpaperEngine.switchTo('<img>')` → 不应插入图片（实际不会，因为 switchTo 只改 STATE，未注入 HTML）

- [ ] **Step 5: 提交 sheet 集成**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 08-wallpaper.html
git commit -m "feat(wallpaper): Section 1 相册来源 + 复用 07-album sheet 模块"
```

---

## Task 5: 显示方式 + 高级 Section

**Files:**
- Modify: `08-wallpaper.html`（追加 Section 3 / 4 的 CSS + DOM + JS）

- [ ] **Step 1: 追加显示方式 / 高级的 CSS**

在 `08-wallpaper.html` 的 `</style>` 之前追加：

```css
/* 显示方式 4 列 */
.display-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  padding: 4px;
}
.display-cell {
  border: 2px solid var(--border);
  border-radius: 12px;
  padding: 8px 6px 10px;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  cursor: pointer;
  transition: border-color 150ms;
  background: var(--surface);
}
.display-cell.on { border-color: var(--accent); }
.display-cell .thumb {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
  background: linear-gradient(135deg, #2F6FEB, #9333EA);
  position: relative;
  overflow: hidden;
}
.display-cell .thumb::after {
  content: ""; position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.3), transparent 50%);
}
/* 4 种显示方式视觉差异 */
.display-cell[data-display="fit"] .thumb { background: #111; }
.display-cell[data-display="fit"] .thumb::after {
  background: linear-gradient(135deg, #2F6FEB 0%, #9333EA 100%);
  width: 70%; height: 70%;
  top: 15%; left: 15%;
  border-radius: 4px;
}
.display-cell[data-display="fill"] .thumb {
  background: linear-gradient(135deg, #2F6FEB 0%, #9333EA 50%, #EC4899 100%);
}
.display-cell[data-display="smart"] .thumb {
  background: linear-gradient(135deg, #16A34A, #F59E0B);
}
.display-cell[data-display="smart"] .thumb::after {
  width: 60%; height: 60%;
  top: 20%; left: 20%;
  background: radial-gradient(circle, rgba(255,255,255,0.4), transparent 60%);
  border-radius: 50%;
}
.display-cell[data-display="blur"] .thumb {
  background: linear-gradient(135deg, #EC4899, #F59E0B);
  filter: blur(4px);
}
.display-cell[data-display="blur"] .thumb::after {
  filter: blur(0);
  width: 50%; height: 50%;
  top: 25%; left: 25%;
  background: rgba(255,255,255,0.5);
  border-radius: 4px;
}
.display-cell .label {
  font-size: 12px; font-weight: 500;
  color: var(--fg);
}
.display-cell.on .label { color: var(--accent); }

/* 高级折叠 */
.adv-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px;
  cursor: pointer;
  user-select: none;
}
.adv-head .l { font-size: 13px; font-weight: 600; color: var(--fg); }
.adv-head .chev {
  transition: transform 200ms;
  color: var(--muted);
}
.adv-head.open .chev { transform: rotate(180deg); }
.adv-body {
  max-height: 0;
  overflow: hidden;
  transition: max-height 240ms ease-out;
}
.adv-body.open { max-height: 240px; }

.adv-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px solid var(--border);
  font-size: 14px;
  color: var(--fg);
}
.adv-row .switch {
  width: 44px; height: 26px;
  background: rgba(120,120,128,0.32);
  border-radius: 13px;
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 200ms;
}
.adv-row .switch::after {
  content: ""; position: absolute;
  top: 2px; left: 2px;
  width: 22px; height: 22px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  transition: transform 200ms;
}
.adv-row .switch.on { background: var(--accent); }
.adv-row .switch.on::after { transform: translateX(18px); }
```

- [ ] **Step 2: 在切换方式 Section 后、Hero 之前的相对位置，插入显示方式 + 高级的 DOM**

实际位置：在 `<div class="sec-h">切换方式</div>` 所在的 radio card 之后、`<div class="footer-bar">` 之前，插入：

```html
<div class="sec-h">显示方式</div>
<div class="sec-card">
  <div class="display-grid" id="displayGrid">
    <div class="display-cell on" data-display="fit"><div class="thumb"></div><span class="label">适配</span></div>
    <div class="display-cell" data-display="fill"><div class="thumb"></div><span class="label">填充</span></div>
    <div class="display-cell" data-display="smart"><div class="thumb"></div><span class="label">智能</span></div>
    <div class="display-cell" data-display="blur"><div class="thumb"></div><span class="label">模糊</span></div>
  </div>
</div>

<div class="sec-h">高级</div>
<div class="sec-card">
  <div class="adv-head" id="advHead">
    <span class="l">高级过滤</span>
    <svg class="chev" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 9l6 6 6-6"/></svg>
  </div>
  <div class="adv-body" id="advBody">
    <div class="adv-row" data-key="fav"><span>仅收藏照片</span><span class="switch"></span></div>
    <div class="adv-row" data-key="recent"><span>仅最近 30 天</span><span class="switch"></span></div>
    <div class="adv-row" data-key="wifi"><span>仅 Wi-Fi 下预下载</span><span class="switch"></span></div>
    <div class="adv-row" data-key="dedupe"><span>跳过相似照片</span><span class="switch"></span></div>
  </div>
</div>
```

- [ ] **Step 3: 在 Task 3 脚本块的首次渲染区（`renderToggle(); renderMode(); renderFooter();` 之后）追加**

```js
  // ───── 渲染：显示方式 ─────
  function renderDisplay() {
    document.querySelectorAll('.display-cell').forEach(c => {
      c.classList.toggle('on', c.dataset.display === STATE.display);
    });
  }
  // ───── 渲染：高级 toggle ─────
  function renderAdv() {
    document.querySelectorAll('.adv-row .switch').forEach(sw => {
      const key = sw.parentElement.dataset.key;
      sw.classList.toggle('on', !!STATE.adv[key]);
    });
  }
  // ───── 事件：显示方式 ─────
  document.getElementById('displayGrid').addEventListener('click', (e) => {
    const cell = e.target.closest('.display-cell');
    if (!cell) return;
    STATE.display = cell.dataset.display;
    saveState();
    renderDisplay();
  });
  // ───── 事件：高级折叠 ─────
  document.getElementById('advHead').addEventListener('click', () => {
    document.getElementById('advHead').classList.toggle('open');
    document.getElementById('advBody').classList.toggle('open');
  });
  // ───── 事件：高级 switch ─────
  document.querySelector('.adv-body').addEventListener('click', (e) => {
    const sw = e.target.closest('.switch');
    if (!sw) return;
    const key = sw.parentElement.dataset.key;
    STATE.adv[key] = !STATE.adv[key];
    saveState();
    renderAdv();
  });

  // ───── 首次渲染追加 ─────
  renderDisplay();
  renderAdv();
```

- [ ] **Step 4: 浏览器手动验证**

启动服务后浏览器打开 `08-wallpaper.html`：
1. 显示方式默认"适配"选中（2px accent border）
2. 点"填充" → 切换选中态
3. 点"高级过滤"标题 → advBody 展开（4 个 switch 可见）
4. 点任一 switch（如"仅收藏照片"）→ switch 变蓝
5. F12 console：`__wallpaperEngine.getState()` 返回 `{display: 'fill', adv: {fav: true, ...}, ...}`

- [ ] **Step 5: 提交显示方式 + 高级**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 08-wallpaper.html
git commit -m "feat(wallpaper): 显示方式 4 选 + 高级折叠面板"
```

---

## Task 6: 切换反馈 + footer 按钮 + 联动 profile 卡片

**Files:**
- Modify: `08-wallpaper.html`（追加切换逻辑）
- Modify: `05-profile.html`（追加状态读取 + 状态卡动态切换）

- [ ] **Step 1: 08-wallpaper.html 加 CSS 切换反馈**

在 `</style>` 前追加：

```css
/* 预览切换动画 */
.preview.switching { opacity: 0; }

/* 「已暂停」overlay */
.hero .pause-overlay {
  position: absolute; inset: 0;
  background: rgba(255,255,255,0.6);
  z-index: 5;
  display: none;
  align-items: center; justify-content: center;
  border-radius: 12px;
  font-size: 14px; font-weight: 500;
  color: var(--muted);
}
.hero .pause-overlay.show { display: flex; }
```

- [ ] **Step 2: 在 08-wallpaper.html 脚本块内追加切换逻辑**

在 Task 5 末尾（`renderAdv();` 之后）追加：

```js
  // ───── 立即切换下一张 ─────
  function switchNow() {
    if (!STATE.enabled) return;
    const preview = document.getElementById('preview');
    const dots = document.querySelectorAll('#dots .dot');
    const srcText = document.getElementById('srcText');
    const nextText = document.getElementById('nextText');

    preview.classList.add('switching');

    setTimeout(() => {
      // 推进激活的圆点（循环）
      const onIdx = Array.from(dots).findIndex(d => d.classList.contains('on'));
      dots[onIdx].classList.remove('on');
      const nextIdx = (onIdx + 1) % dots.length;
      dots[nextIdx].classList.add('on');

      // 数字 + 文字更新
      const cur = parseInt(srcText.textContent.match(/第 (\d+) 张/)[1], 10);
      srcText.textContent = `来自「${findAlbum(STATE.albumId).name}」· 第 ${cur + 1} 张`;

      // 重算下次切换时间（按 mode）
      const times = {
        'unlock': '下次解锁',
        'hourly': '下次 1 小时后',
        'six-hours': '下次 6 小时后',
        'daily-9': '明天 09:00',
        'manual': '不会自动切换'
      };
      nextText.textContent = times[STATE.mode];

      preview.classList.remove('switching');
    }, 200);
  }

  // ───── 暂停 / 恢复视觉反馈 ─────
  function showPaused(show) {
    // 复用 hero 内的 pause-overlay（如果存在）
    let ov = document.querySelector('.hero .pause-overlay');
    if (!ov) {
      const hero = document.querySelector('.hero');
      ov = document.createElement('div');
      ov.className = 'pause-overlay';
      ov.textContent = '已暂停';
      hero.appendChild(ov);
    }
    ov.classList.toggle('show', show);
  }

  // ───── 改写 engine toggle 行为，加暂停反馈 ─────
  document.getElementById('engineToggle').addEventListener('click', () => {
    STATE.enabled = !STATE.enabled;
    saveState();
    renderToggle();
    renderFooter();
    showPaused(!STATE.enabled);
    if (STATE.enabled) {
      setTimeout(() => showPaused(false), 500);
    }
  });

  // ───── 初始显示模式/下次切换文字 ─────
  (function initTexts() {
    const times = {
      'unlock': '下次解锁',
      'hourly': '下次 1 小时后',
      'six-hours': '下次 6 小时后',
      'daily-9': '明天 09:00',
      'manual': '不会自动切换'
    };
    const nextText = document.getElementById('nextText');
    if (nextText) nextText.textContent = times[STATE.mode];
  })();

  // ───── footer 按钮 ─────
  document.getElementById('switchNowBtn').addEventListener('click', switchNow);
```

- [ ] **Step 3: 05-profile.html 追加状态卡联动**

在 `05-profile.html` 的 `</body>` 之前（注入式脚本块**之后**或**新增**一个 IIFE）追加：

```html
<script>
/* 壁纸引擎状态卡：根据 localStorage 渲染 active 态 */
(function () {
  'use strict';

  // XSS 防御：所有从 localStorage 流入 DOM 的字符串都走这里
  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  const STORAGE_KEY = '__wallpaperEngine';
  let enabled = false;
  let albumName = '旅行 2025';
  let modeLabel = '每次解锁';
  let displayLabel = '适配';
  let nextTime = '14:30';
  try {
    const s = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
    enabled = !!s.enabled;
    if (s.albumId) {
      const map = { 'mine':'我的相册','travel':'旅行 2025','family':'家庭','work':'工作','fav':'收藏' };
      albumName = map[s.albumId] || albumName;
    }
    if (s.mode) {
      const map = { 'unlock':'每次解锁','hourly':'每小时','six-hours':'每 6 小时','daily-9':'每天 09:00','manual':'仅手动' };
      modeLabel = map[s.mode] || modeLabel;
    }
    if (s.display) {
      const map = { 'fit':'适配','fill':'填充','smart':'智能','blur':'模糊' };
      displayLabel = map[s.display] || displayLabel;
    }
  } catch (e) {}

  const card = document.getElementById('wpCard');
  const badge = document.getElementById('wpBadge');
  const sub = document.getElementById('wpSub');
  if (!card || !badge || !sub) return;

  if (enabled) {
    badge.textContent = '启用中';
    badge.classList.remove('off');
    badge.classList.add('on');
    // 全部动态值经 escapeHtml 包装
    sub.innerHTML = `
      <div class="wp-detail">
        <span class="wp-album">${escapeHtml(albumName)}</span>
        <span class="wp-meta">${escapeHtml(modeLabel)} · ${escapeHtml(displayLabel)}</span>
        <span class="wp-next">下次切换 ${escapeHtml(nextTime)}</span>
      </div>
    `;
    // 在 wp-icon 之前插入 wp-thumb
    const icon = card.querySelector('.wp-icon');
    if (icon && !card.querySelector('.wp-thumb')) {
      const thumb = document.createElement('div');
      thumb.className = 'wp-thumb';
      icon.replaceWith(thumb);
    }
  } else {
    badge.textContent = '未启用';
    badge.classList.remove('on');
    badge.classList.add('off');
    sub.textContent = '让相册成为会动的壁纸';
    // 反向：thumb 换回 icon（SVG 是静态字符串，无注入风险）
    const thumb = card.querySelector('.wp-thumb');
    if (thumb) {
      const icon = document.createElement('div');
      icon.className = 'wp-icon';
      icon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="14" rx="2"/><path d="M3 18l5-5 4 4 4-4 5 5"/><circle cx="9" cy="10" r="1.5" fill="currentColor"/></svg>';
      thumb.replaceWith(icon);
    }
  }
})();
</script>
```

- [ ] **Step 4: 浏览器手动验证联动**

启动服务后：
1. 打开 `05-profile.html` → 看到"未启用"卡
2. 点 → 跳到 `08-wallpaper.html`
3. 顶栏 toggle 打开 → 看到"已暂停"灰底 0.5s
4. 选个相册（如"工作"）→ 选个 mode（每 6 小时）→ 选 display（填充）
5. 返回 `05-profile.html` → 卡片变为"启用中" + 缩略图 + "工作 / 每 6 小时 · 填充 / 下次切换 14:30"

**注意**：用浏览器原生返回按钮或顶部 ← 按钮，**不要**直接 F5 刷新 05-profile.html（不重新读 localStorage）。或者 F5 也能读，因为脚本每次加载都读 localStorage。

- [ ] **Step 5: 提交联动**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 05-profile.html 08-wallpaper.html
git commit -m "feat(wallpaper): 立即切换反馈 + 联动 05-profile 状态卡"
```

---

## Task 7: Playwright 验证 + 视觉调优

**Files:**
- Test: Playwright 截图（4 张）

- [ ] **Step 1: 启动本地服务（如未启动）**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
lsof -i :8765 -t 2>/dev/null || nohup python3 -m http.server 8765 --bind 127.0.0.1 > /tmp/wp-srv.log 2>&1 &
sleep 1
curl -s -m 3 -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8765/05-profile.html
curl -s -m 3 -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8765/08-wallpaper.html
```

期望：两个 `200`。

- [ ] **Step 2: 截图 1 · 我的页「未启用」态**

```bash
cat > /tmp/wp-1-off.mjs <<'EOF'
import { chromium } from '/Users/Gin/.npm/_npx/9833c18b2d85bc59/node_modules/playwright-core/index.mjs';
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' });
const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 2 });
const page = await ctx.newPage();
await page.goto('http://127.0.0.1:8765/05-profile.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(400);
await page.screenshot({ path: '/tmp/wp-1-off.png', fullPage: false });
await browser.close();
console.log('saved /tmp/wp-1-off.png');
EOF
node /tmp/wp-1-off.mjs
```

**注意**：用 **1280×800 视口**（不是 412×900），这是为了复现真实浏览器环境，避免之前 sheet 宽度 bug 漏检。

读图验证：VIP 卡 与「常用功能」之间有一张"壁纸引擎"卡，显示"未启用" badge + "让相册成为会动的壁纸" 副标。

- [ ] **Step 3: 截图 2 · 08-wallpaper 默认态**

```bash
cat > /tmp/wp-2-default.mjs <<'EOF'
import { chromium } from '/Users/Gin/.npm/_npx/9833c18b2d85bc59/node_modules/playwright-core/index.mjs';
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' });
const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 2 });
const page = await ctx.newPage();
// 先清 localStorage
await page.goto('http://127.0.0.1:8765/05-profile.html', { waitUntil: 'networkidle' });
await page.evaluate(() => localStorage.removeItem('__wallpaperEngine'));
await page.goto('http://127.0.0.1:8765/08-wallpaper.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(400);
await page.screenshot({ path: '/tmp/wp-2-default.png', fullPage: false });
await browser.close();
console.log('saved /tmp/wp-2-default.png');
EOF
node /tmp/wp-2-default.mjs
```

读图验证：顶栏 ← + "壁纸引擎" + 灰 toggle；Hero 卡 渐变预览 + 5 圆点（首亮）+ "来自「旅行 2025」· 第 23 张" + "下次切换 14:30"；切换方式 radio 默认"每次解锁"选中；显示方式 4 选 默认"适配"高亮；底部"立即切换下一张"按钮（灰禁用态）。

- [ ] **Step 4: 截图 3 · Sheet 弹窗**

```bash
cat > /tmp/wp-3-sheet.mjs <<'EOF'
import { chromium } from '/Users/Gin/.npm/_npx/9833c18b2d85bc59/node_modules/playwright-core/index.mjs';
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' });
const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 2 });
const page = await ctx.newPage();
await page.goto('http://127.0.0.1:8765/08-wallpaper.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(300);
await page.click('#albumRow');
await page.waitForTimeout(400);
await page.screenshot({ path: '/tmp/wp-3-sheet.png', fullPage: false });
await browser.close();
console.log('saved /tmp/wp-3-sheet.png');
EOF
node /tmp/wp-3-sheet.mjs
```

读图验证：底部 sheet 从 .device 底部滑入（**撑满 412px 宽度，不是 1280px**）；5 个相册行（我的相册 / 旅行 2025 / 家庭 / 工作 / 收藏）；"旅行 2025" 行 current 态（蓝条 + 浅蓝底 + ✓）。

- [ ] **Step 5: 截图 4 · 我的页「已启用」态**

```bash
cat > /tmp/wp-4-on.mjs <<'EOF'
import { chromium } from '/Users/Gin/.npm/_npx/9833c18b2d85bc59/node_modules/playwright-core/index.mjs';
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' });
const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 2 });
const page = await ctx.newPage();
// 注入 enabled 状态
await page.goto('http://127.0.0.1:8765/05-profile.html', { waitUntil: 'networkidle' });
await page.evaluate(() => localStorage.setItem('__wallpaperEngine', JSON.stringify({
  enabled: true, albumId: 'travel', mode: 'six-hours', display: 'fit',
  adv: { fav: false, recent: false, wifi: false, dedupe: false }
})));
await page.reload({ waitUntil: 'networkidle' });
await page.waitForTimeout(400);
await page.screenshot({ path: '/tmp/wp-4-on.png', fullPage: false });
await browser.close();
console.log('saved /tmp/wp-4-on.png');
EOF
node /tmp/wp-4-on.mjs
```

读图验证：壁纸引擎卡变为"启用中" badge + 渐变缩略图 + "旅行 2025 / 每 6 小时 · 适配 / 下次切换 14:30"。

- [ ] **Step 6: 修任何视觉问题（如果有）**

若任一截图不达预期：
- 对照 spec 视觉规范定位偏差
- 改 CSS / DOM / JS
- 重跑对应截图
- 重复直到 4 张全部通过

特别检查清单：
- [ ] Sheet 撑满 412px 宽度（不超出 .device）
- [ ] 切换方式 radio 选中态视觉清晰
- [ ] 显示方式 4 选 的渐变占位视觉差异明显
- [ ] 「已暂停」overlay 不阻挡后续操作
- [ ] 数字字间距（tabular-nums）生效
- [ ] 无控制台错误

- [ ] **Step 7: 终稿 commit**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git status
git add 05-profile.html 08-wallpaper.html
git commit -m "design(wallpaper): Playwright 验证 + 视觉调优 (终稿)"
```

---

## Self-Review Checklist

执行完成后核对：
- [ ] Spec 覆盖率：8 个 commit 任务全部覆盖 spec 的状态卡 / 详情页 / 4 个 section / 切换反馈 / 联动
- [ ] 占位符扫描：无 TBD/TODO/"implement later"
- [ ] 类型一致：`STATE` 结构在 Task 3 定义、Task 4-6 扩展、Task 6 读取均一致
- [ ] 颜色 token：只用了 `--accent` / `--fg` / `--muted` / `--surface` / `--border` / `--accent-soft`，无硬编码新色（除 ALBUMS 数据色）
- [ ] **XSS 防御**：所有 `${a.name}` / `${a.id}` / `${a.color}` 字符串插值都经过 `escapeHtml()` 包装；05-profile 联动脚本也使用 `escapeHtml()` 包装 localStorage 读出的 albumName / modeLabel / displayLabel / nextTime
- [ ] **Sheet 宽度**：sheet DOM 在 `.device` 内部（与 07-album 修复后一致），不在 `<body>` 直接子元素
- [ ] **localStorage 持久化**：刷新 05-profile / 08-wallpaper 状态都保持
- [ ] 无控制台错误（Playwright headless 跑完无 exception）
- [ ] Playwright 在 1280×800 桌面视口测试，避免之前 sheet width bug 重演

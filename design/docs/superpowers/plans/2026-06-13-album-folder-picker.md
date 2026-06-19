# Album Folder Picker 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `07-album-folder.html` 的 `.folder-path` pill 上加一个底部抽屉（Bottom Sheet），点击后弹出相册切换器，选中后切换 path 并刷新照片墙。

**Architecture:** 单文件 vanilla JS。Sheet 内容用 template literal 注入；事件委托在 `body` 上；状态用模块级变量（`currentAlbumId`）。复用 03-share 的 sheet 视觉 token（圆角 14px / grabber / 遮罩）。**安全约束：所有字符串插值（`${name}` / `${color}` 等）必须经 `escapeHtml()` 处理，因 `prompt()` 输入会写入数据并回灌到 `innerHTML`。**

**Tech Stack:** HTML5 / CSS3 / vanilla JS（无框架）/ Playwright (verify)

**Spec:** `docs/superpowers/specs/2026-06-13-album-folder-picker-design.md`

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `07-album-folder.html` | 单文件包含样式 + 标记 + 脚本 | Modify |
| `docs/superpowers/specs/2026-06-13-album-folder-picker-design.md` | 设计规格 | Read-only |

所有改动集中在一个 HTML 文件中：新增 `<style>` 块、新增 DOM 节点、新增 `<script>` 块。不动现有 CSS token 体系。

---

## Task 1: Sheet 视觉层（CSS）

**Files:**
- Modify: `07-album-folder.html`（在现有 `<style>` 末尾追加，不动现有 token）

- [ ] **Step 1: 在 `</style>` 前追加 sheet CSS**

找到 `07-album-folder.html` 现有的 `</style>` 结束标签（应在 `.ss { ... }` 块之后），在其前面追加以下样式：

```css
/* ───────── B · 文件夹切换 Sheet ───────── */
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
  border-top-left-radius: 14px;
  border-top-right-radius: 14px;
  z-index: 51;
  transform: translateY(100%);
  transition: transform 280ms cubic-bezier(0.32, 0.72, 0, 1);
  max-height: 75vh;
  display: flex; flex-direction: column;
  padding: 8px 0 16px;
  box-shadow: 0 -8px 24px -8px rgba(0,0,0,0.18);
}
.sheet-panel.open { transform: translateY(0); }

.sheet-grabber {
  width: 36px; height: 4px;
  background: var(--border);
  border-radius: 2px;
  margin: 0 auto 12px;
}

.sheet-title {
  font-size: 16px; font-weight: 600;
  letter-spacing: -0.01em;
  padding: 0 20px 12px;
}

.sheet-list {
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 0 8px;
}

.folder-row {
  display: flex; align-items: center; gap: 12px;
  height: 44px; padding: 0 20px;
  position: relative;
  cursor: pointer;
  transition: background 150ms;
}
.folder-row:hover { background: rgba(0,0,0,0.03); }
.folder-row.current {
  background: rgba(47,111,235,0.08);
}
.folder-row.current::before {
  content: ''; position: absolute; left: 0; top: 8px; bottom: 8px;
  width: 3px; background: var(--accent); border-radius: 0 2px 2px 0;
}
.folder-row .fr-icon {
  width: 22px; height: 22px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 5px;
  background: var(--accent);
  color: #fff;
}
.folder-row .fr-icon svg { width: 14px; height: 14px; }
.folder-row .fr-name { flex: 1; font-size: 14px; color: var(--fg); }
.folder-row .fr-chev { color: var(--muted); font-size: 14px; }
.folder-row .fr-check { color: var(--accent); font-size: 16px; font-weight: 600; }

.sheet-section-h {
  font-size: 11px; font-weight: 600; color: var(--muted);
  text-transform: uppercase; letter-spacing: 0.04em;
  padding: 12px 20px 6px;
}
.sheet-divider {
  height: 1px; background: var(--border);
  margin: 4px 20px;
}

.recent-chips {
  display: flex; gap: 6px;
  padding: 0 20px 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.recent-chips::-webkit-scrollbar { display: none; }
.recent-chip {
  flex-shrink: 0;
  height: 28px; padding: 0 12px;
  display: inline-flex; align-items: center; gap: 5px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 12px; font-weight: 500;
  color: var(--fg);
  cursor: pointer;
  white-space: nowrap;
}
.recent-chip .rc-dot {
  width: 8px; height: 8px; border-radius: 50%;
}

.new-folder-row {
  display: flex; align-items: center; gap: 10px;
  height: 44px; padding: 0 20px;
  color: var(--accent);
  font-size: 14px; font-weight: 500;
  cursor: pointer;
}
.new-folder-row:hover { background: rgba(0,0,0,0.03); }

.sheet-cancel {
  margin: 8px 16px 0;
  height: 44px;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 12px;
  font-size: 14px; font-weight: 500;
  color: var(--fg);
  cursor: pointer;
}
.sheet-cancel:hover { background: var(--surface); }

/* ───────── 切换加载遮罩 ───────── */
.switch-overlay {
  position: absolute; inset: 0;
  background: rgba(255,255,255,0.6);
  z-index: 40;
  display: none;
  align-items: center; justify-content: center;
  pointer-events: none;
}
.switch-overlay.active { display: flex; }
.spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(47,111,235,0.2);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ───────── 空文件夹态 ───────── */
.empty-state {
  padding: 40px 20px;
  display: flex; flex-direction: column;
  align-items: center; gap: 12px;
  text-align: center;
}
.empty-state .es-icon {
  width: 48px; height: 48px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: var(--muted);
}
.empty-state .es-icon svg { width: 22px; height: 22px; }
.empty-state .es-title { font-size: 14px; font-weight: 600; color: var(--fg); }
.empty-state .es-sub { font-size: 12px; color: var(--muted); }
.empty-state .es-cta {
  margin-top: 8px;
  height: 36px; padding: 0 16px;
  background: var(--accent); color: #fff;
  border-radius: 999px;
  font-size: 13px; font-weight: 500;
  display: inline-flex; align-items: center; gap: 6px;
  cursor: pointer;
}
```

- [ ] **Step 2: 提交样式**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 07-album-folder.html
git commit -m "design(album): 加 sheet/spinner/empty 样式 token (folder picker 准备)"
```

---

## Task 2: HTML 容器 + 数据模型

**Files:**
- Modify: `07-album-folder.html`（在 `</body>` 前插入 sheet DOM，在 `</style>` 后插入 spinner/empty DOM）

- [ ] **Step 1: 在 `</body>` 前插入 sheet 容器**

找到 `07-album-folder.html` 的 `</body>` 标签（应是最后一行附近），在其前面追加：

```html
<!-- B · 文件夹切换 Sheet -->
<div class="sheet-overlay" id="sheetOverlay" aria-hidden="true"></div>
<div class="sheet-panel" id="sheetPanel" role="dialog" aria-label="选择相册">
  <div class="sheet-grabber"></div>
  <div class="sheet-title">选择相册</div>
  <div class="sheet-list" id="sheetList"></div>
  <div class="sheet-cancel" id="sheetCancel">取消</div>
</div>

<!-- 切换加载遮罩 -->
<div class="switch-overlay" id="switchOverlay">
  <div class="spinner"></div>
</div>
```

- [ ] **Step 2: 验证 HTML 解析**

打开浏览器或运行 `node -e "const fs=require('fs'); const html=fs.readFileSync('07-album-folder.html','utf8'); console.log('sheetOverlay present:', html.includes('id=\"sheetOverlay\"')); console.log('sheetPanel present:', html.includes('id=\"sheetPanel\"')); console.log('switchOverlay present:', html.includes('id=\"switchOverlay\"'));"`

期望输出三行 `true`。

- [ ] **Step 3: 提交 DOM 容器**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 07-album-folder.html
git commit -m "design(album): 加 sheet/spinner DOM 容器 (folder picker 准备)"
```

---

## Task 3: 数据模型 + 渲染函数

**Files:**
- Modify: `07-album-folder.html`（在 `</body>` 前插入 `<script>` 块）

- [ ] **Step 1: 插入脚本块（数据 + 渲染）**

在 `</body>` 前面、在 Task 2 插入的 sheet 容器**之后**追加：

```html
<script>
(function(){
  'use strict';

  // ───── 数据：相册列表（含子目录 + 最近使用）─────
  const ALBUMS = [
    { id: 'mine',    name: '我的相册',     color: '#2F6FEB', count: 1247, recent: true,  children: [
      { id: 'mine-2025', name: '2025',     color: '#2F6FEB', count: 328 },
      { id: 'mine-2024', name: '2024',     color: '#2F6FEB', count: 612 }
    ]},
    { id: 'travel',  name: '旅行 2025',   color: '#16A34A', count: 328,  recent: true,  children: [] },
    { id: 'family',  name: '家庭',        color: '#F59E0B', count: 156,  recent: true,  children: [
      { id: 'family-baby', name: '宝宝成长', color: '#EC4899', count: 89 }
    ]},
    { id: 'work',    name: '工作',        color: '#8B5CF6', count: 47,   recent: false, children: [] },
    { id: 'fav',     name: '收藏',        color: '#EF4444', count: 23,   recent: false, children: [] }
  ];

  let currentAlbumId = 'mine';
  let recents = ['travel', 'family'];

  // ───── 工具：HTML 转义（防御 XSS，用户新建相册名走 prompt 流入模板）─────
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

  // ───── 渲染：sheet 列表 ─────
  function renderSheet() {
    const list = document.getElementById('sheetList');
    const current = findAlbum(currentAlbumId);

    // 一级相册
    let html = '';
    ALBUMS.forEach(a => {
      const isCurrent = a.id === currentAlbumId;
      const hasChildren = a.children && a.children.length > 0;
      const folderSvg = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"/></svg>';
      html += `
        <div class="folder-row${isCurrent ? ' current' : ''}" data-id="${escapeHtml(a.id)}">
          <span class="fr-icon" style="background:${escapeHtml(a.color)}">${folderSvg}</span>
          <span class="fr-name">${escapeHtml(a.name)}</span>
          ${hasChildren ? '<span class="fr-chev">›</span>' : (isCurrent ? '<span class="fr-check">✓</span>' : '')}
        </div>
      `;
      // 有子目录且当前选中：展开子项
      if (hasChildren && isCurrent) {
        a.children.forEach(c => {
          const isCurrentChild = c.id === currentAlbumId;
          html += `
            <div class="folder-row${isCurrentChild ? ' current' : ''}" data-id="${escapeHtml(c.id)}" style="padding-left:48px">
              <span class="fr-icon" style="background:${escapeHtml(c.color)};width:18px;height:18px">${folderSvg}</span>
              <span class="fr-name" style="font-size:13.5px">${escapeHtml(c.name)}</span>
              ${isCurrentChild ? '<span class="fr-check">✓</span>' : ''}
            </div>
          `;
        });
      }
    });

    // 最近使用
    const recentsHtml = recents
      .map(id => findAlbum(id))
      .filter(a => a)
      .map(a => `<div class="recent-chip" data-id="${escapeHtml(a.id)}"><span class="rc-dot" style="background:${escapeHtml(a.color)}"></span>${escapeHtml(a.name)}</div>`)
      .join('');

    // 新建相册
    const newFolderSvg = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px"><path d="M12 5v14M5 12h14"/></svg>';

    list.innerHTML = html
      + `<div class="sheet-divider"></div>`
      + `<div class="sheet-section-h">最近使用</div>`
      + `<div class="recent-chips">${recentsHtml}</div>`
      + `<div class="sheet-divider"></div>`
      + `<div class="new-folder-row" id="newFolderBtn">${newFolderSvg}<span>新建相册</span></div>`;
  }

  // 暴露给后续任务
  window.__albumPicker = { ALBUMS, findAlbum, renderSheet, getCurrent: () => currentAlbumId, setCurrent: (id) => { currentAlbumId = id; } };
})();
</script>
```

- [ ] **Step 2: 浏览器控制台验证渲染**

启动本地服务并打开页面，在 DevTools console 运行：

```js
__albumPicker.renderSheet();
document.getElementById('sheetList').children.length
```

期望：返回大于 0（具体数字 = ALBUMS.length + 子项展开数 + 1 个 divider + 1 个 section-h + 1 个 recent-chips + 1 个 divider + 1 个 new-folder-row，约 11-12）。

- [ ] **Step 3: 提交数据 + 渲染**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 07-album-folder.html
git commit -m "design(album): 加相册数据模型 + sheet 渲染函数"
```

---

## Task 4: 打开/关闭/选中交互

**Files:**
- Modify: `07-album-folder.html`（在 Task 3 的 script 块内、`window.__albumPicker` 暴露之前追加）

- [ ] **Step 1: 追加 open/close/select 逻辑**

在 Task 3 script 的 `})();` 之前（也就是 `__albumPicker` 那行**之前**）插入：

```js
  // ───── 打开 / 关闭 ─────
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

  // ───── 切换到指定相册 ─────
  function switchTo(albumId) {
    if (albumId === currentAlbumId) { closeSheet(); return; }
    const album = findAlbum(albumId);
    currentAlbumId = albumId;
    recents = [albumId, ...recents.filter(id => id !== albumId)].slice(0, 4);
    closeSheet();
    triggerSwitch(album);
  }

  // ───── 暴露切换函数（Task 5 实现 triggerSwitch）─────
  window.__albumPicker.open = openSheet;
  window.__albumPicker.close = closeSheet;
  window.__albumPicker.switchTo = switchTo;
```

然后把 Task 3 末尾的 `window.__albumPicker = { ... }` 这一行**替换**为：

```js
  // 暴露给后续任务（open/close/switchTo 在 Task 4 注入；triggerSwitch 在 Task 5 注入）
  window.__albumPicker = Object.assign(window.__albumPicker || {}, {
    ALBUMS, findAlbum, renderSheet,
    getCurrent: () => currentAlbumId,
    setCurrent: (id) => { currentAlbumId = id; }
  });
```

- [ ] **Step 2: 绑定 trigger 事件**

在 `openSheet` 函数定义之后、`closeSheet` 之前（或在 script 末尾 IIFE 闭包内），添加事件绑定：

```js
  // ───── 事件绑定 ─────
  document.querySelector('.folder-path').addEventListener('click', openSheet);
  document.getElementById('sheetOverlay').addEventListener('click', closeSheet);
  document.getElementById('sheetCancel').addEventListener('click', closeSheet);

  // 委托：folder-row / recent-chip / new-folder
  document.getElementById('sheetPanel').addEventListener('click', (e) => {
    const row = e.target.closest('.folder-row');
    if (row) { switchTo(row.dataset.id); return; }
    const chip = e.target.closest('.recent-chip');
    if (chip) { switchTo(chip.dataset.id); return; }
    if (e.target.closest('#newFolderBtn')) {
      const name = prompt('新建相册名称');
      if (name && name.trim()) {
        const newId = 'new-' + Date.now();
        ALBUMS.push({ id: newId, name: name.trim(), color: '#2F6FEB', count: 0, recent: false, children: [] });
        switchTo(newId);
      }
    }
  });
```

- [ ] **Step 3: 浏览器手动验证（5 个动作）**

启动服务后浏览器打开页面，依次验证：
1. 点击 `.folder-path` → sheet 滑入，背景遮罩渐显 ✓
2. 看到"我的相册"行有左侧蓝条 + 浅蓝底 + ✓
3. 点击"旅行 2025" → sheet 关闭，pill 文字变为"旅行 2025"
4. 再次打开 sheet → "旅行 2025"变为 current，"我的相册"变为普通
5. 点击遮罩 / 拖动 sheet / 点"取消" → sheet 关闭

- [ ] **Step 4: 提交交互**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 07-album-folder.html
git commit -m "design(album): sheet open/close + 选中切换 + 新建相册交互"
```

---

## Task 5: 切换反馈 + 空状态

**Files:**
- Modify: `07-album-folder.html`（在 Task 4 的 script 内、`switchTo` 函数中调用 `triggerSwitch` 处扩展）

- [ ] **Step 1: 实现 triggerSwitch（loading + 刷新）**

在 Task 4 的 `function switchTo` 内部、`closeSheet()` 之后、`}` 之前插入：

```js
    // 切换反馈：loading → 刷新 stats → 淡入
    const stats = document.querySelector('.folder-stats');
    const fpName = document.querySelector('.fp-name');
    const switchOverlay = document.getElementById('switchOverlay');
    const recent = document.querySelector('.recent');
    const dateGroups = document.querySelectorAll('.date-group');
    const photoGrid = document.querySelector('.photo-grid');

    if (fpName) fpName.textContent = album.name;
    if (stats) {
      stats.innerHTML = `<span><b>${album.count.toLocaleString()}</b> 张照片</span><span class="dot">·</span><span>${(album.count * 0.018).toFixed(1)} GB</span>`;
    }

    switchOverlay.classList.add('active');
    if (recent) recent.style.opacity = '0.4';

    setTimeout(() => {
      // 模拟"空文件夹"逻辑（count === 0 → 替换为 empty state）
      if (album.count === 0) {
        if (recent) {
          recent.innerHTML = `
            <div class="empty-state">
              <div class="es-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"/><circle cx="12" cy="13" r="3.5"/></svg>
              </div>
              <div class="es-title">这里还没有照片</div>
              <div class="es-sub">上传第一张照片开始建立你的相册</div>
              <div class="es-cta">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:14px;height:14px"><path d="M12 5v14M5 12h14"/></svg>
                <span>上传照片</span>
              </div>
            </div>
          `;
        }
      } else {
        // 重置为默认照片墙（如果之前是 empty state）
        if (recent && !recent.querySelector('.date-group')) {
          // 不强制恢复（避免大段 HTML 复写），仅做淡入
        }
      }
      switchOverlay.classList.remove('active');
      if (recent) recent.style.opacity = '';
    }, 350);
```

并在 Task 4 的 `switchTo` 函数中，把 `triggerSwitch(album);` 这行替换为 `triggerSwitch(album); function triggerSwitch(album) {` 闭合大括号——不，更简洁的做法是**把 `triggerSwitch` 单独定义**，然后在 `switchTo` 内调用。

为避免重复大括号，把上面那段代码包装为独立函数并插在 `switchTo` 函数定义之前：

```js
  // ───── 切换反馈 ─────
  function triggerSwitch(album) {
    // （上面那段完整逻辑原样放入此处）
  }
```

- [ ] **Step 2: 把 triggerSwitch 暴露**

在 Task 4 的 `__albumPicker` 注入处追加：

```js
    open: openSheet, close: closeSheet, switchTo: switchTo, triggerSwitch: triggerSwitch
```

具体来说，把 `window.__albumPicker = Object.assign(...)` 那行扩展为：

```js
  window.__albumPicker = Object.assign(window.__albumPicker || {}, {
    ALBUMS, findAlbum, renderSheet,
    getCurrent: () => currentAlbumId,
    setCurrent: (id) => { currentAlbumId = id; },
    open: openSheet, close: closeSheet, switchTo: switchTo
  });
```

- [ ] **Step 3: 浏览器手动验证**

启动服务后：
1. 打开 sheet → 点"收藏"（count: 23，验证 stats 数字变 23 张）
2. 再次打开 → 新建相册"测试"（count: 0，验证空状态出现）
3. 关闭 → 重新打开 → 验证"测试"在列表中

- [ ] **Step 4: 提交**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git add 07-album-folder.html
git commit -m "design(album): 切换 loading + stats 刷新 + 空状态"
```

---

## Task 6: Playwright 验证 + 终稿

**Files:**
- Modify: `07-album-folder.html`（如发现视觉问题再调整）
- Test: Playwright 截图（3 张）

- [ ] **Step 1: 启动本地服务（如未启动）**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
lsof -i :8765 -t 2>/dev/null || nohup python3 -m http.server 8765 --bind 127.0.0.1 > /tmp/album-srv.log 2>&1 &
sleep 1
curl -s -m 3 -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8765/07-album-folder.html
```

期望：`200`

- [ ] **Step 2: 截图 1 · 默认态**

```bash
node /tmp/snap.mjs http://127.0.0.1:8765/07-album-folder.html /tmp/album-1-default.png
```

读图验证：folder pill "我的相册"，sub-tabs 完整，照片墙正常。

- [ ] **Step 3: 截图 2 · Sheet 打开**

```bash
cat > /tmp/album-sheet.mjs <<'EOF'
import { chromium } from '/Users/Gin/.npm/_npx/9833c18b2d85bc59/node_modules/playwright-core/index.mjs';
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' });
const ctx = await browser.newContext({ viewport: { width: 412, height: 900 }, deviceScaleFactor: 2 });
const page = await ctx.newPage();
await page.goto('http://127.0.0.1:8765/07-album-folder.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(300);
await page.click('.folder-path');
await page.waitForTimeout(400);
await page.screenshot({ path: '/tmp/album-2-sheet.png', fullPage: false });
await browser.close();
console.log('saved /tmp/album-2-sheet.png');
EOF
node /tmp/album-sheet.mjs
```

读图验证：sheet 滑入，"我的相册"行 current 高亮（蓝条 + 浅蓝底 + ✓），下方"旅行 2025/家庭/工作/收藏"列表，最近使用 chip 横向滚动，新建相册行，底部"取消"按钮。

- [ ] **Step 4: 截图 3 · 切换后**

```bash
cat > /tmp/album-switch.mjs <<'EOF'
import { chromium } from '/Users/Gin/.npm/_npx/9833c18b2d85bc59/node_modules/playwright-core/index.mjs';
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' });
const ctx = await browser.newContext({ viewport: { width: 412, height: 900 }, deviceScaleFactor: 2 });
const page = await ctx.newPage();
await page.goto('http://127.0.0.1:8765/07-album-folder.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(300);
await page.click('.folder-path');
await page.waitForTimeout(400);
await page.click('.folder-row[data-id="travel"]');
await page.waitForTimeout(600);
await page.screenshot({ path: '/tmp/album-3-switched.png', fullPage: false });
await browser.close();
console.log('saved /tmp/album-3-switched.png');
EOF
node /tmp/album-switch.mjs
```

读图验证：pill 文字变为"旅行 2025"，stats 显示"328 张照片"等。

- [ ] **Step 5: 修任何视觉问题（如果有）**

若任一截图不达预期：
- 对照 spec 视觉规范定位偏差
- 改 CSS / DOM / JS
- 重跑对应截图
- 重复直到 3 张全部通过

- [ ] **Step 6: 终稿 commit**

```bash
cd "/Users/Gin/Library/Application Support/Open Design/namespaces/release-stable/data/projects/c871cc9f-31c1-48d0-9dd3-3bcbc4d90266"
git status
git add 07-album-folder.html
git commit -m "design(album): 07-album-folder 加 sheet 切换相册交互 (终稿)"
```

---

## Self-Review Checklist

执行完成后核对：

- [ ] Spec 覆盖率：sheet 样式 / DOM / 渲染 / open-close / 选中 / 切换 / loading / 空状态 → 全部有 task
- [ ] 占位符扫描：无 TBD/TODO/"implement later"
- [ ] 类型一致：`__albumPicker` 的 API 在 Task 3 定义、Task 4 扩展、Task 5 调用均一致
- [ ] 颜色 token：只用了 `--accent` / `--fg` / `--muted` / `--surface` / `--border` / `--bg-secondary`，无硬编码新色（除 SPEC 中允许的 #16A34A/#F59E0B 等数据色）
- [ ] **XSS 防御**：所有 `${a.name}` / `${a.id}` / `${a.color}` / `${c.name}` 等字符串插值都经过 `escapeHtml()` 包装；`prompt()` 输入的相册名经 `escapeHtml()` 后再插入模板
- [ ] 无控制台错误（Playwright headless 跑完无 exception）


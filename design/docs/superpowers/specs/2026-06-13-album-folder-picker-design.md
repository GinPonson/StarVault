# 设计规格：相册文件夹选择器交互

**日期**：2026-06-13
**文件**：`07-album-folder.html`
**状态**：已批准，进入实现

## 目标

让 `07-album-folder.html` 中的 `.folder-path` pill（"📁 我的相册 ▾"）从静态元素变为可交互的文件夹切换器：点击后弹出底部抽屉，列出可切换的相册，选中后切换当前路径并刷新照片墙。

## 选型结论

**A · 底部抽屉（Bottom Sheet）**

- 与 03-share 已有 sheet 模式保持一致
- iOS 用户手势原生
- 支持二级目录（push 新 sheet）
- 切换成本低（1 tap 关闭）

## 触发态

- `.folder-path` hover/press：背景 `var(--surface)` → `rgba(0,0,0,0.05)`，150ms
- `.fp-chev` 旋转：idle 向下 → 打开时向上 180°，200ms

## Sheet 动效

| 维度 | 规范 |
|---|---|
| 进入 | 从底滑入，280ms `cubic-bezier(0.32, 0.72, 0, 1)` |
| 退出 | 下滑关闭，240ms |
| 遮罩 | `rgba(0,0,0,0.4)` fade-in 200ms |
| 高度 | 自适应，最小 320px，最大 75vh |
| 圆角 | 顶部 14px |
| Drag handle | 顶部居中 36×4px `#d1d1d6` |
| 可拖拽关闭 | 下拉 > 30% 自动 dismiss |

## Sheet 内容

```
[drag handle] 8px
"选择相册"  16px / 600 / 左对齐
─────────────────
📁 我的相册         ✓   ← 当前（accent 左 border + 浅蓝底）
📁 旅行 2025
📁 家庭            ›   ← 有子目录
📁 工作
─────────────────
最近使用            12px / 500 / muted
[旅行 2025] [家庭] [...]   横向滚动 chip
─────────────────
+ 新建相册          accent 色
─────────────────
       [ 取消 ]     灰色文字按钮
```

### 视觉规范

- 列表行高 44px（HIG）
- 选中态：左 3px accent border + 整行 `rgba(47,111,235,0.08)` 底
- 复选 ✓ 16px accent，靠右
- chevron › 12px muted

## Tap 行为

| 元素 | 行为 |
|---|---|
| 选中相册行 | 关闭 sheet + 切换 path + 照片墙 200ms 淡入 |
| 有子目录行 | push 第二层 sheet（缩放 280ms）|
| 最近使用 chip | 关闭 sheet + 切换 |
| + 新建相册 | 关闭 sheet + prompt 输入名称 |
| 取消 / 遮罩 / drag-down | 关闭 sheet，状态不变 |

## 切换反馈

- 切换中：照片墙 `rgba(0,0,0,0.04)` 遮罩 + 居中 16px cobalt spinner
- 切换完成：照片墙 200ms 淡入
- 数字过渡：旧 → 新 tabular-nums，150ms

## 状态覆盖

- **空文件夹**：「这里还没有照片」+ 「+ 上传照片」CTA
- **加载中**：spinner overlay
- **错误**：iOS-style alert（cobalt action）

## 设计系统对齐

- 复用 03-share 的 sheet 模式（drag handle / 遮罩 / 圆角 14px）
- 列表行 44px 与 06-files 一致
- 颜色严格走 `--accent` / `--fg` / `--muted` / `--surface`，不引入新色
- Spinner 用现有 token，不新建

## 不在范围

- 多选模式 / 长按手势
- 照片 lightbox 浏览
- 拖拽排序
- 云端 vs 本地区分

## 实现约束

- 纯 vanilla JS（项目无框架）
- 数据硬编码在 `<script>` 内的常量数组
- Sheet 内容用 template literal 注入
- 不引外部资源
- 不修改 CSS token 体系

## 验证

1. Playwright 截图 3 个态：默认 / sheet 打开 / 切换后
2. 视觉与 03-share sheet 风格一致
3. 无控制台错误
4. Commit：`design(album): 07-album-folder 加 sheet 切换相册交互`

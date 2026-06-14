#!/usr/bin/env bash
#
# scripts/capture-design-baseline.sh
#
# 用 Playwright 把 design/*.html 渲染成 412×900 PNG baseline，便于与
# app/src/test/snapshots/images/*.png (Paparazzi 输出) 1:1 像素对比。
#
# 前置：
#   npm i -g playwright
#   npx playwright install chromium
#
# 用法：
#   bash scripts/capture-design-baseline.sh                # 输出到 ./design-baseline/
#   bash scripts/capture-design-baseline.sh /tmp/baseline  # 输出到指定目录
#
# 设计稿 viewport 412×900 (CSS pixel = dp) 与 Paparazzi PHONE_412_900 一致。

set -euo pipefail

OUT_DIR="${1:-$(cd "$(dirname "$0")/.." && pwd)/design-baseline}"
WIDTH=412
HEIGHT=900

# 解析 design/ 软链（指向 Open Design 私有目录）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DESIGN_DIR="$PROJECT_ROOT/design"

if [ ! -d "$DESIGN_DIR" ]; then
  echo "ERROR: $DESIGN_DIR not found. design/ 应为 Open Design 项目目录的 symlink." >&2
  exit 1
fi

# 检查 npx
if ! command -v npx >/dev/null 2>&1; then
  echo "ERROR: npx not found. 请先安装 Node.js (https://nodejs.org/)" >&2
  exit 1
fi

# 检查 chromium
if ! npx playwright --version >/dev/null 2>&1; then
  echo "Playwright 未安装。正在执行: npm i -g playwright && npx playwright install chromium" >&2
  npm i -g playwright
  npx playwright install chromium
fi

mkdir -p "$OUT_DIR"

count=0
for html in "$DESIGN_DIR"/*.html; do
  [ -f "$html" ] || continue
  name=$(basename "$html" .html)
  out="$OUT_DIR/${name}.png"
  echo "Capturing $name ..."
  npx playwright screenshot \
    --viewport-size="${WIDTH},${HEIGHT}" \
    --full-page \
    "file://$html" \
    "$out" >/dev/null
  count=$((count + 1))
done

echo ""
echo "✓ Done. $count design HTML(s) captured to $OUT_DIR/"
ls -la "$OUT_DIR"

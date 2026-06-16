#!/usr/bin/env bash
#
# Wallpaper Client Linux AppImage 安装/卸载脚本
#
# 设计目标：
# - 使用用户级安装目录，不需要 sudo。
# - 安装前自动构建 Linux release AppImage。
# - 安装 AppImage、桌面启动器和应用图标。
# - 支持卸载，并且只删除本脚本管理的文件。
# - 默认使用自动构建后最新的 Tauri AppImage。
#
# 常用命令：
#   ./scripts/linux-appimage.sh install
#   ./scripts/linux-appimage.sh uninstall
#   ./scripts/linux-appimage.sh status
#

set -Eeuo pipefail

APP_NAME="Wallpaper Client"
APP_ID="com.liut.wallpaper"
APPIMAGE_NAME="wallpaper-client.AppImage"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# 遵循 XDG 目录规范；如果环境变量未设置，则使用常见默认值。
BIN_DIR="${XDG_BIN_HOME:-"$HOME/.local/bin"}"
DATA_DIR="${XDG_DATA_HOME:-"$HOME/.local/share"}"
APPLICATIONS_DIR="$DATA_DIR/applications"
ICON_DIR="$DATA_DIR/icons/hicolor/128x128/apps"

APPIMAGE_DEST="$BIN_DIR/$APPIMAGE_NAME"
DESKTOP_FILE="$APPLICATIONS_DIR/$APP_ID.desktop"
ICON_SOURCE="$PROJECT_ROOT/src-tauri/icons/128x128.png"
ICON_DEST="$ICON_DIR/$APP_ID.png"

usage() {
  cat <<EOF
用法：
  $0 install                自动构建并安装用户级 AppImage
  $0 uninstall              卸载 AppImage、桌面启动器和图标
  $0 status                 查看当前安装状态
  $0 help                   显示帮助

说明：
  install 不传 AppImage 路径时，会先自动运行：
    npm run tauri -- build

  然后安装最新的构建产物：
    $PROJECT_ROOT/src-tauri/target/release/bundle/**/*.AppImage
EOF
}

info() {
  printf '[信息] %s\n' "$*"
}

warn() {
  printf '[警告] %s\n' "$*" >&2
}

die() {
  printf '[错误] %s\n' "$*" >&2
  exit 1
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

ensure_parent_dirs() {
  mkdir -p "$BIN_DIR" "$APPLICATIONS_DIR" "$ICON_DIR"
}

build_appimage() {
  info "开始构建 Linux release AppImage..."
  info "构建命令：npm run tauri -- build"

  command_exists npm || die "未找到 npm，请先安装 Node.js/npm。"

  (
    cd "$PROJECT_ROOT"
    npm run tauri -- build
  )

  info "构建完成。"
}

find_latest_appimage() {
  local bundle_dir="$PROJECT_ROOT/src-tauri/target/release/bundle"

  [[ -d "$bundle_dir" ]] || return 1

  # GNU find 的 -printf 可按修改时间排序；Arch Linux 默认可用。
  find "$bundle_dir" -type f -name '*.AppImage' -printf '%T@ %p\n' \
    | sort -nr \
    | sed -n '1s/^[^ ]* //p'
}

absolute_path() {
  local path="$1"
  [[ -e "$path" ]] || die "路径不存在：$path"
  cd "$(dirname "$path")"
  printf '%s/%s\n' "$(pwd -P)" "$(basename "$path")"
}

resolve_appimage_for_install() {
  local source_path

  build_appimage
  source_path="$(find_latest_appimage || true)"

  [[ -n "$source_path" ]] || die "构建完成后仍未找到 AppImage，请检查 Tauri Linux bundle 配置。"
  source_path="$(absolute_path "$source_path")"
  [[ -f "$source_path" ]] || die "AppImage 不是文件：$source_path"
  printf '%s\n' "$source_path"
}

install_appimage() {
  local source_path
  source_path="$(resolve_appimage_for_install)"

  ensure_parent_dirs

  info "安装 AppImage：$APPIMAGE_DEST"
  cp "$source_path" "$APPIMAGE_DEST"
  chmod 0755 "$APPIMAGE_DEST"

  if [[ -f "$ICON_SOURCE" ]]; then
    info "安装图标：$ICON_DEST"
    cp "$ICON_SOURCE" "$ICON_DEST"
    chmod 0644 "$ICON_DEST"
  else
    warn "未找到图标源文件：$ICON_SOURCE"
  fi

  write_desktop_file
  refresh_desktop_database

  info "安装完成。可以从应用菜单启动：$APP_NAME"
}

write_desktop_file() {
  info "写入桌面启动器：$DESKTOP_FILE"

  cat >"$DESKTOP_FILE" <<EOF
[Desktop Entry]
Type=Application
Name=$APP_NAME
Comment=Bing wallpaper client
Exec=$APPIMAGE_DEST
Icon=$APP_ID
Terminal=false
Categories=Utility;Graphics;
StartupNotify=true
EOF

  chmod 0644 "$DESKTOP_FILE"
}

refresh_desktop_database() {
  # 这些刷新命令不是所有系统都有；不存在时跳过即可。
  if command_exists update-desktop-database; then
    update-desktop-database "$APPLICATIONS_DIR" >/dev/null 2>&1 || true
  fi

  if command_exists gtk-update-icon-cache; then
    gtk-update-icon-cache "$DATA_DIR/icons/hicolor" >/dev/null 2>&1 || true
  fi
}

uninstall_appimage() {
  local removed=0

  for path in "$APPIMAGE_DEST" "$DESKTOP_FILE" "$ICON_DEST"; do
    if [[ -e "$path" || -L "$path" ]]; then
      info "删除：$path"
      rm -f "$path"
      removed=1
    fi
  done

  refresh_desktop_database

  if [[ "$removed" -eq 1 ]]; then
    info "卸载完成。"
  else
    info "没有发现已安装文件。"
  fi
}

show_status() {
  printf '应用名称：%s\n' "$APP_NAME"
  printf 'AppImage：%s\n' "$APPIMAGE_DEST"
  if [[ -x "$APPIMAGE_DEST" ]]; then
    printf '  状态：已安装，可执行\n'
  elif [[ -e "$APPIMAGE_DEST" ]]; then
    printf '  状态：已安装，但不可执行\n'
  else
    printf '  状态：未安装\n'
  fi

  printf '桌面启动器：%s\n' "$DESKTOP_FILE"
  [[ -f "$DESKTOP_FILE" ]] && printf '  状态：存在\n' || printf '  状态：不存在\n'

  printf '图标：%s\n' "$ICON_DEST"
  [[ -f "$ICON_DEST" ]] && printf '  状态：存在\n' || printf '  状态：不存在\n'
}

main() {
  local command_name="${1:-help}"
  shift || true

  case "$command_name" in
    install)
      if [[ "$#" -gt 0 ]]; then
        die "install 不接受 AppImage 路径；请直接运行：$0 install"
      fi
      install_appimage
      ;;
    uninstall)
      uninstall_appimage
      ;;
    status)
      show_status
      ;;
    help|-h|--help)
      usage
      ;;
    *)
      usage
      die "未知命令：$command_name"
      ;;
  esac
}

main "$@"

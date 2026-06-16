# Wallora

Wallora is a Tauri 2 wallpaper client with a vanilla JavaScript frontend, Rust shared logic, and an Android native wallpaper bridge.

## Documentation

- [Work report](docs/WORK_REPORT.md)
- [Build instructions](docs/BUILD.md)

## Quick Checks

```bash
cargo test -p wallpaper-core
node --check src/main.js
npm run tauri -- android build --debug
```

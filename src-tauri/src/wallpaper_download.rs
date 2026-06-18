use std::fs;
use std::path::{Path, PathBuf};

/// 本地壁纸文件的来源，用于调用方区分“复用缓存”和“新下载”。
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum LocalWallpaperFile {
    /// 目标文件已存在且通过基础有效性检查，本次未发起网络下载。
    Reused(PathBuf),
    /// 本次从网络下载，并在校验完成后移动到目标路径。
    Downloaded(PathBuf),
}

impl LocalWallpaperFile {
    /// 返回可交给平台层设置壁纸或保存相册的本地文件路径。
    pub fn path(&self) -> &Path {
        match self {
            Self::Reused(path) | Self::Downloaded(path) => path,
        }
    }
}

/// 下载壁纸到指定目标文件；若目标文件已存在且非空，则直接复用。
///
/// 该函数是桌面端和 Android 端共用的下载入口：
/// - 下载前先检查目标文件，避免重复请求网络；
/// - 网络响应写入同目录临时文件；
/// - 校验 `Content-Length` 与实际字节数一致后再移动到目标路径。
pub async fn download_wallpaper_file(
    url: &str,
    target: &Path,
) -> Result<LocalWallpaperFile, String> {
    if let Some(path) = reusable_wallpaper_file(target)? {
        return Ok(LocalWallpaperFile::Reused(path));
    }

    const MAX_RETRIES: u32 = 3;
    let mut last_err = String::new();
    for attempt in 0..MAX_RETRIES {
        match try_download_file_once(url, target).await {
            Ok(()) => return Ok(LocalWallpaperFile::Downloaded(target.to_path_buf())),
            Err(err) => {
                last_err = err;
                if attempt + 1 < MAX_RETRIES {
                    eprintln!(
                        "Download attempt {}/{MAX_RETRIES} failed, retrying...",
                        attempt + 1
                    );
                }
            }
        }
    }

    Err(format!(
        "Download failed after {MAX_RETRIES} attempts: {last_err}"
    ))
}

/// 检查目标文件是否可直接复用。
///
/// 目前的基础完整性标准是“文件存在且长度大于 0”。下载过程中会做
/// `Content-Length` 校验；已存在文件无法可靠还原响应头，因此只做本地基础检查。
pub fn reusable_wallpaper_file(target: &Path) -> Result<Option<PathBuf>, String> {
    match fs::metadata(target) {
        Ok(metadata) if metadata.is_file() && metadata.len() > 0 => Ok(Some(target.to_path_buf())),
        Ok(_) => Ok(None),
        Err(err) if err.kind() == std::io::ErrorKind::NotFound => Ok(None),
        Err(err) => Err(format!("Failed to inspect wallpaper file: {err}")),
    }
}

async fn try_download_file_once(url: &str, target: &Path) -> Result<(), String> {
    let response = reqwest::get(url)
        .await
        .map_err(|err| format!("Failed to download wallpaper: {err}"))?
        .error_for_status()
        .map_err(|err| format!("Wallpaper download returned an error: {err}"))?;
    let expected_len = response.content_length();
    let bytes = response
        .bytes()
        .await
        .map_err(|err| format!("Failed to read wallpaper bytes: {err}"))?;

    persist_checked_download(target, bytes.as_ref(), expected_len)
}

/// 将下载结果先写入临时文件，校验完成后再移动到目标文件。
///
/// 调用方传入的 `expected_len` 来自 HTTP `Content-Length`。当服务器未提供该响应头时，
/// 只校验文件非空；提供时必须与实际字节数完全一致，否则删除临时文件并报错。
pub fn persist_checked_download(
    target: &Path,
    bytes: &[u8],
    expected_len: Option<u64>,
) -> Result<(), String> {
    if bytes.is_empty() {
        return Err("Downloaded wallpaper is empty".to_string());
    }
    if let Some(expected) = expected_len {
        let actual = bytes.len() as u64;
        if actual != expected {
            return Err(format!(
                "Downloaded wallpaper Content-Length mismatch: expected {expected}, got {actual}"
            ));
        }
    }

    let parent = target
        .parent()
        .ok_or_else(|| "Wallpaper target has no parent directory".to_string())?;
    fs::create_dir_all(parent)
        .map_err(|err| format!("Failed to create wallpaper directory: {err}"))?;

    let temp_path = temporary_download_path(target)?;
    fs::write(&temp_path, bytes)
        .map_err(|err| format!("Failed to write temporary wallpaper file: {err}"))?;

    // 下载期间若另一个任务已经生成了目标文件，优先复用已有文件，避免重复覆盖。
    if reusable_wallpaper_file(target)?.is_some() {
        let _ = fs::remove_file(&temp_path);
        return Ok(());
    }
    if target.exists() {
        fs::remove_file(target)
            .map_err(|err| format!("Failed to remove invalid wallpaper file: {err}"))?;
    }

    fs::rename(&temp_path, target)
        .map_err(|err| format!("Failed to move wallpaper into place: {err}"))
}

/// 将临时壁纸文件移动到持久目标；若目标已存在且可复用，则删除临时文件并复用目标。
///
/// 桌面端开启“保存到文件系统”时使用该函数完成“临时下载 → 图片目录”的搬运。
/// 这样网络下载始终固定落在临时缓存目录，持久目录只接收已完整下载的文件。
#[cfg(not(target_os = "android"))]
pub fn move_temporary_wallpaper_to_target(
    temporary_path: &Path,
    target: &Path,
) -> Result<PathBuf, String> {
    if let Some(path) = reusable_wallpaper_file(target)? {
        let _ = fs::remove_file(temporary_path);
        return Ok(path);
    }
    if reusable_wallpaper_file(temporary_path)?.is_none() {
        return Err("Temporary wallpaper file is missing or empty".to_string());
    }

    let parent = target
        .parent()
        .ok_or_else(|| "Wallpaper target has no parent directory".to_string())?;
    fs::create_dir_all(parent)
        .map_err(|err| format!("Failed to create wallpaper directory: {err}"))?;

    fs::rename(temporary_path, target)
        .map_err(|err| format!("Failed to move wallpaper into place: {err}"))?;
    Ok(target.to_path_buf())
}

fn temporary_download_path(target: &Path) -> Result<PathBuf, String> {
    let file_name = target
        .file_name()
        .and_then(|name| name.to_str())
        .ok_or_else(|| "Wallpaper target has no valid file name".to_string())?;
    Ok(target.with_file_name(format!("{file_name}.download")))
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::tempdir;

    #[test]
    fn existing_non_empty_target_file_is_reused_without_downloading() {
        let dir = tempdir().unwrap();
        let target = dir.path().join("wallora.jpg");
        std::fs::write(&target, b"cached").unwrap();

        let result = reusable_wallpaper_file(&target).unwrap();

        assert_eq!(result, Some(target));
    }

    #[test]
    fn completed_download_rejects_mismatched_content_length() {
        let dir = tempdir().unwrap();
        let target = dir.path().join("wallora.jpg");

        let result = persist_checked_download(&target, b"abc", Some(4));

        assert!(result.unwrap_err().contains("Content-Length"));
        assert!(!target.exists());
    }

    #[test]
    fn temporary_file_moves_to_persistent_target_after_download() {
        let dir = tempdir().unwrap();
        let temporary_path = dir.path().join("temp").join("wallora.jpg");
        let target = dir
            .path()
            .join("Pictures")
            .join("Wallora")
            .join("wallora.jpg");
        std::fs::create_dir_all(temporary_path.parent().unwrap()).unwrap();
        std::fs::write(&temporary_path, b"complete").unwrap();

        let result = move_temporary_wallpaper_to_target(&temporary_path, &target).unwrap();

        assert_eq!(result, target);
        assert!(!temporary_path.exists());
        assert_eq!(std::fs::read(&target).unwrap(), b"complete");
    }
}

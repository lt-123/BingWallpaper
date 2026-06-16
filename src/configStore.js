import { BING_PAGE_SIZE } from "./galleryPager.js";

const STORAGE_KEY = "wallpaper.config";

/**
 * 从 localStorage 读取持久化配置。
 * 若缺少一级必要字段（bing / schedule / platform），视为 schema 不兼容，返回 null，
 * 由调用方回退到 default_config，避免 writeConfigToForm 访问 undefined 子对象时崩溃。
 * @returns {object|null} 配置对象，或 null（首次运行 / 无缓存 / schema 不兼容）
 */
export function loadStoredConfig() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const config = JSON.parse(raw);
    if (!config?.bing || !config?.schedule || !config?.platform) return null;
    return config;
  } catch {
    return null;
  }
}

/**
 * 将配置对象序列化后写入 localStorage。
 * @param {object} config 完整的 AppConfig 对象
 */
export function saveStoredConfig(config) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
}

/**
 * 将配置对象的各字段写回到表单控件。
 * 调用前需确保 config 的结构与当前 AppConfig schema 完全匹配；
 * 若从 localStorage 加载的旧版配置缺少 bing / platform 等子对象，
 * 此函数会抛出 TypeError，应在调用前做校验或直接使用 default_config 回退。
 *
 * @param {Record<string, HTMLElement>} elements 通过 bindElements 填充的 DOM 元素映射
 * @param {object} config AppConfig 对象
 */
export function writeConfigToForm(elements, config) {
  elements.market.value = config.bing.market;
  elements.resolution.value = config.bing.resolution;
  elements.fitMode.value = config.fit_mode;
  elements.scheduleEnabled.checked = config.schedule.enabled;
  elements.intervalMinutes.value = config.schedule.interval_minutes;
  elements.notifyBackground.checked =
    config.schedule.notify_on_background_update;
  elements.saveToFileSystem.checked = config.save_to_file_system;
  elements.setLockScreen.checked = config.platform.set_lock_screen;
}

/**
 * 从表单控件读取当前用户输入，构造完整的 AppConfig 对象。
 *
 * count 固定为 BING_PAGE_SIZE：画廊分页尺寸不对用户开放配置，
 * 避免与 loadGallery 的分页逻辑不一致。
 *
 * notify_on_manual_update 暂无对应 UI 控件，固定为 true（手动应用时始终触发通知）。
 * 若未来需要可配置，在 index.html 添加对应开关后修改此处。
 *
 * @param {Record<string, HTMLElement>} elements 通过 bindElements 填充的 DOM 元素映射
 * @returns {object} 完整的 AppConfig 对象
 */
/**
 * 从表单控件读取当前用户输入，构造完整的 AppConfig 对象。
 *
 * count 固定为 BING_PAGE_SIZE：画廊分页尺寸不对用户开放配置，
 * 避免与 loadGallery 的分页逻辑不一致。
 *
 * notify_on_manual_update 暂无对应 UI 控件，从 previousConfig 继承原值，
 * 首次使用时默认 true（手动应用时触发通知）。
 * 若未来需要可配置，在 index.html 添加对应开关后修改此处。
 *
 * @param {Record<string, HTMLElement>} elements 通过 bindElements 填充的 DOM 元素映射
 * @param {object|null} previousConfig 当前 state.config，用于保留无 UI 控件的字段值
 * @returns {object} 完整的 AppConfig 对象
 */
export function readConfigFromForm(elements, previousConfig = null) {
  return {
    bing: {
      market: elements.market.value,
      resolution: elements.resolution.value,
      count: BING_PAGE_SIZE,
    },
    fit_mode: elements.fitMode.value,
    schedule: {
      enabled: elements.scheduleEnabled.checked,
      interval_minutes: Number(elements.intervalMinutes.value),
      notify_on_background_update: elements.notifyBackground.checked,
    },
    save_to_file_system: elements.saveToFileSystem.checked,
    platform: {
      set_lock_screen: elements.setLockScreen.checked,
      // 保留历史值；暂无 UI 控件时默认 true
      notify_on_manual_update: previousConfig?.platform?.notify_on_manual_update ?? true,
    },
  };
}

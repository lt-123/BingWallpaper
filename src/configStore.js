import { BING_PAGE_SIZE } from "./galleryPager.js";

const STORAGE_KEY = "wallpaper.config";

/**
 * 从 localStorage 读取持久化配置。
 * 若缺少一级必要字段（bing / schedule / platform），视为 schema 不兼容，返回 null，
 * 由调用方回退到 default_config，避免 writeConfigToForm 访问 undefined 子对象时崩溃。
 *
 * 新字段（mode / daily_times）由 writeConfigToForm 用 ?? 回退到默认值，
 * 因此旧版本存储的配置在缺少这些字段时仍可正常加载。
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
  // mode / daily_times 是新字段，旧版缓存可能缺失，用 ?? 回退到默认值
  elements.scheduleMode.value = config.schedule.mode ?? "Interval";
  elements.intervalMinutes.value = config.schedule.interval_minutes;
  renderDailyTimesTags(elements.dailyTimesTags, config.schedule.daily_times ?? []);
  updateScheduleModeVisibility(elements);
  elements.notifyBackground.checked =
    config.schedule.notify_on_background_update;
  elements.saveToFileSystem.checked = config.save_to_file_system;
  elements.setLockScreen.checked = config.platform.set_lock_screen;
}

/**
 * 根据 scheduleMode 的值显示或隐藏对应的配置行。
 * @param {Record<string, HTMLElement>} elements
 */
export function updateScheduleModeVisibility(elements) {
  const isDailyAt = elements.scheduleMode.value === "DailyAt";
  elements.rowIntervalMinutes.hidden = isDailyAt;
  elements.rowDailyTimes.hidden = !isDailyAt;
}

/**
 * 将 daily_times 数组渲染为 tag 列表。
 * 每个 tag 含时间文字和删除按钮，点击删除后重新同步配置并触发 change 事件。
 * @param {HTMLElement} container
 * @param {string[]} times
 */
export function renderDailyTimesTags(container, times) {
  container.replaceChildren();
  for (const time of times) {
    const tag = document.createElement("span");
    tag.className = "daily-time-tag";
    tag.dataset.time = time;

    const label = document.createElement("span");
    label.textContent = time;

    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.setAttribute("aria-label", `删除 ${time}`);
    removeBtn.textContent = "×";
    removeBtn.addEventListener("click", () => {
      tag.remove();
      // 触发 settings form 的 change 事件，让 main.js 同步配置
      container.closest("form")?.dispatchEvent(new Event("change", { bubbles: true }));
    });

    tag.append(label, removeBtn);
    container.append(tag);
  }
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
      mode: elements.scheduleMode.value,
      interval_minutes: Number(elements.intervalMinutes.value),
      daily_times: readDailyTimesFromTags(elements.dailyTimesTags),
      notify_on_background_update: elements.notifyBackground.checked,
    },
    save_to_file_system: elements.saveToFileSystem.checked,
    platform: {
      set_lock_screen: elements.setLockScreen.checked,
      notify_on_manual_update: previousConfig?.platform?.notify_on_manual_update ?? true,
    },
  };
}

/**
 * 从 dailyTimesTags 容器中读出当前所有时间点字符串。
 * @param {HTMLElement} container
 * @returns {string[]}
 */
function readDailyTimesFromTags(container) {
  return Array.from(container.querySelectorAll(".daily-time-tag[data-time]"))
    .map((tag) => tag.dataset.time)
    .filter(Boolean);
}

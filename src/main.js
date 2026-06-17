import {
  loadStoredConfig,
  readConfigFromForm,
  renderDailyTimesTags,
  saveStoredConfig,
  updateScheduleModeVisibility,
  writeConfigToForm,
} from "./configStore.js";
import {
  appendUniqueWallpapers,
  BING_PAGE_SIZE,
  createInitialPager,
  nextPagerAfterLoad,
} from "./galleryPager.js";

// 在 Tauri 运行时中使用真实的 invoke，浏览器开发环境回退到 mockInvoke
const invoke = window.__TAURI__?.core?.invoke ?? mockInvoke;

// 应用全局可变状态，所有异步操作都读写此对象
const state = {
  config: null,    // AppConfig，从 localStorage 或 default_config 初始化
  gallery: [],     // 当前已加载的壁纸列表
  pager: null,     // 分页状态，null 表示尚未初始化
  selected: null,  // 当前选中的壁纸，用于预览和应用
};

// DOM 元素缓存，由 bindElements 在 DOMContentLoaded 后填充
const elements = {};

// 开发模式占位图：内联 SVG 渐变风景，避免外部网络请求
const MOCK_IMAGE_URL = `data:image/svg+xml,${encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1600 900">
  <defs>
    <linearGradient id="sky" x1="0" x2="1" y1="0" y2="1">
      <stop offset="0" stop-color="#4f8fb8"/>
      <stop offset="0.55" stop-color="#d8b56d"/>
      <stop offset="1" stop-color="#2f5d50"/>
    </linearGradient>
  </defs>
  <rect width="1600" height="900" fill="url(#sky)"/>
  <path d="M0 690 C240 540 420 650 620 510 C820 370 1040 560 1220 430 C1360 330 1480 390 1600 310 L1600 900 L0 900 Z" fill="#264e47" opacity="0.9"/>
  <path d="M0 760 C260 650 520 720 760 610 C980 510 1200 650 1600 560 L1600 900 L0 900 Z" fill="#183933" opacity="0.9"/>
</svg>
`)}`;

window.addEventListener("DOMContentLoaded", async () => {
  bindElements();
  bindEvents();
  // 优先使用本地缓存配置，否则向 Rust 后端请求默认值
  state.config = loadStoredConfig() ?? (await invoke("default_config"));
  writeConfigToForm(elements, state.config);
  await syncPlatformCapabilities();
  await syncPlatformPreferences();
  syncViewFromHash();
  await loadGallery({ reset: true });
  await syncSchedule();
});

// 将所有需要操作的 DOM 元素统一缓存到 elements 对象，避免重复查询
function bindElements() {
  for (const id of [
    "homeView",
    "settingsView",
    "aboutView",
    "market",
    "resolution",
    "rowFitMode",
    "fitMode",
    "rowSetLockScreen",
    "rowExcludeFromRecents",
    "scheduleEnabled",
    "scheduleMode",
    "rowIntervalMinutes",
    "intervalMinutes",
    "rowDailyTimes",
    "dailyTimesTags",
    "newDailyTime",
    "addDailyTime",
    "notifyBackground",
    "saveToFileSystem",
    "setLockScreen",
    "excludeFromRecents",
    "rowBatteryExemption",
    "batteryExemptionStatus",
    "requestBatteryExemption",
    "pageTitle",
    "status",
    "preview",
    "gallery",
    "refreshGallery",
    "loadMoreGallery",
    "applySelected",
    "clearWallpaper",
    "openSettings",
    "openAbout",
    "backHome",
  ]) {
    elements[id] = document.querySelector(`#${id}`);
  }
}

function bindEvents() {
  // 使用 URL hash 驱动视图切换，hashchange 事件确保浏览器前进/后退按钮也生效
  window.addEventListener("hashchange", syncViewFromHash);

  document.querySelector("#settings").addEventListener("change", () => {
    // 在更新 state.config 之前保存 Bing 源参数快照，直接比较字段值而非 JSON 字符串
    const prevMarket = state.config.bing.market;
    const prevResolution = state.config.bing.resolution;
    syncConfigFromForm();
    saveStoredConfig(state.config);
    // 仅更新预览的 objectFit 样式，不重新下载图片
    renderPreview();
    // scheduleMode 切换时同步显示/隐藏对应配置行
    updateScheduleModeVisibility(elements);
    void syncPlatformPreferences();
    void syncSchedule();

    // 仅 Bing 源参数（market / resolution）变化时才重置并重载画廊
    if (state.config.bing.market !== prevMarket || state.config.bing.resolution !== prevResolution) {
      void loadGallery({ reset: true });
    }
  });

  // 添加每日时间点：从 time input 读取并插入 tag 列表
  elements.addDailyTime.addEventListener("click", () => {
    const time = elements.newDailyTime.value;
    if (!time) return;
    const existing = Array.from(
      elements.dailyTimesTags.querySelectorAll(".daily-time-tag[data-time]"),
    ).map((t) => t.dataset.time);
    if (existing.includes(time)) return;
    syncConfigFromForm();
    const newTimes = [...existing, time].sort();
    renderDailyTimesTags(elements.dailyTimesTags, newTimes);
    syncConfigFromForm();
    saveStoredConfig(state.config);
    void syncSchedule();
    elements.newDailyTime.value = "";
  });

  elements.refreshGallery.addEventListener("click", () => loadGallery({ reset: true }));
  elements.loadMoreGallery.addEventListener("click", () =>
    loadGallery({ reset: false }),
  );
  elements.applySelected.addEventListener("click", applySelected);
  elements.clearWallpaper.addEventListener("click", clearWallpaper);
  elements.requestBatteryExemption.addEventListener("click", requestBatteryExemption);
  elements.openSettings.addEventListener("click", () => {
    location.hash = "settings";
  });
  elements.openAbout.addEventListener("click", () => {
    location.hash = "about";
  });
  // 返回逻辑：about → settings，其他所有页 → home
  elements.backHome.addEventListener("click", () => {
    location.hash = location.hash === "#about" ? "settings" : "";
  });
}

// 将表单当前值同步到 state.config，传入当前 config 以保留无 UI 控件的字段值
function syncConfigFromForm() {
  state.config = readConfigFromForm(elements, state.config);
}

/**
 * 根据 URL hash 切换可见视图，并同步标题栏控件的显隐状态。
 * 视图层级：home（默认）/ settings / about
 * 导航拓扑：home ←→ settings ←→ about
 */
function syncViewFromHash() {
  const view = location.hash === "#settings"
    ? "settings"
    : location.hash === "#about"
      ? "about"
      : "home";

  elements.homeView.hidden = view !== "home";
  elements.settingsView.hidden = view !== "settings";
  elements.aboutView.hidden = view !== "about";
  elements.pageTitle.textContent = {
    home: "Wallora",
    settings: "设置",
    about: "关于 Wallora",
  }[view];
  // home 视图不显示返回按钮和设置按钮
  elements.backHome.hidden = view === "home";
  elements.refreshGallery.hidden = view !== "home";
  elements.openSettings.hidden = view !== "home";
}

/**
 * 查询平台能力并据此显示/隐藏平台特有功能按钮。
 * 若 IPC 调用失败（例如插件尚未初始化），保守地隐藏平台专属按钮，不展示错误。
 */
async function syncPlatformCapabilities() {
  try {
    const capabilities = await invoke("platform_capabilities");
    elements.clearWallpaper.hidden = !capabilities.can_clear_wallpaper;
    elements.rowFitMode.hidden = !capabilities.supports_fit_mode;
    elements.rowSetLockScreen.hidden = !capabilities.supports_lock_screen_wallpaper;
    elements.rowExcludeFromRecents.hidden = !capabilities.has_exclude_from_recents;
    elements.rowBatteryExemption.hidden = !capabilities.has_battery_optimization;
    if (capabilities.has_battery_optimization) {
      await syncBatteryExemptionStatus();
    }
  } catch {
    elements.clearWallpaper.hidden = true;
    elements.rowSetLockScreen.hidden = true;
    elements.rowExcludeFromRecents.hidden = true;
    elements.rowBatteryExemption.hidden = true;
  }
}

async function syncPlatformPreferences() {
  try {
    await invoke("sync_platform_preferences", { config: state.config.platform });
  } catch {
    // 非 Android 或旧运行时不支持时忽略；配置仍会保存在 localStorage。
  }
}

/**
 * 查询电池优化豁免状态并更新 UI 显示。
 * 若已豁免则显示"已豁免"文字并隐藏申请按钮；否则显示"未豁免"和申请按钮。
 */
async function syncBatteryExemptionStatus() {
  try {
    const exempted = await invoke("check_battery_exemption");
    elements.batteryExemptionStatus.textContent = exempted ? "已豁免" : "未豁免（可能影响后台定时）";
    elements.requestBatteryExemption.hidden = exempted;
  } catch {
    elements.batteryExemptionStatus.textContent = "状态未知";
    elements.requestBatteryExemption.hidden = true;
  }
}

/**
 * 打开系统页面引导用户申请电池优化豁免。
 * 导航到系统设置后无法得知用户是否完成豁免，延迟 1s 后重新查询状态。
 */
async function requestBatteryExemption() {
  try {
    await invoke("request_battery_exemption");
    // 系统页面为异步跳转，500ms 后刷新状态（用户快速返回的场景）
    setTimeout(() => syncBatteryExemptionStatus(), 500);
  } catch (error) {
    setStatus(error);
  }
}

/**
 * 加载 Bing 壁纸画廊。
 *
 * reset=true：清空现有列表，从第 0 页重新拉取（刷新 / Bing 源参数变更时使用）。
 * reset=false：从分页器的 nextIndex 继续拉取并追加，自动去重。
 *
 * 分页原理：Bing archive API 使用 idx（偏移量）参数，
 * 每次固定拉取 BING_PAGE_SIZE 条；当返回数量 < BING_PAGE_SIZE 时认为已到末页。
 *
 * @param {{ reset: boolean }} options
 */
async function loadGallery({ reset }) {
  syncConfigFromForm();

  if (reset || !state.pager) {
    state.gallery = [];
    state.pager = createInitialPager();
    state.selected = null;
  }

  if (!state.pager.hasMore) {
    setStatus("No more wallpapers to load.");
    renderLoadMore();
    return;
  }

  const page = state.pager.nextIndex;
  // 强制覆盖 count，确保分页逻辑始终使用固定页大小
  const config = { ...state.config.bing, count: BING_PAGE_SIZE };
  setBusy(reset ? elements.refreshGallery : elements.loadMoreGallery, true);
  setStatus(reset ? "Loading Bing gallery..." : "Loading more wallpapers...");

  try {
    const wallpapers = await invoke("fetch_bing_gallery", { config, page });

    // reset 时不需要去重，直接使用原始列表；加载更多时才走去重合并逻辑
    const merged = reset
      ? { gallery: wallpapers, addedCount: wallpapers.length }
      : appendUniqueWallpapers(state.gallery, wallpapers);

    state.gallery = merged.gallery;
    // 将"全为重复项"信号传入 nextPagerAfterLoad，由其决定 hasMore，保持 pager 对象不可变
    state.pager = nextPagerAfterLoad(state.pager, wallpapers.length, {
      allDuplicates: !reset && merged.addedCount === 0,
    });

    // reset 时默认选中第一张；加载更多时保持原来的选中项，若尚无选中则选第一张
    state.selected = reset
      ? state.gallery[0] ?? null
      : state.selected ?? state.gallery[0] ?? null;

    renderGallery();
    renderPreview();
    renderLoadMore();
    setStatus(
      reset
        ? `Loaded ${state.gallery.length} wallpapers.`
        : merged.addedCount > 0
          ? `Loaded ${merged.addedCount} more wallpapers.`
          : "No more wallpapers to load.",
    );
  } catch (error) {
    setStatus(error);
  } finally {
    setBusy(reset ? elements.refreshGallery : elements.loadMoreGallery, false);
    renderLoadMore();
  }
}

// 应用当前选中壁纸，调用 Rust 后端下载并设置系统壁纸
async function applySelected() {
  if (!state.selected) {
    setStatus("Select a gallery item first.");
    return;
  }
  syncConfigFromForm();
  setBusy(elements.applySelected, true);
  setStatus("Applying selected wallpaper...");
  try {
    const result = await invoke("apply_wallpaper", {
      wallpaper: state.selected,
      config: state.config,
    });
    setStatus(result.message);
  } catch (error) {
    setStatus(error);
  } finally {
    setBusy(elements.applySelected, false);
  }
}

// 清除系统壁纸（仅 Android 平台可用，桌面端此按钮默认隐藏）
async function clearWallpaper() {
  setBusy(elements.clearWallpaper, true);
  try {
    setStatus(await invoke("clear_system_wallpaper"));
  } catch (error) {
    setStatus(error);
  } finally {
    setBusy(elements.clearWallpaper, false);
  }
}

/**
 * 根据当前配置启用或取消自动更新计划。
 * 桌面端使用 Rust 侧的线程调度器；Android 端通过 Kotlin WorkManager 实现。
 */
async function syncSchedule() {
  try {
    const status = state.config.schedule.enabled
      ? await invoke("configure_schedule", { config: state.config })
      : await invoke("cancel_schedule");
    setStatus(status.message);
  } catch (error) {
    setStatus(error);
  }
}

// 重建画廊 DOM。仅在列表内容变化时调用；选中状态变更通过直接更新 aria-pressed 处理。
function renderGallery() {
  elements.gallery.replaceChildren();
  for (const wallpaper of state.gallery) {
    const card = document.createElement("button");
    const image = document.createElement("img");
    const title = document.createElement("span");

    card.type = "button";
    card.className = "gallery-item";
    card.setAttribute("aria-pressed", String(wallpaper === state.selected));
    image.src = wallpaper.thumbnail_url;
    image.alt = wallpaper.title;
    image.loading = "lazy";
    title.textContent = wallpaper.title;

    card.append(image, title);
    card.addEventListener("click", () => {
      // 只切换两个卡片的 aria-pressed，焦点保持在被点击的卡片上，不重建 DOM
      const prev = elements.gallery.querySelector('[aria-pressed="true"]');
      if (prev) prev.setAttribute("aria-pressed", "false");
      card.setAttribute("aria-pressed", "true");
      state.selected = wallpaper;
      renderPreview();
    });
    elements.gallery.append(card);
  }
}

// 渲染壁纸预览区域。
// 若展示的是同一张图片，只更新 objectFit，避免重建 DOM 打断正在进行中的图片加载。
function renderPreview() {
  if (!state.selected) {
    elements.preview.replaceChildren();
    const empty = document.createElement("p");
    empty.textContent = "No wallpaper selected.";
    elements.preview.append(empty);
    return;
  }

  const objectFit = fitModeToObjectFit(state.config.fit_mode);
  const existingImg = elements.preview.querySelector("img");

  // 同一张图片仅更新 objectFit，保留正在加载的网络请求
  if (existingImg && existingImg.src === state.selected.preview_url) {
    existingImg.style.objectFit = objectFit;
    return;
  }

  const image = document.createElement("img");
  const details = document.createElement("div");
  const title = document.createElement("h2");
  const description = document.createElement("p");
  const publishedDate = document.createElement("small");

  image.src = state.selected.preview_url;
  image.alt = state.selected.title;
  image.style.objectFit = objectFit;
  title.textContent = state.selected.title;
  description.textContent = state.selected.description;
  publishedDate.textContent = state.selected.published_date;

  details.append(title, description, publishedDate);
  elements.preview.replaceChildren(image, details);
}

// 根据分页器状态控制"加载更多"按钮的可用性
function renderLoadMore() {
  elements.loadMoreGallery.disabled = !state.pager?.hasMore;
}

// 将 AppConfig 的 FitMode 枚举值映射为 CSS object-fit 属性值
function fitModeToObjectFit(mode) {
  return {
    Fill: "cover",
    Fit: "contain",
    Stretch: "fill",
    Center: "none",
  }[mode];
}

// 设置按钮的加载中状态（禁用 + aria-busy 属性）
function setBusy(button, busy) {
  button.setAttribute("aria-busy", String(busy));
  button.disabled = busy;
}

function setStatus(message) {
  elements.status.textContent = String(message);
}

/**
 * 浏览器开发模式下的 IPC 模拟层，模拟 Tauri invoke 的返回结构。
 * 在非 Tauri 运行时（如直接用浏览器打开 index.html）时自动启用。
 *
 * fetch_bing_gallery 模拟行为：
 * - 第 0 页返回 items 1-8，第 8 页返回 items 9-16
 * - page >= 16 时回绕到 items 9-16（模拟 Bing 历史存档的有限深度）
 */
async function mockInvoke(command, args = {}) {
  if (command === "default_config") {
    return {
      bing: { market: "UnitedStates", resolution: "Uhd4k", count: 8 },
      fit_mode: "Fill",
      schedule: {
        enabled: false,
        mode: "Interval",
        interval_minutes: 360,
        daily_times: [],
        notify_on_background_update: true,
      },
      save_to_file_system: true,
      platform: {
        set_lock_screen: false,
        exclude_from_recents: false,
      },
    };
  }
  if (command === "platform_capabilities") {
    return {
      can_clear_wallpaper: false,
      supports_lock_screen_wallpaper: false,
      has_battery_optimization: false,
      supports_fit_mode: false,
      has_exclude_from_recents: false,
    };
  }
  if (command === "check_battery_exemption") {
    return true;
  }
  if (command === "request_battery_exemption") {
    return null;
  }
  if (command === "sync_platform_preferences") {
    return null;
  }
  if (command === "fetch_bing_gallery") {
    const page = Number(args.page ?? 0);
    const count = Number(args.config?.count ?? BING_PAGE_SIZE);
    const start = page >= 16 ? 8 : page;
    const returnedCount = count;
    return Array.from({ length: returnedCount }, (_, index) => {
      const itemNumber = start + index + 1;
      return {
        source_id: "bing",
        source_wallpaper_id: `demo-${itemNumber}`,
        title: `Preview ${itemNumber}`,
        description: "Run inside Tauri to load live Bing wallpapers.",
        published_date: `202606${String(16 - itemNumber).padStart(2, "0")}`,
        thumbnail_url: MOCK_IMAGE_URL,
        preview_url: MOCK_IMAGE_URL,
        image_url: MOCK_IMAGE_URL,
        detail_url: null,
      };
    });
  }
  if (command === "configure_schedule") {
    return { enabled: true, message: "Automatic updates enabled." };
  }
  if (command === "cancel_schedule") {
    return { enabled: false, message: "Automatic updates disabled." };
  }
  throw new Error("This action requires the Tauri runtime.");
}

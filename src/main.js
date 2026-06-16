const invoke = window.__TAURI__?.core?.invoke ?? mockInvoke;

const state = {
  config: null,
  gallery: [],
  selected: null,
  timerId: null,
};

const elements = {};

window.addEventListener("DOMContentLoaded", async () => {
  bindElements();
  bindEvents();
  state.config = loadConfig() ?? (await invoke("default_config"));
  syncFormFromConfig();
  await loadGallery();
  restartSchedule();
});

function bindElements() {
  for (const id of [
    "market",
    "resolution",
    "count",
    "fitMode",
    "scheduleEnabled",
    "intervalMinutes",
    "notifyBackground",
    "saveToFileSystem",
    "setLockScreen",
    "status",
    "preview",
    "gallery",
    "manualUpdate",
    "refreshGallery",
    "applySelected",
    "clearWallpaper",
  ]) {
    elements[id] = document.querySelector(`#${id}`);
  }
}

function bindEvents() {
  document.querySelector("#settings").addEventListener("change", () => {
    syncConfigFromForm();
    saveConfig();
    restartSchedule();
    loadGallery();
  });
  elements.manualUpdate.addEventListener("click", manualUpdate);
  elements.refreshGallery.addEventListener("click", loadGallery);
  elements.applySelected.addEventListener("click", applySelected);
  elements.clearWallpaper.addEventListener("click", clearWallpaper);
}

function syncFormFromConfig() {
  elements.market.value = state.config.bing.market;
  elements.resolution.value = state.config.bing.resolution;
  elements.count.value = state.config.bing.count;
  elements.fitMode.value = state.config.fit_mode;
  elements.scheduleEnabled.checked = state.config.schedule.enabled;
  elements.intervalMinutes.value = state.config.schedule.interval_minutes;
  elements.notifyBackground.checked =
    state.config.schedule.notify_on_background_update;
  elements.saveToFileSystem.checked = state.config.save_to_file_system;
  elements.setLockScreen.checked = state.config.platform.set_lock_screen;
}

function syncConfigFromForm() {
  state.config = {
    bing: {
      market: elements.market.value,
      resolution: elements.resolution.value,
      count: Number(elements.count.value),
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
      notify_on_manual_update: true,
    },
  };
}

async function loadGallery() {
  syncConfigFromForm();
  setStatus("Loading Bing gallery...");
  try {
    state.gallery = await invoke("fetch_bing_gallery", {
      config: state.config.bing,
      page: 0,
    });
    state.selected = state.gallery[0] ?? null;
    renderGallery();
    renderPreview();
    setStatus(`Loaded ${state.gallery.length} wallpapers.`);
  } catch (error) {
    setStatus(error);
  }
}

async function manualUpdate({ scheduled = false } = {}) {
  syncConfigFromForm();
  setBusy(elements.manualUpdate, true);
  setStatus("Updating wallpaper...");
  try {
    const config = configForUpdate(scheduled);
    const result = await invoke("manual_update", { config });
    setStatus(result.message);
  } catch (error) {
    setStatus(error);
  } finally {
    setBusy(elements.manualUpdate, false);
  }
}

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

function restartSchedule() {
  if (state.timerId) {
    clearInterval(state.timerId);
    state.timerId = null;
  }
  if (!state.config.schedule.enabled) return;

  const intervalMs = Math.max(1, state.config.schedule.interval_minutes) * 60_000;
  state.timerId = setInterval(() => {
    manualUpdate({ scheduled: true });
    if (state.config.schedule.notify_on_background_update) {
      setStatus("Scheduled wallpaper update started.");
    }
  }, intervalMs);
}

function configForUpdate(scheduled) {
  return {
    ...state.config,
    platform: {
      ...state.config.platform,
      notify_on_manual_update: scheduled
        ? state.config.schedule.notify_on_background_update
        : state.config.platform.notify_on_manual_update,
    },
  };
}

function renderGallery() {
  elements.gallery.replaceChildren();
  for (const wallpaper of state.gallery) {
    const card = document.createElement("button");
    card.type = "button";
    card.className = "gallery-item";
    card.setAttribute("aria-pressed", wallpaper === state.selected);
    card.innerHTML = `
      <img src="${wallpaper.image_url}" alt="${wallpaper.title}" loading="lazy" />
      <span>${wallpaper.title}</span>
    `;
    card.addEventListener("click", () => {
      state.selected = wallpaper;
      renderGallery();
      renderPreview();
    });
    elements.gallery.append(card);
  }
}

function renderPreview() {
  if (!state.selected) {
    elements.preview.innerHTML = "<p>No wallpaper selected.</p>";
    return;
  }
  elements.preview.innerHTML = `
    <img src="${state.selected.image_url}" alt="${state.selected.title}" />
    <div>
      <h2>${state.selected.title}</h2>
      <p>${state.selected.description}</p>
      <small>${state.selected.published_date}</small>
    </div>
  `;
  elements.preview.querySelector("img").style.objectFit =
    fitModeToObjectFit(state.config.fit_mode);
}

function fitModeToObjectFit(mode) {
  return {
    Fill: "cover",
    Fit: "contain",
    Stretch: "fill",
    Center: "none",
  }[mode];
}

function saveConfig() {
  localStorage.setItem("wallpaper.config", JSON.stringify(state.config));
}

function loadConfig() {
  const raw = localStorage.getItem("wallpaper.config");
  return raw ? JSON.parse(raw) : null;
}

function setBusy(button, busy) {
  button.setAttribute("aria-busy", String(busy));
  button.disabled = busy;
}

function setStatus(message) {
  elements.status.textContent = String(message);
}

async function mockInvoke(command) {
  if (command === "default_config") {
    return {
      bing: { market: "UnitedStates", resolution: "Uhd4k", count: 8 },
      fit_mode: "Fill",
      schedule: {
        enabled: false,
        interval_minutes: 360,
        notify_on_background_update: true,
      },
      save_to_file_system: true,
      platform: { set_lock_screen: false, notify_on_manual_update: true },
    };
  }
  if (command === "fetch_bing_gallery") {
    return [
      {
        source_id: "bing",
        source_wallpaper_id: "demo",
        title: "Preview only",
        description: "Run inside Tauri to load live Bing wallpapers.",
        published_date: "20260615",
        image_url: "https://www.bing.com/th?id=OHR.Example_EN-US1234567890_UHD.jpg",
        detail_url: null,
      },
    ];
  }
  throw new Error("This action requires the Tauri runtime.");
}

import assert from "node:assert/strict";
import test from "node:test";

import { readConfigFromForm, writeConfigToForm } from "./configStore.js";

function makeElements() {
  const dailyTimesTags = {
    replaceChildren() {},
    querySelectorAll() {
      return [];
    },
  };
  return {
    market: { value: "" },
    resolution: { value: "" },
    fitMode: { value: "" },
    scheduleEnabled: { checked: false },
    scheduleMode: { value: "" },
    intervalMinutes: { value: "0" },
    rowIntervalMinutes: { hidden: false },
    rowDailyTimes: { hidden: false },
    dailyTimesTags,
    notifyBackground: { checked: false },
    saveToFileSystem: { checked: false },
    setLockScreen: { checked: false },
    excludeFromRecents: { checked: false },
  };
}

test("platform recents preference round-trips through the settings form", () => {
  const elements = makeElements();

  writeConfigToForm(elements, {
    bing: { market: "China", resolution: "Portrait1080x1920", count: 8 },
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
      set_lock_screen: true,
      exclude_from_recents: true,
    },
  });

  assert.equal(elements.excludeFromRecents.checked, true);
  const config = readConfigFromForm(elements);
  assert.equal(config.platform.exclude_from_recents, true);
  assert.equal("notify_on_manual_update" in config.platform, false);
});

test("platform recents preference defaults to disabled for old stored configs", () => {
  const elements = makeElements();

  assert.equal(readConfigFromForm(elements).platform.exclude_from_recents, false);
});

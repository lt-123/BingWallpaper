import assert from "node:assert/strict";
import test from "node:test";

import {
  BING_PAGE_SIZE,
  appendUniqueWallpapers,
  createInitialPager,
  nextPagerAfterLoad,
} from "./galleryPager.js";

test("Bing page size is fixed at eight", () => {
  assert.equal(BING_PAGE_SIZE, 8);
  assert.deepEqual(createInitialPager(), {
    nextIndex: 0,
    hasMore: true,
  });
});

test("nextPagerAfterLoad advances the next Bing index by the fixed page size", () => {
  const pager = createInitialPager();

  assert.deepEqual(nextPagerAfterLoad(pager, 8), {
    nextIndex: 8,
    hasMore: true,
  });
});

test("nextPagerAfterLoad stops pagination when Bing returns a partial page", () => {
  const pager = { nextIndex: 8, hasMore: true };

  assert.deepEqual(nextPagerAfterLoad(pager, 3), {
    nextIndex: 16,
    hasMore: false,
  });
});

test("nextPagerAfterLoad stops pagination when a full page is entirely duplicates", () => {
  const pager = { nextIndex: 8, hasMore: true };

  assert.deepEqual(nextPagerAfterLoad(pager, 8, { allDuplicates: true }), {
    nextIndex: 16,
    hasMore: false,
  });
});

test("appendUniqueWallpapers filters overlapping Bing pages", () => {
  const current = [
    { source_wallpaper_id: "a", title: "A" },
    { source_wallpaper_id: "b", title: "B" },
  ];
  const incoming = [
    { source_wallpaper_id: "b", title: "B again" },
    { source_wallpaper_id: "c", title: "C" },
  ];

  assert.deepEqual(appendUniqueWallpapers(current, incoming), {
    gallery: [
      { source_wallpaper_id: "a", title: "A" },
      { source_wallpaper_id: "b", title: "B" },
      { source_wallpaper_id: "c", title: "C" },
    ],
    addedCount: 1,
  });
});

test("appendUniqueWallpapers reports no additions for a fully repeated page", () => {
  const current = [{ source_wallpaper_id: "a" }];
  const incoming = [{ source_wallpaper_id: "a" }];

  assert.deepEqual(appendUniqueWallpapers(current, incoming), {
    gallery: current,
    addedCount: 0,
  });
});

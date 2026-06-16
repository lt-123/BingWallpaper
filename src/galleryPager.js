/**
 * Bing 每次请求返回的壁纸数量上限，同时作为"是否还有更多"的判断阈值：
 * 若实际返回数量 < BING_PAGE_SIZE，则认为已到达末页。
 */
export const BING_PAGE_SIZE = 8;

/**
 * 创建分页器初始状态。
 * nextIndex 对应 Bing archive API 的 idx 参数（起始偏移）。
 * @returns {{ nextIndex: number, hasMore: boolean }}
 */
export function createInitialPager() {
  return {
    nextIndex: 0,
    hasMore: true,
  };
}

/**
 * 根据本次请求的实际返回数量，推进分页器到下一页。
 * 返回新对象，不修改传入的 pager。
 *
 * 判断逻辑：
 * - Bing 返回满页（>= BING_PAGE_SIZE）时认为还有更多；
 * - 返回不足一页时即为末页；
 * - 即使返回满页，若全部为重复项（allDuplicates=true），也标记为末页。
 *
 * @param {{ nextIndex: number, hasMore: boolean }} pager 当前分页器状态
 * @param {number} returnedCount 本次 API 实际返回的壁纸数量
 * @param {{ allDuplicates?: boolean }} options
 * @returns {{ nextIndex: number, hasMore: boolean }}
 */
export function nextPagerAfterLoad(pager, returnedCount, { allDuplicates = false } = {}) {
  return {
    nextIndex: pager.nextIndex + BING_PAGE_SIZE,
    hasMore: returnedCount >= BING_PAGE_SIZE && !allDuplicates,
  };
}

/**
 * 将新拉取的壁纸合并到现有画廊，自动过滤重复项。
 * 同一批 incoming 内部的重复也会被去除（通过在 filter 内实时更新 existingIds）。
 *
 * 若无新增内容，直接返回原始 current 引用，避免不必要的数组复制。
 *
 * @param {object[]} current 当前画廊列表
 * @param {object[]} incoming 新拉取的壁纸列表
 * @returns {{ gallery: object[], addedCount: number }}
 */
export function appendUniqueWallpapers(current, incoming) {
  const existingIds = new Set(current.map(wallpaperIdentity));
  const additions = incoming.filter((wallpaper) => {
    const identity = wallpaperIdentity(wallpaper);
    if (existingIds.has(identity)) {
      return false;
    }
    // 将本批次已接受的 ID 也加入集合，防止 incoming 内部出现重复
    existingIds.add(identity);
    return true;
  });

  return {
    gallery: additions.length > 0 ? [...current, ...additions] : current,
    addedCount: additions.length,
  };
}

/**
 * 壁纸的唯一标识键。
 * 优先使用来源侧 ID（source_wallpaper_id），
 * 仅在 ID 缺失（null/undefined）时回退到图片 URL。
 * 注意：source_wallpaper_id 在 Rust 端被序列化为 String，不会是数字 0 或空字符串，
 * 因此 || 回退仅处理 null/undefined 的情况，行为与预期一致。
 */
function wallpaperIdentity(wallpaper) {
  return wallpaper.source_wallpaper_id || wallpaper.image_url;
}

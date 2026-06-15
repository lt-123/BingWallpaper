# Bing Resolution and Fit Mode Design

## 背景

当前 Bing 图片下载分辨率由 `BingWallpaperSource.DEFAULT_RESOLUTION = "UHD"` 固定控制，用户无法切换省流量、横屏或竖屏分辨率。当前 v3 设置壁纸流程通过 `WallpaperSetter.setStream()` 把下载文件直接交给系统，图片比例和设备屏幕比例不一致时由系统决定装配效果。

本需求增加两个配置：

- 下载分辨率：控制最终 Bing 图片 URL 的后缀。
- 壁纸装配模式：控制下载图与设备屏幕尺寸不匹配时的处理方式。

## 目标

- 默认行为兼容现状：分辨率默认 `UHD`，装配模式默认 `系统默认`。
- 分辨率配置通过 `SettingsStore` 持久化，并注入 `BingWallpaperSource`。
- 装配模式配置通过 `SettingsStore` 持久化，并注入 `WallpaperSetter`。
- 显式装配模式只在用户选择时启用；默认模式继续使用 `WallpaperManager.setStream()`。
- 图片变换逻辑独立成可测试单元，避免把比例计算塞进 Activity 或 Service。

## 非目标

- 不新增在线分辨率探测。
- 不为每个设备自动选择不同 Bing 分辨率。
- 不改变定时、通知、保存到相册流程。
- 不引入第三方图片库。

## 用户体验

设置页在“必应地区”附近新增两个原生设置项：

- `图片分辨率`
  - 默认：`UHD`
  - 选项：`UHD`、`1920x1080`、`1920x1200`、`1366x768`、`1080x1920`、`768x1366`
- `装配模式`
  - 默认：`系统默认`
  - 选项：
    - `系统默认`：保持现有系统行为。
    - `裁剪填满`：等比缩放填满屏幕，居中裁剪超出部分。
    - `完整显示`：等比完整显示，黑色背景补边。
    - `拉伸填满`：非等比拉伸到屏幕尺寸。

## 数据模型

### `BingSourceOptions`

增加分辨率字段：

```java
public static final String RESOLUTION_UHD = "UHD";
public static final String RESOLUTION_1920_1080 = "1920x1080";
public static final String RESOLUTION_1920_1200 = "1920x1200";
public static final String RESOLUTION_1366_768 = "1366x768";
public static final String RESOLUTION_1080_1920 = "1080x1920";
public static final String RESOLUTION_768_1366 = "768x1366";

public BingSourceOptions(String market, String resolution);
public String getResolution();
public static String normalizeResolution(String resolution);
```

`normalizeResolution()` 只允许内置白名单，非法值回退 `UHD`。

### `WallpaperFitMode`

新增枚举：

```java
public enum WallpaperFitMode {
    SYSTEM,
    CROP,
    CONTAIN,
    STRETCH
}
```

提供稳定的持久化值、默认值和 `fromValue()`，非法值回退 `SYSTEM`。

### `SettingsStore`

新增：

```java
public String bingResolution();
public void bingResolution(String resolution);
public WallpaperFitMode wallpaperFitMode();
public void wallpaperFitMode(WallpaperFitMode mode);
```

`bingSourceOptions()` 返回 `new BingSourceOptions(bingMarket(), bingResolution())`。

## 壁纸装配

新增 `WallpaperBitmapTransformer`：

- `CROP`：按较大比例等比缩放，输出尺寸等于屏幕尺寸，居中裁剪。
- `CONTAIN`：按较小比例等比缩放，输出尺寸等于屏幕尺寸，黑色背景居中补边。
- `STRETCH`：直接缩放到屏幕尺寸。

`WallpaperSetter`：

- 构造时接收 `WallpaperFitMode`，默认 `SYSTEM`。
- `SYSTEM` 使用现有 `setStream()` 路径。
- 其它模式读取图片、获取屏幕尺寸、调用 `WallpaperBitmapTransformer`，再通过 `setBitmap()` 设置。
- 锁屏开关语义保持不变：开启时同时设置桌面和锁屏，关闭时只设置桌面。

## 测试策略

- `BingSourceOptionsTest`
  - 默认分辨率为 `UHD`。
  - 合法分辨率保留，非法分辨率回退 `UHD`。
- `SettingsStoreTest`
  - 分辨率可读写，非法存储值回退 `UHD`。
  - 装配模式可读写，非法存储值回退 `SYSTEM`。
- `WallpaperFitModeTest`
  - 持久化值往返。
  - 非法值回退默认。
- `WallpaperBitmapTransformerTest`
  - `CROP` 输出屏幕尺寸并裁剪两侧或上下。
  - `CONTAIN` 输出屏幕尺寸并保留完整图像。
  - `STRETCH` 输出屏幕尺寸。
- `BingWallpaperSourceTest`
  - `fetch()` 使用配置分辨率生成图片 URL 和文件名。

## 成功标准

- 默认配置仍下载 `UHD`，并使用系统默认装配。
- 用户选择 `1920x1080` 后，下一次同步下载 URL 使用 `_1920x1080.jpg`。
- 用户选择 `裁剪填满`、`完整显示` 或 `拉伸填满` 后，下一次设置壁纸走显式 Bitmap 装配。
- `testDebugUnitTest`、`assembleDebug` 和 `git diff --check` 通过。

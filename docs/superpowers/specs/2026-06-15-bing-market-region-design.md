# Bing Market Region Design

## 背景

GitHub issue #2 请求为 Bing 每日壁纸 API 增加自定义地区能力。Bing 的
`HPImageArchive.aspx` 接口可以通过 `mkt` 参数选择市场地区，例如
`mkt=en-ww`，从而在中国网络环境下获取国际版每日壁纸。issue 同时提到，
返回的 `urlbase` 即使来自国际市场，仍可通过不同 Bing 域名访问。当前实现采用固定规则：
中国地区使用 `cn.bing.com`，其它地区使用 `www.bing.com`。

当前 v3 代码已经删除多源功能，只保留 Bing 内置源；这次设计只扩展 Bing 源参数，不恢复旧版自定义源列表。

## 目标

- 支持用户配置 Bing API 的 `mkt` 地区参数。
- 图片 URL 域名不再单独配置：`zh-CN` 使用 `cn.bing.com`，其它地区和默认地区使用 `www.bing.com`。
- 默认行为保持兼容：不配置 `mkt` 时仍请求当前默认 Bing API。
- 所有配置通过 `SettingsStore` 读写，避免 UI、Service、Source 直接依赖 SharedPreferences key。
- `BingWallpaperSource` 继续只负责 Bing API URL 构造、JSON 解析和 `WallpaperInfo` 生成。

## 非目标

- 不恢复旧版自定义直链源功能。
- 不增加第三方依赖。
- 不引入复杂地区数据库或远程地区列表。
- 不改变保存、设置壁纸、定时同步流程。

## 用户体验

主设置页新增一个配置项，放在“保存到相册”之前或之后均可，优先放在壁纸来源相关位置：

- `必应地区`
  - 副标题展示当前值，例如 `跟随默认`、`国际版 en-WW`、`美国 en-US`、`中国 zh-CN`、`自定义 ja-JP`。
  - 点击后弹出原生 `AlertDialog` 单选列表。
  - 列表包含常用项：`跟随默认`、`中国 zh-CN`、`国际版 en-WW`、`美国 en-US`、`英国 en-GB`、`日本 ja-JP`、`自定义...`。
  - 选择自定义时弹出 `EditText`，用户输入市场代码。保存前做轻量校验。

`mkt` 自定义校验规则：

- 允许空值；空值表示不追加 `mkt` 参数。
- 非空时只允许 ASCII 字母、数字和连字符。
- 长度限制为 2 到 16 个字符。
- 保存时统一 trim。

## UI 设计

### 设置页布局

继续使用现有 `activity_setting.xml` 的原生设置项风格，不引入自定义 Toolbar、不指定 Activity 主题。新增设置项应复用现有模式：

```xml
<LinearLayout
    android:id="@+id/ll_bing_market"
    style="@style/SettingItem"
    android:orientation="vertical">

    <TextView
        style="@style/SettingItemTitle"
        android:text="@string/bing_market" />

    <TextView
        android:id="@+id/tv_bing_market"
        style="@style/SettingItemSubTitle"
        android:text="@string/bing_market_default" />
</LinearLayout>

<View style="@style/SettingItemLine" />
```

推荐位置：放在 `ll_time` 之后、`ll_save_path` 之前。理由是这个配置影响“下载哪张 Bing 壁纸”，比保存路径、锁屏、Toast 更接近源配置。

根布局继续保留 `android:fitsSystemWindows="true"`，避免默认系统标题栏或系统栏覆盖内容。不要在 Manifest 中为 `SettingActivity` 指定自定义主题。

### 设置项文案

新增字符串：

```xml
<string name="bing_market">必应地区</string>
<string name="bing_market_default">跟随默认</string>
<string name="bing_market_china">中国 zh-CN</string>
<string name="bing_market_global">国际版 en-WW</string>
<string name="bing_market_us">美国 en-US</string>
<string name="bing_market_uk">英国 en-GB</string>
<string name="bing_market_japan">日本 ja-JP</string>
<string name="bing_market_custom">自定义...</string>
<string name="bing_market_custom_title">自定义必应地区</string>
<string name="bing_market_custom_hint">例如 en-WW</string>
<string name="bing_market_invalid">地区代码格式不正确</string>
```

### 交互设计

`必应地区` 点击流程：

1. `SettingActivity` 调用 `showBingMarketDialog()`。
2. 弹出单选 `AlertDialog`，列表顺序固定为：
   - 跟随默认
   - 中国 zh-CN
   - 国际版 en-WW
   - 美国 en-US
   - 英国 en-GB
   - 日本 ja-JP
   - 自定义...
3. 用户选择预设项时立即保存 market 并刷新副标题。
4. 用户选择自定义时，再弹出 `EditText` 对话框。
5. 自定义输入校验通过后保存；校验失败时保留原配置并 Toast 提示。

副标题生成规则：

- market 为空：`跟随默认`
- market 是预设值：显示对应中文名称和值，例如 `国际版 en-WW`
- market 不是预设值：显示 `自定义 <market>`
- 图片 host：由 market 自动推导，UI 不单独展示。

### Activity 代码组织

`SettingActivity` 目前已经承担多个设置项的读写。为避免继续拉长 `onClick()` 和 `refreshSubText()`，新增 Bing 配置相关方法集中放置：

```java
private TextView tvBingMarket;

private void bindViews();
private void bindListeners();
private void showBingMarketDialog();
private void showCustomBingMarketDialog();
private void refreshBingSourceText();
private int currentMarketSelection(String market);
```

`onCreate()` 只做三件事：`setContentView`、初始化 `SettingsStore`、调用 bind 方法。现有其它设置项可以暂不重构，但新增逻辑不要继续堆到 `onCreate()` 中。

`onClick()` 新增一个分支：

```java
} else if (id == R.id.ll_bing_market) {
    showBingMarketDialog();
}
```

`loadData()` 读取配置后调用 `refreshBingSourceText()`；预设项保存后也调用该方法，不需要重启页面。

## 数据模型

新增一个轻量配置模型：

```java
public final class BingSourceOptions {
    public static final String HOST_GLOBAL = "www.bing.com";
    public static final String HOST_CHINA = "cn.bing.com";

    public static final String MARKET_DEFAULT = "";
    public static final String MARKET_CHINA = "zh-CN";
    public static final String MARKET_GLOBAL = "en-WW";
    public static final String MARKET_US = "en-US";
    public static final String MARKET_UK = "en-GB";
    public static final String MARKET_JAPAN = "ja-JP";

    private final String market;

    public BingSourceOptions(String market) {
        this.market = normalizeMarket(market);
    }

    public String getMarket() {
        return market;
    }

    public String getImageHost() {
        return imageHostForMarket(market);
    }

    public static boolean isValidMarket(String market) {
        if (market == null) {
            return true;
        }
        String value = market.trim();
        if (value.isEmpty()) {
            return true;
        }
        return value.length() >= 2
                && value.length() <= 16
                && value.matches("[A-Za-z0-9-]+");
    }

    public static String normalizeMarket(String market) {
        if (market == null) {
            return MARKET_DEFAULT;
        }
        String value = market.trim();
        if (!isValidMarket(value)) {
            return MARKET_DEFAULT;
        }
        return value;
    }

    public static String imageHostForMarket(String market) {
        return MARKET_CHINA.equals(normalizeMarket(market)) ? HOST_CHINA : HOST_GLOBAL;
    }

    public static BingSourceOptions defaults() {
        return new BingSourceOptions(MARKET_DEFAULT);
    }
}
```

配置含义：

- `market`
  - 空字符串表示不传 `mkt`。
  - 非空字符串直接作为 `mkt` 参数值，例如 `en-WW`。
- `imageHost`
  - 不做持久化配置。
  - 由 `market` 推导：`zh-CN` 使用 `cn.bing.com`，其它地区和默认地区使用 `www.bing.com`。

`Constants.Default` 增加：

```java
String KEY_BING_MARKET = "KEY_BING_MARKET";
```

`SettingsStore` 增加：

```java
public String bingMarket();
public void bingMarket(String market);
public BingSourceOptions bingSourceOptions();
```

## 代码结构设计

### 新增文件

`app/src/main/java/xyz/liut/bingwallpaper/v3/source/BingSourceOptions.java`

职责：

- 持有 Bing 源的用户配置。
- 提供 market 校验和规范化。
- 提供 market 到 image host 的推导逻辑。

不做的事：

- 不读写 SharedPreferences。
- 不构造 API URL。
- 不依赖 Android UI。

### 修改 `Constants`

在 `Constants.Default` 增加 key。旧的 `KEY_SOURCE_LIST`、`KEY_DEFAULT_SOURCE` 暂不在本需求中清理，避免扩大范围。

```java
String KEY_BING_MARKET = "KEY_BING_MARKET";
```

### 修改 `SettingsStore`

`SettingsStore` 是唯一读写持久化配置的入口：

```java
public String bingMarket() {
    return BingSourceOptions.normalizeMarket(
            spTool.get(Constants.Default.KEY_BING_MARKET, BingSourceOptions.MARKET_DEFAULT));
}

public void bingMarket(String market) {
    spTool.save(Constants.Default.KEY_BING_MARKET, BingSourceOptions.normalizeMarket(market));
}

public BingSourceOptions bingSourceOptions() {
    return new BingSourceOptions(bingMarket());
}
```

### 修改 `BingWallpaperSource`

`BingWallpaperSource` 增加 `BingSourceOptions options` 字段：

```java
private final BingSourceOptions options;
```

构造函数：

```java
public BingWallpaperSource() {
    this(new HttpDownloader(), DEFAULT_RESOLUTION, BingSourceOptions.defaults());
}

public BingWallpaperSource(HttpDownloader downloader, String resolution, BingSourceOptions options) {
    this(new DownloaderTextFetcher(downloader), resolution, options);
}

public BingWallpaperSource(TextFetcher textFetcher, String resolution, BingSourceOptions options) {
    this.textFetcher = textFetcher;
    this.resolution = resolution;
    this.options = options == null ? BingSourceOptions.defaults() : options;
}
```

`fetch()`：

```java
public WallpaperInfo fetch() throws Exception {
    String json = textFetcher.get(buildApiUrl(options.getMarket()));
    return parseJson(json, resolution, options.getImageHost());
}
```

`buildApiUrl()` 使用 `Uri.Builder`：

```java
static String buildApiUrl(String market) {
    Uri.Builder builder = new Uri.Builder()
            .scheme("https")
            .authority("www.bing.com")
            .path("HPImageArchive.aspx")
            .appendQueryParameter("format", "js")
            .appendQueryParameter("n", "1");
    String normalizedMarket = BingSourceOptions.normalizeMarket(market);
    if (!normalizedMarket.isEmpty()) {
        builder.appendQueryParameter("mkt", normalizedMarket);
    }
    return builder.build().toString();
}
```

`parseJson()`：

```java
public static WallpaperInfo parseJson(String json, String resolution, String imageHost) throws Exception {
    JSONObject image = new JSONObject(json)
            .getJSONArray("images")
            .getJSONObject(0);

    String host = sanitizeImageHost(imageHost);
    String urlbase = image.getString("urlbase");
    String imageUrl = "https://" + host + urlbase + "_" + resolution + ".jpg";
    String fileName = fileNameFromUrlbase(urlbase, resolution);
    String title = image.optString("title");
    String description = image.optString("copyright");

    return new WallpaperInfo(imageUrl, fileName, title, description);
}
```

### 修改 `SyncWallpaperService`

只改 source 创建处：

```java
BingSourceOptions options = settingsStore.bingSourceOptions();
wallpaperSource = new BingWallpaperSource(httpDownloader, BingWallpaperSource.DEFAULT_RESOLUTION, options);
```

Service 不关心 market 和 host 的具体 key，也不做校验。

### 修改 `SettingActivity`

新增字段：

```java
private TextView tvBingMarket;
```

新增常量数组，避免对话框和副标题逻辑写散：

```java
private static final String[] MARKET_VALUES = {
        BingSourceOptions.MARKET_DEFAULT,
        BingSourceOptions.MARKET_CHINA,
        BingSourceOptions.MARKET_GLOBAL,
        BingSourceOptions.MARKET_US,
        BingSourceOptions.MARKET_UK,
        BingSourceOptions.MARKET_JAPAN
};

```

市场列表 UI 文字从 string 资源读取，值从数组读取。`自定义...` 不放入 `MARKET_VALUES`，避免误保存 literal 文案。

自定义输入用原生 `EditText`：

```java
EditText input = new EditText(this);
input.setSingleLine(true);
input.setHint(R.string.bing_market_custom_hint);
input.setText(settingsStore.bingMarket());
```

保存：

```java
String market = input.getText().toString().trim();
if (!BingSourceOptions.isValidMarket(market)) {
    ToastUtil.showToast(this, getString(R.string.bing_market_invalid));
    return;
}
settingsStore.bingMarket(market);
refreshBingSourceText();
```

### 代码边界

- UI 只保存用户选择，不拼 Bing API URL。
- `SettingsStore` 只负责持久化和默认值，不解析 JSON。
- `BingSourceOptions` 负责配置值合法性和规范化。
- `BingWallpaperSource` 负责把配置转成实际请求 URL 和图片 URL。
- `WallpaperSyncUseCase` 不需要变化。

## Source 设计

`BingWallpaperSource` 构造函数增加 `BingSourceOptions`：

```java
public BingWallpaperSource(HttpDownloader downloader, String resolution, BingSourceOptions options)
```

默认构造仍使用空 market 和 `www.bing.com`，保持现有行为。

API URL 构造从常量字符串改为方法：

```java
static String buildApiUrl(String market)
```

规则：

- 基础地址为 `https://www.bing.com/HPImageArchive.aspx`。
- 固定参数：`format=js`、`n=1`。
- `market` 非空时追加 `mkt=<market>`。
- 使用 `Uri.Builder` 或 `java.net.URI` 风格的安全构造方式，避免手写拼接遗漏转义。

JSON 解析增加 image host 参数：

```java
public static WallpaperInfo parseJson(String json, String resolution, String imageHost)
```

规则：

- `urlbase` 仍从 Bing API JSON 读取。
- 最终图片 URL 为 `https://<imageHost><urlbase>_<resolution>.jpg`。
- 文件名仍从 `urlbase` 的 `id=` 提取并安全化，不包含 host。

保留旧的 `parseJson(String json, String resolution)` 作为测试和兼容入口，内部委托到新方法并使用默认 host。

## 同步流程

`SyncWallpaperService.onCreate()` 中创建 `BingWallpaperSource` 时从 `SettingsStore` 读取配置：

```java
BingSourceOptions options = settingsStore.bingSourceOptions();
wallpaperSource = new BingWallpaperSource(httpDownloader, BingWallpaperSource.DEFAULT_RESOLUTION, options);
```

这意味着每次启动同步服务都会读取最新配置。用户修改地区后，不需要重启应用；下一次“立即设置”、定时同步或磁贴同步会使用新配置。

## 错误处理

- 用户输入非法 market 时不保存，并显示 Toast。
- Bing API 因 market 不存在或网络异常失败时，沿用现有 `WallpaperSyncUseCase` 的失败结果和重试策略。
- JSON 中缺少 `urlbase` 或格式异常时，继续返回同步失败，不静默回退到默认市场，避免用户误以为自定义地区生效。

## 测试策略

`BingWallpaperSourceTest`：

- `buildApiUrl` 空 market 不包含 `mkt`。
- `buildApiUrl("en-WW")` 包含 `mkt=en-WW`。
- `parseJson(..., "UHD", "cn.bing.com")` 生成 `https://cn.bing.com/..._UHD.jpg`。
- 默认 `parseJson(json, "UHD")` 仍生成当前默认 host。
- `new BingSourceOptions("zh-CN")` 推导出 `cn.bing.com`。
- `new BingSourceOptions("en-WW")` 推导出 `www.bing.com`。

`SettingsStoreTest`：

- 默认 market 为空。
- 保存并读取 market。
- `zh-CN` market 生成的 `BingSourceOptions` 使用 `cn.bing.com`。

UI 可用 Robolectric 做轻量测试，风险可控时也可只覆盖 store/source：

- 点击地区项后弹出选择框。
- 选择预设 market 后写入 `SettingsStore`。

## 兼容性

- `minSdk=29`，可使用平台 `AlertDialog` 和 `EditText`。
- 不新增依赖。
- 不改变 Manifest 权限。
- 老用户升级后没有新 key，默认值会保持当前行为。

## 实施顺序

1. 增加 `BingSourceOptions` 和 `SettingsStore` 配置读写。
2. 为 `BingWallpaperSource` 增加 API URL 构造和 market-derived image host。
3. 修改 `SyncWallpaperService` 注入最新配置。
4. 修改 `activity_setting.xml` 和 `SettingActivity`，新增地区设置入口。
5. 补充单元测试并运行 `testDebugUnitTest`、`assembleDebug`。

## 成功标准

- 不配置 market 时，生成的 API URL 与当前默认行为一致。
- 配置 `en-WW` 后，请求 URL 包含 `mkt=en-WW`。
- 配置 `zh-CN` 后，最终图片 URL host 为 `cn.bing.com`。
- 配置其它地区或默认地区后，最终图片 URL host 为 `www.bing.com`。
- 配置变更在下一次同步生效。
- debug 构建和相关单元测试通过。

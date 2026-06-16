# Bing 壁纸归档源调研

调研时间：2026-06-16

## 背景

当前应用使用 Bing 公开接口：

```text
https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=zh-CN
```

实测该接口适合获取近期首页壁纸，但不适合作为完整历史归档源。

## 官方接口限制

### 已确认行为

- `n` 单次最多稳定返回 8 张。
- `idx` 不是可靠的无限分页游标。
- 实测 `idx=16&n=8`、`idx=24&n=8` 会重复返回 `idx=8&n=8` 附近的数据。
- `idx=0&n=8` 与 `idx=8&n=8` 之间也可能有 1 张重叠。

### 参考来源

- Microsoft Q&A：Microsoft 外部员工回复称，公开接口的 `n` 参数只支持 `1` 到 `8`，没有受支持的方法下载全部历史首页图。  
  https://learn.microsoft.com/en-sg/answers/questions/1473961/bing-hpimagearchive-aspx-question
- blog.atwork.at：说明 `idx=0` 是当天、`idx=1` 是前一天，最多回溯 7 天；更大的 `idx` 会返回 7 天前图片。  
  https://blog.atwork.at/post/2020/use-the-daily-bing-picture-in-teams-calls/
- Stack Overflow：社区观察到 `n=8` 能取 8 张，`idx=8&n=8` 能再取前一批，但没有办法继续扩展。  
  https://stackoverflow.com/questions/10639914/is-there-a-way-to-get-bings-photo-of-the-day

## 候选三方归档源

### 1. npanuhin/Bing-Wallpaper-Archive

地址：

- GitHub: https://github.com/npanuhin/Bing-Wallpaper-Archive
- API 示例: https://bing.npanuhin.me/CN-zh.2026.06.min.json

特点：

- 免费、开源，MIT license。
- 提供静态 JSON API 文件。
- 支持按国家/语言、年份、月份获取数据。
- 支持中国区：`CN-zh`。
- README 声明 API 文件可自由访问，没有频率或流量限制。
- 图片 URL 使用该项目自己的存储地址，不完全依赖 Bing 旧原始 URL。

端点形态：

```text
https://bing.npanuhin.me/{country}-{language}.json
https://bing.npanuhin.me/{country}-{language}.{YYYY}.json
https://bing.npanuhin.me/{country}-{language}.{YYYY}.{MM}.json
https://bing.npanuhin.me/{country}/{language}/{YYYY-MM-DD}.jpg
```

数据字段示例：

```json
{
  "title": "Example title",
  "caption": "Example caption",
  "subtitle": "Example subtitle",
  "copyright": "Copyright",
  "description": "Description",
  "date": "2024-01-16",
  "bing_url": null,
  "url": "https://bing.npanuhin.me/US/en/2024-01-16.jpg"
}
```

风险：

- 不是 Microsoft 官方服务。
- 图片仍受原版权方限制。
- README 建议图片下载保持合理量，例如不要超过每天一个完整归档。

适配建议：

- 最适合作为本项目的“历史归档源”。
- 可以保留当前 Bing 官方源用于最新图片，新增 npanuhin 源用于历史画廊。
- 分页可以按年月 JSON 拉取，再在前端或 core 层统一映射为 `WallpaperItem`。

### 2. Peapix

地址：

- Gallery: https://peapix.com/bing
- API: https://peapix.com/api
- API 示例: https://peapix.com/bing/feed?country=cn&n=3

特点：

- 免费 API。
- 支持 Bing 和 Windows Spotlight 图片。
- 支持国家参数：`au`、`br`、`ca`、`cn`、`de`、`fr`、`in`、`it`、`jp`、`es`、`gb`、`us`。
- API 数据缓存 30 分钟。
- 页面有多页画廊。

API 字段示例：

```json
{
  "title": "七英里海滩附近的玳瑁海龟伴侣，大开曼岛，开曼群岛",
  "copyright": "© Alex Mustard/Nature Picture Librar",
  "fullUrl": "https://img.peapix.com/example_1920.jpg",
  "thumbUrl": "https://img.peapix.com/example_640.jpg",
  "imageUrl": "https://img.peapix.com/example.jpg",
  "pageUrl": "https://peapix.com/bing/56495",
  "date": "2026-06-16"
}
```

风险：

- API 文档主要描述 latest feed，不像 npanuhin 那样明确提供按年月归档 JSON。
- 免费第三方服务，稳定性和长期可用性需观察。

适配建议：

- 适合做近期图片补充源。
- 不建议作为完整历史归档主源。

### 3. TimothyYe/bing-wallpaper

地址：

- GitHub: https://github.com/TimothyYe/bing-wallpaper
- API: https://bing.biturl.top

特点：

- 免费、开源，Apache-2.0 license。
- 提供 REST API。
- 支持 `json` 或图片重定向。
- 支持多个市场和多种分辨率。

限制：

- README 中 `index` 从 0 开始，`random` 只在 0 到 7 之间选择。
- 本质更接近 Bing 官方近期接口封装，不适合扩展历史画廊。

适配建议：

- 可作为官方近期接口的替代封装。
- 不建议作为历史归档主源。

### 4. zenghongtu/bing-wallpaper

地址：

- GitHub: https://github.com/zenghongtu/bing-wallpaper
- API: https://bingw.jasonzeng.dev

特点：

- 免费、开源，MIT license。
- 支持按 `index`、`date`、`random` 获取图片。
- README 声明 `date` 支持从 `20190309` 到今天。
- 实测 `index=-1` 可返回早期图片。

限制：

- 个人服务，稳定性、限流和长期可用性不如静态归档透明。
- 响应中的图片 URL 仍指向 Bing 图片地址，旧图长期有效性需要验证。

适配建议：

- 功能上可以作为历史查询候选。
- 如果接入，建议作为可选源，而不是默认源。

### 5. bingwallpaper.anerg.com

地址：

- https://bingwallpaper.anerg.com/

特点：

- 页面显示多国家归档。
- 页面可追溯到 2009 年左右。
- 支持中国、美国、日本、英国、德国等地区。

限制：

- 未看到清晰公开 API 文档。
- 更像网页归档，不适合直接作为应用数据源。

适配建议：

- 可作为人工参考或备用调研对象。
- 不建议在当前客户端中直接抓取网页。

## 结论

推荐优先级：

1. `npanuhin/Bing-Wallpaper-Archive`：最适合作为历史归档主源。
2. `Peapix`：适合作为近期图片补充源。
3. `zenghongtu/bing-wallpaper`：可作为可选历史查询源，但不建议默认依赖。
4. `TimothyYe/bing-wallpaper`：适合作为近期 Bing API 封装，不解决历史分页问题。
5. `bingwallpaper.anerg.com`：归档范围大，但缺少明确 API，不建议直接集成。

建议后续方案：

- 保留当前 Bing 官方源作为默认最新源。
- 新增“历史归档源”配置，首选 npanuhin。
- 将不同来源统一映射到现有 `WallpaperItem`。
- 对三方源增加来源标识、错误提示和去重逻辑。
- 遵守图片版权限制，仅用于壁纸用途。

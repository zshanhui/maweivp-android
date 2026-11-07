# MaweiVP

An Android client for VLESS + REALITY, built on [Xray core](https://github.com/XTLS/Xray-core).

中国大陆用户的 MaweiVP 客户端，基于 Xray 内核。

[![API](https://img.shields.io/badge/API-24%2B-yellow.svg?style=flat)](https://developer.android.com/about/versions/lollipop)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.4.0-blue.svg)](https://kotlinlang.org)

---

## About this project / 关于本项目

MaweiVP is a fork of [v2rayNG](https://github.com/2dust/v2rayNG) by [2dust](https://github.com/2dust),
hard-forked for our own service and UI. The upstream project and this fork are both distributed under
the [GNU GPL v3](LICENSE); all original copyright notices are retained, and this repository is the
complete corresponding source for every APK we publish.

MaweiVP 是 [v2rayNG](https://github.com/2dust/v2rayNG) 的衍生分支（GPL-3.0），
仅用于自有服务与界面。本项目与 2dust / v2rayNG 官方无关，也不代表其立场。

What differs from upstream:

- Own application id `com.wuweilabs.maweivp`, so it installs alongside v2rayNG without conflicting.
- QR-code-only import, and only VLESS profiles are accepted from scanned or pasted links.
- Own theme (port palette), dark-only UI.
- Links (source, issues, privacy policy, update check) point at this repository, never upstream.

---

## Download / 下载

Builds are published on this repository's releases page:

构建产物发布在本仓库的 Releases 页面：

[https://github.com/zshanhui/maweivp-android/releases](https://github.com/zshanhui/maweivp-android/releases)

Release signing and GPG verification are not configured yet — until they are, treat any APK you did
not build yourself as untrusted.

---

### Geoip and Geosite

- geoip.dat and geosite.dat files live in `Android/data/com.wuweilabs.maweivp/files/assets`
  (path may differ on some devices)
- the download feature fetches an enhanced build from
  [this repo](https://github.com/Loyalsoldier/v2ray-rules-dat) (needs a working proxy)
- the official [domain list](https://github.com/Loyalsoldier/v2ray-rules-dat) and
  [ip list](https://github.com/Loyalsoldier/geoip) can also be imported manually
- a third-party dat file in the same folder works too, e.g.
  [h2y](https://guide.v2fly.org/routing/sitedata.html#%E5%A4%96%E7%BD%AE%E7%9A%84%E5%9F%9F%E5%90%8D%E6%96%87%E4%BB%B6)

### Geoip 与 Geosite

- geoip.dat 和 geosite.dat 文件位于 `Android/data/com.wuweilabs.maweivp/files/assets`（部分设备路径可能不同）
- 下载功能将获取该 [仓库](https://github.com/Loyalsoldier/v2ray-rules-dat) 中的增强版本（注意：此功能需要一个可用的代理）
- 最新官方 [域名列表](https://github.com/Loyalsoldier/v2ray-rules-dat) 和 [IP 列表](https://github.com/Loyalsoldier/geoip) 可手动导入
- 也可在同一文件夹中使用第三方 dat 文件，例如 [h2y](https://guide.v2fly.org/routing/sitedata.html#%E5%A4%96%E7%BD%AE%E7%9A%84%E5%9F%9F%E5%90%8D%E6%96%87%E4%BB%B6)

---

## Development guide / 开发指南

### Note

- The Android project under `V2rayNG/` builds with the Gradle wrapper, no Android Studio required.
  See `AGENTS.md` (and the scoped guides it lists) for the validated build loop.
- `V2rayNG/app/libs/` is not tracked: it needs `libv2ray.aar` from
  [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) and the `libhev-socks5-tunnel`
  libraries produced by `compile-hevtun.sh`.
- MaweiVP runs on Android Emulators. For WSA, grant the VPN permission with
  `appops set com.wuweilabs.maweivp ACTIVATE_VPN allow`.

### 提示

- `V2rayNG/` 下的 Android 项目使用 Gradle wrapper 即可编译，无需 Android Studio。构建与校验流程见 `AGENTS.md`。
- `V2rayNG/app/libs/` 不在版本库中：需要来自
  [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) 的 `libv2ray.aar`，以及由
  `compile-hevtun.sh` 生成的 `libhev-socks5-tunnel` 系列库。
- MaweiVP 可在 Android 模拟器上运行。对于 WSA，需要通过
  `appops set com.wuweilabs.maweivp ACTIVATE_VPN allow` 授予 VPN 权限。

---

## Community / 社区

Issues: [https://github.com/zshanhui/maweivp-android/issues](https://github.com/zshanhui/maweivp-android/issues)

No Telegram channel is operated for this fork. / 本分支暂不运营 Telegram 频道。

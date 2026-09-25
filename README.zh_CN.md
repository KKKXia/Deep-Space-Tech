<div align="center">
  <p><b>中文</b> | <a href="README.md">English</a></p>

  <h1>Deep-Space-Tech</h1>

  <p>
    <a href="https://github.com/KKKXia/Deep-Space-Tech/releases/latest">
      <img src="https://img.shields.io/github/v/release/KKKXia/Deep-Space-Tech?style=for-the-badge&color=orange" alt="Latest Version">
    </a>
    <a href="https://mit-license.org/">
      <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
    </a>
  </p>

</div>

<hr>

## ✨ 核心特性

* **多流体槽单方块**：一台机器同时有 6 格物品输入、6 格物品输出、6 路流体输入、6 路流体输出——GT 自己的单方块机器只有 1 个流体罐，跑不了多方块的配方。
* **24 个等级**：LV..UMV（12 台多流体槽电解机 + 12 台多流体槽化学反应釜），每台由同等级的 GT 电解机 / 化学反应釜在工作台中无序 1:1 合成。
* **等级对应容量**：单罐容量为 GT 同等级单方块容量的一半，上限 256,000 mB。

<hr>

## 📥 下载与兼容性

| GTNH 版本 | GT5-Unofficial | Mod 版本 |        状态        | 下载                                                                                                                |
|:---------:|:--------------:|:--------:|:------------------:|:--------------------------------------------------------------------------------------------------------------------|
| **2.9.0-rc1** |  `5.09.54.183`  | `v0.1.0-rc1` | ✅ **推荐使用**     | [![Latest](https://img.shields.io/badge/Download-Latest-success)](https://github.com/KKKXia/Deep-Space-Tech/releases/latest) |

## 🧱 依赖要求

* **运行环境**：Minecraft `1.7.10`、Forge `10.13.4.1614`、GTNH `2.9.0-rc1` 及以上（GT5-Unofficial `5.09.54.183`）。
* **开发环境**：JDK `25`；构建命令 `gradlew.bat clean build`（Linux/macOS 用 `./gradlew clean build`），产物在 `build/libs/`。
* 本 mod 占用 GT 的机器 id 区间 `31611..31622` 与 `31631..31642`；若与其它 addon 冲突，启动时会直接报错并指出占用者。

## 📜 鸣谢

| 项目 | 作者 | 许可 |
|:-----|:-----|:-----|
| **GT5-Unofficial**（平台、配方、多方块） | [GTNewHorizons](https://github.com/GTNewHorizons) | [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html) |
| **ModularUI / ModularUI2**（机器界面） | [GTNewHorizons](https://github.com/GTNewHorizons) | [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html) |
| **GTNH 构建脚手架 / 示例模版** | [GTNewHorizons](https://github.com/GTNewHorizons) | [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html) |

本 mod 以 **MIT** 许可发布，见 [LICENSE](LICENSE)。

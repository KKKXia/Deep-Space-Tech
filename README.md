<div align="center">
  <p><b>English</b> | <a href="README.zh_CN.md">中文</a></p>

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


## ✨ Key Features

* **Multi-tank single blocks**: 6 item inputs, 6 item outputs, 6 fluid inputs and 6 fluid outputs on one block — GT's own single blocks have exactly one fluid tank, which cannot run multiblock recipes.
* **24 tiers**: LV through UMV (12 electrolysers + 12 chemical reactors), each a shapeless 1:1 conversion of the same-tier GT electrolyser / chemical reactor.
* **Tier-appropriate tanks**: per-tank capacity is half of GT's same-tier single-block capacity, capped at 256,000 mB.

<hr>

## 📥 Downloads & Compatibility

| GTNH Version | GT5-Unofficial | Mod Version |       Status       | Download                                                                                                            |
|:------------:|:--------------:|:-----------:|:------------------:|:--------------------------------------------------------------------------------------------------------------------|
| **2.9.0-rc1** |  `5.09.54.183`  | `v0.1.0-rc1` | ✅ **Recommended** | [![Latest](https://img.shields.io/badge/Download-Latest-success)](https://github.com/KKKXia/Deep-Space-Tech/releases/latest) |

## 🧱 Dependency Requirements

* **Runtime**: Minecraft `1.7.10`, Forge `10.13.4.1614`, GTNH `2.9.0-rc1`+ (GT5-Unofficial `5.09.54.183`).
* **Development**: JDK `25`; build with `gradlew.bat clean build` (`./gradlew clean build`), output in `build/libs/`.
* The mod occupies GT meta tile entity ids `31611..31622` and `31631..31642`; a conflict with another addon fails startup with an explicit message.

## 📜 Credits

| Project | Author | License |
|:--------|:-------|:--------|
| **GT5-Unofficial** (platform, recipes, multiblocks) | [GTNewHorizons](https://github.com/GTNewHorizons) | [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html) |
| **ModularUI / ModularUI2** (machine GUI) | [GTNewHorizons](https://github.com/GTNewHorizons) | [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html) |
| **GTNH buildscript / example mod scaffolding** | [GTNewHorizons](https://github.com/GTNewHorizons) | [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html) |

Licensed under **MIT** — see [LICENSE](LICENSE).

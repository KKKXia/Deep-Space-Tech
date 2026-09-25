# Deep-Space-Tech

GregTech: New Horizons 的附属 mod（Minecraft 1.7.10）。不重造 GT 已有的轮子：电力、算力、配方、材料、
多方块结构、GUI 与 NEI 全部直接复用 [GT5-Unofficial](https://github.com/GTNewHorizons/GT5-Unofficial)。

## 内容

**多流体槽电解机 / 多流体槽化学反应釜（LV..UMV，共 24 台）**

- 单方块机器：6 物品输入 + 6 物品输出 + 6 路流体输入 + 6 路流体输出；
- 配方池直接复用 GT 多方块的工业电解机（`electrolyzerNonCellRecipes`）与大型化学反应釜
  （`multiblockChemicalReactorRecipes`），因此 NEI 配方页与那两台多方块机器共用同一页；
- 机器界面按该配方池的 NEI 布局排布，流体槽与物品槽逐列对齐；
- 每台由同等级的 GT 电解机 / 化学反应釜在工作台中无序 1:1 合成。

示例内容（示例单方块与多方块机器、示例材料、启动自检）**只在 dev 环境注册**，正式环境不出现。

## 构建

```
./gradlew build
```

产物在 `build/libs/`，文件名形如 `deepspacetech-<版本>-dev.jar`。GT5-Unofficial、StructureLib、
GTNHLib、IC2、ModularUI2 等依赖由 `dependencies.gradle` 传递而来，无需手动放置。

## 开发运行

```
./gradlew runClient
./gradlew runServer
```

dev 环境启动时会执行自检（用真实配方池跑通机器判定、校验 24 条转换配方确实进了合成表），
失败会直接把问题挡在启动阶段而不是留到游戏里。

## 许可

MIT，见 [LICENSE](LICENSE)。

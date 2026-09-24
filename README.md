# More Mooncake 🥮

> **Mid-Autumn Festival mooncakes for Minecraft 1.21.1** — a cross-platform (Fabric + NeoForge)
> mod built on [Architectury](https://docs.architectury.dev). Craft 40 mooncake slices
> (5 flavours × 8 copper-style oxidation states), assemble grand mooncakes from 8 slices + 1 egg,
> and eat them wedge by wedge. Optional [JEI](https://www.curseforge.com/minecraft/mc-mods/jei)
> integration shows both custom recipes.
> 中文说明见下 ↓

快到中秋节了，请大家吃月饼！一个跨平台（Fabric + NeoForge）的 Minecraft 1.21.1 模组，
基于 [Architectury](https://docs.architectury.dev) 一次编写、双平台产出。

## 内容

### 🍰 大月饼（八块拼装系统）

- **小块（小月饼）**：5 种口味 × 8 种铜氧化状态 = **40 种**，扇形切片造型，营养 1，单吃即生效。
- **大月饼**：8 个小块围绕 1 个鸡蛋拼装而成。放置后是**真正的八块扇形月饼**（8 个扇区，不是蛋糕造型），
  吃掉一块就少一块，**不能再手动拆分**。
- 大月饼的 8 个槽位 = 合成时**从左上角顺时针**的外圈摆放顺序，**吃的时候按顺序**触发对应小块的 (口味 × 状态) 效果：
  第 1 口 = 左上角那一块，第 8 口 = 最后一块。
- **名称直接显示顺序全称**：如「大月饼：五仁月饼块 → 豆沙月饼块 → 苏子月饼块 → …」。
- **材质对应材料**：每块扇形按对应小块的馅料颜色上色（氧化状态混入锈/绿调，涂蜡偏亮），一眼看出"是什么拼成的、吃到哪一块了"。
- **碰撞箱/轮廓 = 模型 = 渲染**：三者由同一份几何数据（`MooncakeGeometry`）生成，形状永远一致。
- 组合数：小块 40 种；大月饼 = 8 槽位任意小块 → 40⁸ ≈ 6.6 万亿种（同口味内也是 8⁸/口味）。

| 口味 | 注册名前缀 | 核心效果 |
| --- | --- | --- |
| 五仁 | `wuren` | 力量 (Strength) |
| 豆沙 | `dousha` | 再生 (Regeneration) |
| 苏子 | `suzi` | 跳跃提升 (Jump Boost) |
| 红枣 | `hongzao` | 伤害吸收 (Absorption) |
| 咸蛋黄 | `xianyadan` | 抗性提升 (Resistance) |

每种口味的 8 种状态（模仿铜的氧化体系）：

- 月饼（新鲜）→ 锈蚀的 → 斑驳的 → 氧化的（tier 1-3）
- 涂蜡的 / 涂蜡的锈蚀的 / 涂蜡的斑驳的 / 涂蜡的氧化的

### 效果机制

- 氧化程度越高，主效果等级越高（I→IV）、持续时间越长（15s→30s）。
- **涂蜡**＝“保鲜”：主效果持续时间翻倍，且不会再氧化。
- **氧化且未涂蜡**的月饼已经放坏了：还会给你 4 秒反胃 (Nausea)！
  所以要么吃新鲜的，要么记得涂蜡保鲜。

### 合成配方

**小块基础合成**（生存获取入口，每种口味 1 条）：
- `小麦 + 南瓜种子` → 五仁月饼块
- `小麦 + 可可豆` → 豆沙月饼块
- `小麦 + 甜菜根` → 苏子月饼块
- `小麦 + 甜浆果` → 红枣月饼块
- `小麦 + 鸡蛋` → 咸蛋黄月饼块

**纯大月饼配方**（配方书可见，8 同块 + 蛋）：
- 工作台 3×3：中间 1 个鸡蛋，外圈 8 格放**同一种**小块（同口味同状态）→ 对应的纯大月饼。
- 5 口味 × 8 状态 = 40 条，配方书中按组折叠展示；槽位顺序 = 左上起顺时针，吃的时候按同顺序。

**拼装**（自定义配方 `mooncake_assembly`，什锦/混态专用）：
- 工作台 3×3：**中间放 1 个鸡蛋，外圈 8 格放任意小块**（可不同口味、不同状态）→ 1 个大月饼。
- 槽位顺序 = 外圈**从左上角起顺时针**：左上 → 上中 → 右上 → 右中 → 右下 → 下中 → 左下 → 左中。

**拆分 / 回收**：
- 大月饼**不能**手动拆分。想吃回来只有两种办法：
  - **斧头右键**切开放置的大月饼 → 返还**还没吃掉**的小块（按原顺序），斧头耐久 -1；
  - **用镐/空手挖掉**放置的大月饼 → 返还"吃过几口"的大月饼物品本身（可以再放下继续吃）。
- **食用不返还小块**：每吃一口只是吃掉那一块并触发它的效果。

**涂蜡 / 刮蜡 / 氧化 / 除锈链**（作用于小块）：
- 涂蜡：`任意未涂蜡小块 + 蜂巢` → 对应涂蜡版
- 氧化链：`小块 + 铁粒` → 锈蚀的；`锈蚀的 + 铜锭` → 斑驳的；`斑驳的 + 铜块` → 氧化的
- **刮蜡 / 除锈**（同一个自定义配方 `mooncake_scrape`）：`1 个小块 + 1 把斧头`，
  **斧头不消耗**，只掉 1 点耐久留在合成格子里（和刮铜块一个手感）：
  - 涂蜡的小块 → 刮掉蜡，保留氧化状态
  - 氧化的 → 斑驳的 → 锈蚀的 → 新鲜的（逐级除锈）
  - 新鲜小块没有可刮的东西，不会触发该配方

所有物品在创造标签页 **更多月饼 / More Mooncake** 中展示（含两个大月饼样板：纯五仁大月饼、什锦大月饼）。

## 工程结构（Architectury）

```
common/    共享代码与资源（物品/方块/方块实体/配方注册、效果、贴图、模型、语言、配方）
fabric/    Fabric 入口点与 fabric.mod.json
neoforge/  NeoForge 入口点与 neoforge.mods.toml
tools/     TextureGen.java / ResourceGen.java —— 重新生成贴图与数据（见下）
```

跨平台逻辑全部在 common 中通过 Architectury API 实现
（`DeferredRegister`、`CreativeTabRegistry`），两个平台只保留各自入口点。
新增内容同样在 common：`MooncakeBlock`（蛋糕式 8 bite + BlockEntity 槽位）、
`WholeMooncakeItem`（放置/拆分/NBT 槽位）、`MooncakeAssemblyRecipe`（自定义拼装配方）。

## 版本组合（Minecraft 1.21.1）

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| Gradle | 8.8（wrapper 内置） |
| Architectury Loom | 1.7-SNAPSHOT |
| Architectury Plugin | 3.4-SNAPSHOT |
| Architectury API | 13.0.8 |
| Fabric Loader | 0.16.10 |
| Fabric API | 0.103.0+1.21.1 |
| NeoForge | 21.1.238 |
| JEI（可选，两平台同版） | 19.57.0.448 |

## 构建

```bash
# 需要 JDK 21（JAVA_HOME 指向 JDK 21）
./gradlew :fabric:build :neoforge:build
```

产物（已按加载器区分文件名）：
- `fabric/build/libs/more-mooncake-fabric-1.0.0.jar`
- `neoforge/build/libs/more-mooncake-neoforge-1.0.0.jar`

## 运行

- **Fabric**：需安装 Fabric Loader 0.16.x + [Fabric API](https://modrinth.com/mod/fabric-api) + [Architectury API](https://modrinth.com/mod/architectury-api)（Fabric 版）
- **NeoForge**：需安装 [NeoForge 21.1.238+](https://neoforged.net/) + Architectury API（NeoForge 版）
- **JEI（可选）**：[JEI 1.21.1](https://www.curseforge.com/minecraft/mc-mods/jei)（Fabric 版 / NeoForge 版）装上后能看到本模组的两个自定义配方
  （`拼装大月饼`、`刮蜡与除锈`）；不装 JEI 完全不影响游玩。

将对应 jar 放入 `mods/` 文件夹即可。进游戏后在创造模式物品栏搜索 “Mooncake” 即可看到全部内容。
开发环境直接跑 `./gradlew :neoforge:runClient`（或 `:fabric:runClient`）。

## 发布 / CI

- GitHub Actions（`.github/workflows/build.yml`）会在 `push`/`PR` 时自动构建双平台并上传 jar 产物；
- 打 `v*` 格式的 tag（如 `v1.0.0`）时会额外自动创建 GitHub Release 并附带两个 jar。
- 发布到 Modrinth / CurseForge 时，建议分别上传对应加载器的 jar 并在依赖里声明
  Architectury API（必需）与 JEI（可选）。

## 重新生成资源

资源生成分成两个独立工具，职责清晰、互不覆盖：

```bash
# 1) 贴图（16x16）：40 个切片、整饼图标、方块贴图、模组图标、预览图
java tools/TextureGen.java .

# 2) 数据与模型（方块模型/blockstate/几何表/语言/配方）
java tools/ResourceGen.java .
```

- `TextureGen` 在 64×64 画布上作画（超采样：渐变、果仁颗粒、铜锈斑、涂蜡亮膜），
  最后盒式降采样到 **16×16** 原版尺寸，并额外输出 `tools/preview_grid.png` 预览图
  （8 列 = 8 种氧化状态，5 行 = 5 种口味，最后一行 = 整饼/top/side/inside/bottom）。
- `ResourceGen` 只写模型与数据。方块模型的面会带上**按世界坐标算出的 uv**，
  让扇形阶梯盒拼成一整块连续贴图；因此 `mooncake_side`/`mooncake_inside`
  的有效内容画在贴图下半部（uv v 8..16 对应 8px 高的饼身）。
- 贴图是确定性的（随机种子取自贴图名），反复生成结果完全一致。

> ⚠️ 注意：Minecraft 1.21 起数据包配方目录从 `recipes`（复数）改名为
> `recipe`（单数）。`ResourceGen` 已生成到 `data/<modid>/recipe/`；
> 若手动添加配方 JSON，务必使用单数目录，否则游戏会静默忽略全部配方。

中秋快乐！🌕🥮

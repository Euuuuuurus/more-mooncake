# More Mooncake — Seedream 材质生成指令

> 用途：把下面的内容转述给能调用 **seedream 模型**的 AI agent，让它绘制本模组全部月饼纹理。
> 生成后把 PNG 按命名规范存到 `tools/seedream_out/`，再交给开发侧缩成 16×16 集成（见文末）。

---

## 一、总体要求（先对 seedream agent 说的话）

请为一款《我的世界》(Minecraft) Java 版模组绘制一组**像素艺术风格 (pixel art)** 的月饼贴图。
所有图统一要求：**Minecraft 传统 16×16 像素画质感、俯视 45°、单个月饼居中占比约 75%、纯白色背景、清晰轮廓、边缘平滑、细节丰富、上色干净、阴影自然**。
每张图输出 **1024×1024 正方形 PNG**，主体占画面 70–80%，背景为**纯白**（便于后续抠图）。

---

## 二、风格基础模板（每张图都以此为基础，再叠加口味/状态差异）

**中文：**
Minecraft 像素艺术风格游戏贴图，16×16 像素分辨率采样，高清细节。单个广式月饼，俯视正上方视角，居中占画面 75%，纯白色纯色背景，无文字无边框无杂物。圆润金色烘焙饼皮，表面有细腻烘烤纹理与轻微高光，轮廓清晰，边缘抗锯齿，柔和的顶光阴影。画面干净、颜色饱满、适合作为游戏物品图标。

**English:**
Minecraft pixel art game texture, 16x16 pixel resolution sampling, high detail. A single Cantonese mooncake, top-down straight view, centered and filling 75% of the canvas, plain solid white background, no text no border no extra objects. Round golden-brown baked crust with fine baking texture and subtle highlights, crisp silhouette, anti-aliased edge, soft top-lit shading. Clean, saturated colors, suitable as a game item icon.

> 提示：告诉 seedream「先按此模板生成一张 16×16 概念验证图」，满意后再批量；或直接逐组生成。

---

## 三、需要生成的图（共 46 张）

### A. 40 张小块（扇形切片，占一半以上工作量）
- 命名：`tools/seedream_out/slice_<flavor>_<state>.png`
- flavor（口味）：`wuren`（五仁）、`dousha`（豆沙）、`suzi`（苏子）、`hongzao`（红枣）、`xianyadan`（咸蛋黄）
- state（状态）：`t0`（新鲜）、`t1`（铜锈）、`t2`（风化）、`t3`（氧化），再加 `waxed_` 前缀 = 涂蜡版
  - 即：`slice_wuren_t0.png`、`slice_wuren_t1.png` … `slice_xianyadan_t3.png` 共 20 张
  - 涂蜡版：`slice_wuren_waxed_t0.png` … 共 20 张
- 造型：一块**扇形月饼切片（八分之一圆饼）**，横放居中；能看到两侧直边切面和外侧弧边；饼皮 + 内馅在切面可见。

**切片造型模板（英文）：**
one-eighth slice of a round mooncake pie, pie wedge shape, lying flat, centered; crisp straight cut faces on both straight sides showing the filling, rounded outer crust arc on the third side; Minecraft pixel art, top-down view, plain white background.

### B. 大月饼物品图标（1 张）
- 命名：`tools/seedream_out/mooncake.png`
- 造型：**完整圆饼**，顶面有 8 条从圆心放射的浅切痕（把饼均匀分成 8 块）、中心一枚**红底金「月」字方形印章**。

**提示词（英文）：**
whole round Cantonese mooncake, top-down view, crust with 8 subtle radial cut lines dividing it into 8 equal wedges, a small red square seal stamp with a golden Chinese "moon" glyph in the very center, Minecraft pixel art, centered, plain white background.

### C. 大月饼方块纹理（4 张）
- 命名：`tools/seedream_out/mooncake_top.png`、`mooncake_side.png`、`mooncake_bottom.png`、`mooncake_inside.png`
- top：顶面 = 完整圆饼 + 8 条放射切痕 + 中心红印（同 B，但**不要**物品图标那种深边框）
- side：侧面 = 圆形饼的竖切侧影（厚约 3 格），上下面饼皮、中间可见内馅横断面
- bottom：底面 = 浅金烤痕饼底，纯色为主，略有烘烤焦斑
- inside：内馅截面 = 奶黄色（淡米色）面皮横截面，带细微孔洞质感（大月饼被切开后露出的截面纹理）

---

## 四、口味差异化（叠加到切片切面/内馅，写入每张图的提示词）

| flavor | 中文 | 内馅描述 | 提示词关键词（英文） |
| --- | --- | --- | --- |
| wuren | 五仁 | 淡米黄饼皮 + 五彩坚果碎（花生/核桃/瓜子/杏仁/芝麻），切面五彩果仁颗粒 | mixed five-seed & nut filling, cream-tan paste with scattered colorful chopped nuts (peanut, walnut, sunflower seed, almond, sesame) |
| dousha | 豆沙 | 深红棕色细腻红豆沙馅 | smooth dark reddish-brown sweet red bean paste filling |
| suzi | 苏子 | 深紫紫苏籽馅，带细小深紫籽粒 | deep purple perilla-seed (shiso) filling with tiny dark purple seeds |
| hongzao | 红枣 | 深枣红色枣泥馅 | dark jujube-red red-date paste filling |
| xianyadan | 咸蛋黄 | 金黄流心咸蛋黄馅，中心亮黄流心 | golden salty egg-yolk filling with a bright glossy molten-yolk core |

**中英文都加**：例如「豆沙小块：深红棕红豆沙内馅，切面细腻无颗粒」。

---

## 五、氧化状态差异化（叠加到饼皮表面，写入提示词）

饼皮表面出现"铜氧化"色斑（模拟铜锈），越高级越明显；涂蜡版饼皮表面有**光滑亮膜高光**（像打了蜡）。

| state | 说明 | 提示词关键词（英文） |
| --- | --- | --- |
| t0 | 新鲜，无锈 | （不额外加） |
| t1 rusted | 少量红铜色锈斑 | scattered small reddish-copper rust spots on the crust |
| t2 weathered | 更多青绿铜锈 + 少量锈斑 | more teal-green copper patina patches plus some rust spots on the crust |
| t3 oxidized | 大面积青绿铜绿、带暗斑 | large areas of teal-green copper patina with dark mottled patches, crust looks weathered |
| waxed_* | 涂蜡版（保持对应 t 级的锈，但表面有光泽亮膜） | shiny glossy waxed varnish sheen on the crust surface, light reflections |

组合示例：`slice_suzi_weathered.png` = 苏子深紫内馅 + 饼皮青绿铜锈斑。

---

## 六、输出硬性规范（对 seedream agent 的要求）

1. 每张：1024×1024 正方形、PNG、像素艺术风、单主体居中、**纯白背景**。
2. 保存：`tools/seedream_out/<上面规定的名字>.png`。
3. 每张完成后自检：是否居中、是否单主体、背景是否纯白、切面/馅料/锈色是否符合指定颜色。不合格重画。
4. 涂蜡版（waxed_）必须明显比同 tier 的非涂蜡版"亮/有光泽"，否则视为失败。

---

## 七、生成后的集成（开发侧，供参考）

1. 用最近邻采样把 1024×1024 缩到 16×16（保留锐利像素感；不要用模糊双线性）。
2. 物品图标（`slice_*`、`mooncake.png`）抠除白色背景 → 透明底；方块纹理（top/side/bottom/inside）保留纯色底。
3. 替换：把成品放入 `common/src/main/resources/assets/more_mooncake/textures/item|block/` 对应文件名（slice 需按模组实际注册名映射），并让 `tools/ResourceGen.java` 跳过程序化纹理生成（或改为直接复制成品）。

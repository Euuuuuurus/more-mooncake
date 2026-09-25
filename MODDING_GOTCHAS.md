# More Mooncake 模组开发全坑总结（用于改进 skill）

> 项目：Architectury 多平台（Fabric + NeoForge）MC 1.21.1 月饼模组
> 40 个月饼（5 口味 × 8 铜氧化状态）+ 大月饼拼装系统 + JEI 集成 + GitHub 发布
> 用法：每条都是「症状 → 根因 → 解法」，做下一个模组时逐条对照。

---

## 一、版本纪律（最先做，做错全盘皆输）

1. **不同 MC 时代的 Architectury 插件/API 差异极大**，一律以 generate.architectury.dev 生成器 + docs.architectury.dev 为准，绝不凭记忆猜版本。
2. 本工程锁定的可用组合（MC 1.21.1 时代）：
   - Architectury API `13.0.8`、architectury-plugin `3.4-SNAPSHOT`、loom `1.7-SNAPSHOT`
   - fabric-loader `0.16.10`、fabric-api `0.103.0+1.21.1`
   - **neoforge `21.1.238`**、Gradle `8.8`、Java 21
3. **NeoForge 版本必须满足依赖模组的最低要求**：JEI 19.57 要求 neoforge ≥21.1.238，本地 21.1.209 时 runClient 直接 FATAL `Mod jei requires neoforge 21.1.238 or above`。升级 gradle.properties 一个数字，代码零改动。
4. **Loom 版本决定能注入哪些 dev 依赖**：Loom 1.7 无法 `modRuntimeOnly` 注入用 Loom 1.18.2 构建的 JEI fabric 包（报 `Mod was built with a newer version of Loom`）→ 平台模块不放 JEI dev 依赖，common 只 `compileOnly` JEI common-api。
5. 版本全部集中到 `gradle.properties`，一处改全局生效。

## 二、Architectury 多平台纪律

1. common 只用统一 API：`DeferredRegister`（dev.architectury.registry）、`Platform`/`Env`、architectury 事件、`NetworkManager`；平台差异用 `@ExpectPlatform` 封装，实现放各平台模块。
2. 平台模块只留入口点（neoforge `@Mod` / fabric `ModInitializer`，都调同一个 common 初始化器）。
3. 平台类绝不泄漏进 common（`FabricLoader`、`NeoForge.EVENT_BUS`、`@SubscribeEvent` 一律禁止）。
4. 共享资源（assets/data JSON）放 common 的 `src/main/resources`，两平台共用一份，不重复维护。
5. 注册表键用 `modid:name`，资源路径与注册名严格一致。
6. 客户端专属代码用 `Platform.isClient()` 隔离。
7. **JEI 插件可以放 common**：`@JeiPlugin` 由 JEI 扫描，JEI 未装时该类不会被触碰 → 软依赖安全，JEI 只是 optional dependency。

## 三、MC 1.21.1 API 细节（本项目的坑王）

1. **配方目录是单数**：`data/<ns>/recipe/`（1.21.2 才改复数 `recipes/`）。写错目录配方静默不加载。
2. **shaped 配方输出带 NBT 物品**：1.21.1 用组件系统，result 里写
   `"components": {"minecraft:custom_data": {"Slices": [8 个 id]}}`
   （旧版 NBT tag 写法已失效）。
3. **物品标签**：`c:axes` 若未定义会导致配方加载失败（找不到 tag）；用原版 `minecraft:axes`。自定义 `c:` 命名空间 tag 必须自己定义。
4. **1.21.1 没有 RecipeDisplay 系统**！`net.minecraft.world.item.crafting.display` 包在 1.21.2/1.21.3 才引入。写 display 方法直接编译报"找不到包"。
5. 自定义 CRAFTING 配方用 `SimpleCraftingRecipeSerializer` 注册，可被工作台发现（`RecipeType.CRAFTING`）。
6. **`getRemainingItems` 让工具不消耗**：刮蜡配方里斧头 copy + `setDamageValue(+1)` 回原格 → 解决"刮一次蜡消耗整把斧子"。
7. **waxed→unwaxed 状态匹配按 tier 而不是字符串 suffix**：suffix `"waxed"` 对不上普通名，会永远匹配失败；自检能抓到这种逻辑 bug。

## 四、配方体系设计

1. 82 条配方 = slice(5) + wax(20) + oxidize(15) + grand(40) + assembly(1 自定义) + scrape(1 自定义)。
2. shaped 配方的 `group` 字段折叠配方书，同类型配方共用 group。
3. **生成工具写配方前先清空目录**：旧 json 残留会被游戏加载 → 曾导致 116 条（35 条废弃配方残留）bug。
4. 生成工具用固定种子 + itemId 派生随机数，保证重复生成结果一致。
5. 工具类（生成器/自检）放 `tools/` 或独立 dev 包，与游戏代码分离。

## 五、JEI 集成（19.x API，1.21.1）

1. 用 `mezz.jei.api.recipe.RecipeIngredientRole`、`RecipeType.create(ns, path, class)`、`registration.addRecipeCategories/addRecipes`。
2. **`IRecipeCategory` 用 `getWidth()/getHeight()/getIcon()/setRecipe()`**；`getBackground()` 已 deprecated（编译警告标为移除）。
3. 自定义配方没有现成 RecipeDisplay → 用内部 record 手造 JEI 展示对象（装配示例：8 不同小块 + 蛋 → 什锦大月饼；刮蜡示例：5 个状态转换）。
4. 槽位手画坐标：`builder.addSlot(role, x, y).addItemStack(...)`。

## 六、纹理与资源生成

1. 大图作画 → **盒式降采样**到目标尺寸（64→16，OUT=16 BOX=4），对比度 1.10，alpha≥128→255 硬边。
2. **当前模型不能看图片**（read_image 报不支持）→ 写 PNG→字符画工具（亮度映射 `" .:-=+*#%@"`）自查纹理，"自己看看"= 看字符画。
3. 斑驳/铜锈纹理要避开印章区域：`bakeMottle(rnd, wedgeOnly, count, rMin, rMax)` 限定半径范围。
4. 大月饼 8 扇形几何：`WEDGE_BOXES[8][7][6]` 一份数据，**模型 JSON / 碰撞箱 / BER 共用**，模型与碰撞必须一致（玩家会盯着拼装看）。
5. BER 用 sprite UV（与模型 up 面一致）+ tint 乘法着色（tintFor 预补偿 CRUST 基色），`RenderType.cutout()`，alpha 255。

## 七、构建与运行验证（防乱码防假成功）

1. 构建命令模板（PowerShell）：
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.12.1"
   $env:GRADLE_USER_HOME="<项目>\.build-tmp\gh"
   & <gradle-8.8>\bin\gradle.bat :fabric:build :neoforge:build
   ```
2. 输出重定向到日志文件再 grep（`[Console]::OutputEncoding=UTF8`），否则中文错误乱码。
3. **runClient/runServer 前后**：kill java 进程 + 删 `neoforge/run/world/session.lock`，否则 IOException 打不开世界。
4. loom runs 加 vmArgs：`-Xmx4G -Djava.io.tmpdir=...`。
5. **每完成一个子系统就在两个平台各自构建验证**，不攒到最后排错。
6. 内嵌自检代码（服务器启动时验证配方键、合成行为、模型元素数），`hadErrorsLoading()` 汇总加载错误。

## 八、本机工具链/网络坑（Windows 通用性很高）

1. **curl.exe 报 `SEC_E_NO_CREDENTIALS (0x8009030E)`、Invoke-WebRequest 报"基础连接已经关闭"** → Windows Schannel TLS 出站问题；但 **Java 网络栈完全正常**（gradle 下依赖、下 JEI jar 都行）。
2. 万能下载方案：写个 `DownloadUtil.java`（HttpURLConnection + 手动跟随重定向 + User-Agent），`java File.java download <url> <target>` 单文件运行。
3. **git 默认 schannel 后端同样报 SEC_E_NO_CREDENTIALS** → `git -c http.sslBackend=openssl` 解决（Git for Windows 自带 OpenSSL 后端）。
4. **winget 安装可能失败**（exit -1978335231 或无输出）→ 改直接下载官方安装包静默安装：
   `Git-...exe /VERYSILENT /NORESTART /NOCANCEL /SP- /SUPPRESSMSGBOXES /DIR="C:\Users\<user>\Git"`（装用户目录免 UAC）。
5. **git-for-windows 资产命名**：tag `v2.55.0.windows.3` → 资产名是**四段版本** `Git-2.55.0.3-64-bit.exe`（不是三段 `Git-2.55.0-64-bit.exe`，三段必 404）。MinGit/PortableGit 同理。
6. **setx 写入 User 级注册表后，已运行的进程不刷新**：`$env:X` 读不到，要用 `[System.Environment]::GetEnvironmentVariable("X","User")` 读注册表。
7. **敏感值（token）绝不打印/绝不进命令行参数**：从注册表读 → 塞进子进程环境变量 → HTTP header 注入（`AUTHORIZATION: basic base64(user:token)`），git 用 `-c http.extraHeader=`。

## 九、Git / GitHub / CI 发布坑

1. **PowerShell 把外部程序 stderr 当错误显示**：git 的进度输出都在 stderr，会显示成红色 "exit 1"，但实际成功。看 `* [new branch]` / `* [new tag]` 判断。
2. **PowerShell 传含内嵌引号的 JSON 给原生程序会破坏参数** → JSON 写文件，程序读文件。
3. GitHub API 未认证走共享 IP 限流（403 rate limit exceeded），**带 token 才有 5000/h**。
4. **Actions 内置 GITHUB_TOKEN 默认 Read-only**，`softprops/action-gh-release@v2` 生成 release notes 需要 write → 403 `Resource not accessible by integration`。
5. 修复：仓库 Settings → Actions → Workflow permissions → Read and write；或 API：
   `PUT /repos/{owner}/{repo}/actions/permissions/workflow`
   **body 字段名是 `default_workflow_permissions`（值 "write"），不是 `default_permissions`** —— 传错字段返回 204 但静默忽略（坑中之坑，PUT 成功后要 GET 验证）。
6. **push 到不存在的仓库报 Repository not found** → 先用 `POST /user/repos` 创建仓库再 push。
7. Release 资产上传：`POST https://uploads.github.com/repos/{o}/{r}/releases/{id}/assets?name=xxx`，二进制 body，`Content-Type: application/octet-stream`。
8. **.gitignore 必须覆盖**：`run/`、`**/run/`、`build/`、`**/build/`、`mods/`、`**/mods/`、`.build-tmp/`、`.gradle/`；提交前 `git status` 数文件确认无泄漏（本工程 230 个文件全对）。
9. CI 注意：ubuntu 上 `chmod +x gradlew`；版本号从 gradle.properties 提取与产物名匹配（`more-mooncake-fabric-${VER}.jar`）。
10. 发布流程标准化：`git tag vX.Y.Z && git push origin vX.Y.Z` → CI 构建 → 自动 Release（权限配好后全自动）。

## 十、流程方法论（skill 的元规则）

1. **先锁版本组合，再写代码**；生成器/文档为准，不凭记忆。
2. **一个子系统 → 两平台构建 → 自检断言 → 下一子系统**，绝不攒到最后。
3. 复杂几何/纹理用代码生成（确定性种子）+ 字符画自查，弥补模型不能看图的短板。
4. **用户报 bug 立即定位到机制**："刮蜡消耗整把斧子" → 机制根因是 shapeless 配方必然消耗全部原料 → 换自定义配方 + getRemainingItems + 自检断言修复。
5. 自检代码是第三只眼：配方键存在性、合成输出断言、工具耐久断言、模型元素数，全在服务端启动时跑。
6. 发布前先配好 CI 权限，否则 tag 触发一半失败（build 成功但 release 403）。
7. 交付时说明：common/fabric/neoforge 各改了什么、如何分别构建两个平台产物、运行验证方式。

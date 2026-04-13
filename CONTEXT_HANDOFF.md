# ApisLens 项目上下文交接文档

## 项目概述
**ApisLens** - Android 设备折旧成本追踪应用
- 技术栈: Kotlin, Jetpack Compose, Material You (Material3), Hilt, Room
- Rust 核心层: Rust (JNI) - 成本计算引擎
- 主题风格: 暗色专业风格，Material You 设计语言
- 构建系统: Gradle (Kotlin DSL) + Cargo (Rust)
- 最低SDK: 26, 目标SDK: 34

## 项目路径
`/Users/dustopus/projects/ApisLens/`

## 已完成的工作

### 1. 主题系统 (已完成 ✅)
三个核心主题文件已全部更新为 Material You 风格：

- **Color.kt** - 完整的 Material You 暗色调色板 + 自定义状态颜色 + 渐变色
- **Theme.kt** - `DarkColorScheme` + `ApisLensShapes` 圆角系统 + `ApisLensTheme`
- **Type.kt** - `ApisLensTypography` 替代原来的 `Typography`

### 2. 应用基础架构 (已完成 ✅)
- Room 数据库实体: Device, ChargeRecord, UsageRecord
- Hilt 依赖注入配置完成
- ViewModel 层: DashboardViewModel, DeviceListViewModel, DeviceDetailViewModel, AddEditDeviceViewModel, AddChargeRecordViewModel, AddUsageRecordViewModel, SettingsViewModel
- 导航系统: Screen sealed class + ApisLensNavGraph + 底部导航栏

### 3. 设备分类标签交互组件 (已完成 ✅)
- **DeviceListViewModel**: 添加 `selectedCategories` StateFlow + `toggleCategory()` + `filteredDevices`
- **DeviceListScreen**: FilterChip 横向滚动分类标签，支持多选，选中/未选中视觉区分
- **AddEditDeviceScreen**: FilterChip 分类选择器，替代原 DropdownMenu
- **AddEditDeviceViewModel**: 分类验证逻辑
- 分类列表: 手机、平板、笔记本、智能手表、耳机、充电宝、电池、游戏机、相机、音箱、其他

### 4. 购买日期选择功能 (已完成 ✅)
- **AddEditDeviceScreen**: 集成 Material3 DatePicker
- 日期格式化显示，点击弹出日期选择对话框
- 日期范围验证（不超过当前日期）
- 异常处理：日期解析失败回退

### 5. 数据仪表盘模块 (已完成 ✅)
- **ChargeRecordDao**: 新增统计查询方法
  - `getLatestChargeRecord()`: 最近充电记录
  - `getLongestUnchargedDeviceId()`: 最长时间未充电设备
  - `getTotalPurchaseCost()`: 总购买成本
- **ChargeRecordRepository**: 新增对应数据访问方法
- **DashboardViewModel**: 全面重写
  - `totalDevices`: 设备总数
  - `totalDailyCost`: 每日折旧总成本
  - `averageDailyCost`: 平均日成本
  - `totalPurchaseCost`: 总购买成本
  - `latestChargedDevice`: 最近充电设备
  - `longestUnchargedDevice`: 最长时间未充电设备
- **DashboardScreen**: 全面重设计
  - DailyCostHeroCard: 渐变背景大号成本卡片
  - StatsGrid: 4格统计网格
  - LatestChargeCard / LongestUnchargedCard: 详情卡片

### 6. 底部导航栏 + Navigation重构 (已完成 ✅)
- **Navigation.kt**: 
  - `BottomNavItem` 数据类
  - 3个底部导航项: 仪表盘(Dashboard)、设备(Devices)、设置(Settings)
  - `ApisLensNavGraph` 包含底部 NavigationBar + NavHost
  - 详情页/表单页保持顶部返回导航
- **MainActivity.kt**: 简化为直接调用 `ApisLensNavGraph`

### 7. TopAppBar统一风格 (已完成 ✅)
所有页面统一使用 `surfaceContainer` 色调 TopAppBar:
- DashboardScreen, DeviceListScreen, DeviceDetailScreen
- AddEditDeviceScreen, AddChargeRecordScreen, AddUsageRecordScreen
- SettingsScreen

### 8. DeviceDetailScreen重设计 (已完成 ✅)
- 顶部大号设备信息卡片
- 操作按钮使用 FilledTonalButton
- 充电记录使用时间线样式 (ChargeRecordTimelineItem)
- 统一的 TopAppBar 风格

### 9. SettingsScreen重设计 (已完成 ✅)
- 分组卡片布局: 数据管理 + 关于
- 图标装饰 (Surface + Icon)
- 版本信息展示

### 10. Rust核心层 (已完成 ✅)
- **rust-core/**: Rust 库项目
  - `src/models.rs`: Device, ChargeRecord, UsageRecord, DashboardStats 数据模型
  - `src/calculator.rs`: CostCalculator 成本计算引擎
    - `calculate_daily_cost()`: 每日折旧成本
    - `days_since()`: 购买天数计算
    - `total_depreciation()`: 总折旧计算
    - `needs_charge_reminder()`: 充电提醒判断
    - `calculate_battery_health()`: 电池健康度评估
    - `compute_dashboard_stats()`: 仪表盘统计计算
  - `src/jni_bridge.rs`: JNI 桥接层
    - `nativeCalculateDailyCost`, `nativeDaysSince`, `nativeTotalDepreciation`
    - `nativeNeedsChargeReminder`, `nativeGetVersion`
  - `scripts/build-android.sh`: Android 交叉编译脚本
- **RustCore.kt**: Kotlin JNI 桥接类
  - 封装所有 native 方法调用
  - 自动加载 `libapislens_core.so`

## 待完成的工作 (按优先级排序)

### 🔴 高优先级

#### 1. Rust核心层集成到Android构建
- 配置 Gradle 构建流程自动编译 Rust 代码
- 将 .so 文件输出到 `app/src/main/jniLibs/`
- 在 ViewModel 中替换 Kotlin 计算为 Rust 调用
- 添加 Rust 编译失败的 fallback 机制

#### 2. 性能优化
- 数据预加载和缓存机制
- 大数据量下的 LazyColumn 优化
- 低配置设备适配测试

### 🟡 中优先级

#### 3. 完善电池健康度图表
- 集成 MPAndroidChart 或 Vico 图表库
- 使用 Rust `calculate_battery_health()` 数据
- 交互式图表展示

#### 4. 多架构 Rust 编译
- 添加 armv7-linux-androideabi, x86_64-linux-android, i686-linux-android 目标
- 更新 build-android.sh 支持多架构

### 🔵 最后

#### 5. 构建验证 APK
- 运行 `./gradlew assembleDebug`
- 修复编译错误
- 验证 UI 效果

## 关键技术注意事项

### Material You 设计规范
- TopAppBar: 使用 `surfaceContainer` 而非 `primary` 色背景
- 卡片: 使用 `surfaceContainerLow` / `surfaceContainerHigh` 分层
- 按钮: 主要操作用 FilledButton，次要操作用 FilledTonalButton
- 浮动按钮: 使用 `primaryContainer` 色
- 文字层次: headlineSmall > titleLarge > titleMedium > bodyMedium

### Rust 核心层架构
```
Kotlin (UI/ViewModel)
    ↓ JNI 调用
RustCore.kt (桥接类)
    ↓ System.loadLibrary("apislens_core")
libapislens_core.so (Rust cdylib)
    ↓
CostCalculator (纯 Rust 计算)
```

### 数据模型
```kotlin
// Device 实体
data class Device(
    val id: Long, val name: String, val model: String,
    val category: String, val purchasePriceCents: Int,
    val purchaseDate: String, val iconUri: String?,
    val createdAt: Long, val updatedAt: Long, val note: String
)

// ChargeRecord 实体
data class ChargeRecord(
    val id: Long, val deviceId: Long, val startTime: Long,
    val endTime: Long?, val startLevel: Int, val endLevel: Int?,
    val durationMinutes: Int?, val note: String, val createdAt: Long
)

// UsageRecord 实体
data class UsageRecord(
    val id: Long, val deviceId: Long, val date: String,
    val usageMinutes: Int, val note: String,
    val createdAt: Long, val updatedAt: Long
)
```

### 导航路由
```kotlin
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object DeviceList : Screen("devices")
    object DeviceDetail : Screen("device/{deviceId}")
    object AddDevice : Screen("add_device")
    object AddChargeRecord : Screen("add_charge/{deviceId}")
    object AddUsageRecord : Screen("add_usage/{deviceId}")
    object Settings : Screen("settings")
}
```

### 底部导航
```kotlin
val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "仪表盘", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(Screen.DeviceList, "设备", Icons.Filled.Devices, Icons.Outlined.Devices),
    BottomNavItem(Screen.Settings, "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
)
```

## 当前各文件状态摘要

### UI Screens
- **DashboardScreen.kt** (~250行) - 渐变HeroCard + StatsGrid + 设备列表
- **DeviceListScreen.kt** (~180行) - FilterChip分类 + 搜索 + 设备卡片列表
- **DeviceDetailScreen.kt** (~220行) - 设备信息卡 + 时间线充电记录
- **AddEditDeviceScreen.kt** (~200行) - FilterChip分类 + DatePicker + 表单验证
- **AddChargeRecordScreen.kt** (~90行) - 电量输入 + 范围验证
- **AddUsageRecordScreen.kt** (~75行) - 使用时长输入
- **SettingsScreen.kt** (~108行) - 分组卡片 + 数据管理 + 关于

### Navigation
- **Navigation.kt** (~120行) - 底部导航 + NavHost + 路由配置
- **MainActivity.kt** (~20行) - 简化入口

### Rust Core
- **rust-core/src/lib.rs** - 模块入口
- **rust-core/src/models.rs** - 数据模型
- **rust-core/src/calculator.rs** - 成本计算引擎 (含4个单元测试)
- **rust-core/src/jni_bridge.rs** - JNI桥接
- **rust-core/scripts/build-android.sh** - 交叉编译脚本

### Kotlin JNI
- **RustCore.kt** - JNI桥接类

## 如何让新 AI 读取上下文

1. 将此文件放在项目根目录: `/Users/dustopus/projects/ApisLens/CONTEXT_HANDOFF.md`
2. 在新 AI 窗口中，输入以下提示:

```
请阅读项目上下文文件 /Users/dustopus/projects/ApisLens/CONTEXT_HANDOFF.md 和调试日志 /Users/dustopus/projects/ApisLens/DEBUG_LOG.md，然后继续完成其中列出的待完成工作。
```

3. AI 会读取该文件并了解完整上下文，从中断处继续工作。

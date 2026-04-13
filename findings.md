# ApisLens 研究发现

## 项目架构
- Kotlin + Jetpack Compose + Material3 + Hilt + Room
- minSdk=26, targetSdk=34, Java 17
- Gradle 8.5, AGP 8.6.1, Kotlin 2.0.0

## 关键发现
- Device 实体已有 CATEGORIES 预设分类列表
- DeviceListScreen 当前无分类过滤功能
- AddEditDeviceScreen 使用文本输入分类和日期
- DashboardScreen 仅有简单的每日折旧总成本卡片
- 无底部导航栏
- 所有 TopAppBar 使用 primary 色背景 (需改为 surfaceContainer)

## Rust 集成方案
- 使用 jni crate 创建 Rust 共享库
- 通过 JNI 桥接核心业务逻辑
- Kotlin 侧通过 native 方法调用 Rust 函数
- 构建流程: cargo build -> .so 文件 -> jniLibs

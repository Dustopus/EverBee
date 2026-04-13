# ApisLens 开发文档

## 项目概述

ApisLens是一款Android应用，用于追踪设备折旧成本和电池健康状态。采用Material You + 暗色专业风格设计，使用Jetpack Compose构建UI。

## 技术栈

- **UI框架**: Jetpack Compose + Material 3 (暗色主题)
- **架构**: MVVM + Clean Architecture
- **数据库**: Room
- **依赖注入**: Hilt
- **图表**: MPAndroidChart
- **最低API**: 26 (Android 8.0)
- **目标API**: 34 (Android 14)

## 项目结构

```
ApisLens/
├── app/src/main/java/com/apislens/
│   ├── data/                    # 数据层
│   │   ├── local/               # 本地数据源
│   │   │   ├── database/         # Room数据库
│   │   │   ├── entity/           # 数据实体
│   │   │   │   ├── Device.kt     # 设备实体
│   │   │   │   ├── ChargeRecord.kt # 充电记录实体
│   │   │   │   └── UsageRecord.kt # 使用记录实体
│   │   │   └── dao/              # 数据访问对象
│   │   ├── repository/           # 仓库实现
│   │   └── utils/                # 工具类
│   ├── di/                       # 依赖注入模块
│   ├── ui/                       # 表现层
│   │   ├── theme/                # Material 3主题
│   │   │   ├── Color.kt          # 颜色定义
│   │   │   ├── Theme.kt          # 主题配置
│   │   │   └── Type.kt           # 字体定义
│   │   ├── screens/              # 各页面
│   │   │   ├── DashboardScreen.kt # 仪表盘
│   │   │   ├── DeviceListScreen.kt # 设备列表
│   │   │   ├── DeviceDetailScreen.kt # 设备详情
│   │   │   ├── AddEditDeviceScreen.kt # 添加/编辑设备
│   │   │   ├── AddChargeRecordScreen.kt # 添加充电记录
│   │   │   ├── AddUsageRecordScreen.kt # 添加使用记录
│   │   │   └── SettingsScreen.kt  # 设置页面
│   │   ├── components/           # 可复用组件
│   │   │   └── ChartComponents.kt # 图表组件
│   │   ├── viewmodel/            # ViewModel
│   │   │   ├── DashboardViewModel.kt
│   │   │   ├── DeviceListViewModel.kt
│   │   │   ├── DeviceDetailViewModel.kt
│   │   │   ├── AddEditDeviceViewModel.kt
│   │   │   ├── AddChargeRecordViewModel.kt
│   │   │   ├── AddUsageRecordViewModel.kt
│   │   │   └── SettingsViewModel.kt
│   │   └── navigation/           # 导航
│   │       └── Navigation.kt     # 导航图
│   ├── MainActivity.kt           # 主Activity
│   └── ApisLensApp.kt            # Application类
└── app/src/main/res/
    └── values/
        ├── strings.xml            # 字符串资源
        ├── colors.xml             # 颜色资源
        └── themes.xml             # 主题资源
```

## 核心功能模块

### 1. 设备管理
- 添加/编辑设备（名称、型号、分类、购买日期、购买价格）
- 设备列表展示
- 设备详情查看

### 2. 充电记录管理
- 记录每次充电的起止时间和电量
- 充电历史查看

### 3. 使用记录管理
- 记录设备使用时长
- 使用历史查看

### 4. 数据可视化
- 充电历史折线图
- 电池健康趋势分析

### 5. 折旧计算
- 自动计算每日折旧成本
- 公式: 每日折旧 = 购买价格 / 已使用天数

### 6. 数据导入导出
- JSON格式导出所有数据
- 从JSON导入数据

## 构建说明

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34

### 构建步骤
1. 打开Android Studio
2. 选择 "Open an Existing Project"
3. 选择ApisLens项目目录
4. 等待Gradle同步完成
5. 点击 Build > Build Bundle(s) / APK(s) > Build APK(s)

### 签名APK构建
1. 生成签名密钥:
   ```
   keytool -genkey -v -keystore apislens.keystore -alias apislens -keyalg RSA -keysize 2048 -validity 10000
   ```
2. 在app/build.gradle.kts中配置签名
3. 执行 ./gradlew assembleRelease

## 依赖库

```kotlin
// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.7")

// Hilt
implementation("com.google.dagger:hilt-android:2.51.1")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// Charts
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

// Gson
implementation("com.google.code.gson:gson:2.10.1")
```

## 版本历史

### v1.0.0 (2024-04-12)
- 初始版本发布
- 实现设备管理功能
- 实现充电记录功能
- 实现使用记录功能
- 实现数据可视化
- 实现数据导入导出

## 许可证

MIT License

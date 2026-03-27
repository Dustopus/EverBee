<p align="center">
  <img src="app/src/main/res/raw/apislens_logo.png" alt="ApisLens Logo" width="128" height="128">
</p>

<h1 align="center">🐝 ApisLens</h1>

<p align="center"><b>个人电子设备生命周期与续航智能管理应用</b></p>

<p align="center">
  <a href="https://www.gnu.org/licenses/agpl-3.0"><img src="https://img.shields.io/badge/License-AGPL_v3-blue.svg" alt="License: AGPL v3"></a>
  <a href="https://developer.android.com/about/versions/oreo"><img src="https://img.shields.io/badge/Android-8.0%2B-brightgreen" alt="Android"></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.0-blueviolet" alt="Kotlin"></a>
</p>

> 追踪每台设备的真实成本，掌控电池健康，让每一分钱都花得明白。

---

## 📱 功能概览

### 设备与充电记录
- **设备管理** — 添加/编辑设备（名称、型号、图标、购买日期、购买价格）
- **充电事件** — 手动记录每次充电（起止时间、起始/结束电量）
- **使用时长** — 每日使用时间手动记录
- **自动折旧** — 根据购买价格与使用天数，自动计算"每日折旧成本"

### 📊 数据可视化
- **电池健康趋势图** — 基于充电记录推算电池衰减曲线
- **使用时间分布图** — 每日/每周使用时长统计
- **充电习惯分析** — 充电时间段分布、充电频率热力图

### 💰 成本看板
- **每日成本** — 每台设备的实时折旧日均花费
- **累计消费看板** — 所有设备总投入与综合日均成本
- **设备生命周期洞察** — 使用效率与成本效益分析

### 📦 数据导入/导出
- **一键导出** — 全部设备及历史记录导出为 JSON 文件
- **快速导入** — 从 JSON 文件恢复完整数据
- **基于 SAF** — 使用 Android 存储访问框架，兼容所有存储位置

---

## 🛠 技术栈

| 类别 | 技术选型 | 说明 |
|------|----------|------|
| **语言** | Kotlin | 现代 Android 开发首选 |
| **架构** | MVVM | ViewModel + LiveData/StateFlow |
| **数据库** | Room | SQLite 抽象层，类型安全 |
| **图表** | MPAndroidChart | 成熟的 Android 图表库 |
| **依赖注入** | Hilt | 基于 Dagger 的简化 DI |
| **视图绑定** | ViewBinding | 类型安全的视图引用 |
| **后台任务** | WorkManager | 可靠的延迟/周期任务 |
| **文件操作** | Storage Access Framework (SAF) | 安全的文件读写 |

---

## 📁 项目结构

```
com.apislens/
├── data/
│   ├── local/
│   │   ├── dao/           # Room Data Access Objects
│   │   ├── entity/        # Device, ChargeRecord, UsageRecord
│   │   └── AppDatabase.kt # Room 数据库定义
│   ├── model/             # UI 层数据模型
│   ├── repository/        # 数据仓库（单一数据源）
│   └── utils/             # JSON 导入导出处理器
├── domain/                # 用例层（业务逻辑）
├── presentation/
│   ├── view/              # Activity, Fragment
│   ├── viewmodel/         # 各界面对应的 ViewModel
│   └── adapter/           # RecyclerView 适配器
└── utils/                 # 扩展函数、日期格式化、成本计算
```

---

## 🧮 核心算法

### 每日折旧成本

```kotlin
val dailyCost = purchasePrice / daysSince(purchaseDate)
```

### 电池健康度（基于充电记录推算）

```kotlin
// 通过分析充电周期中的电量变化曲线
// 结合充放电效率衰减模型估算当前电池容量
val healthPercent = estimatedCurrentCapacity / originalCapacity * 100
```

---

## 🚀 构建

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更新
- JDK 17
- Android SDK 34
- 最低支持 Android 8.0 (API 26)

### 构建步骤

```bash
git clone https://github.com/Dustopus/ApisLens.git
cd ApisLens
./gradlew assembleDebug
```

---

## 📄 许可证

本项目基于 **[GNU Affero General Public License v3.0](LICENSE)** 开源。

这意味着你可以自由地使用、修改和分发本软件，但任何基于本软件的服务（包括网络服务）也必须以相同协议开源。

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

<p align="center">
  Made with 🐝 by <a href="https://github.com/Dustopus">Dustopus</a>
</p>

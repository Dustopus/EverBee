# ApisLens 重构与功能增强计划

## 目标
1. 设备分类标签交互组件
2. 购买日期选择器 (DatePickerDialog)
3. 主界面仪表盘全面优化
4. Rust核心层搭建 + JNI桥接
5. 持续维护 CONTEXT_HANDOFF.md 和 DEBUG_LOG.md

---

## Phase 1: 设备分类标签交互组件 ⏳
- [ ] DeviceListScreen: 添加横向滚动 FilterChip 分类过滤栏
- [ ] AddEditDeviceScreen: 替换文本输入为 FilterChip 分类选择
- [ ] 支持多标签同时选中过滤
- [ ] 选中/未选中状态视觉反馈 (FilledTonal vs Outlined)
- [ ] DeviceListViewModel: 添加分类过滤逻辑

## Phase 2: 购买日期选择器 ⏳
- [ ] AddEditDeviceScreen: 替换文本输入为 Material3 DatePicker
- [ ] 日期范围验证 (不能选未来日期)
- [ ] 异常处理 (接口调用失败等)
- [ ] AddEditDeviceViewModel: 更新日期状态管理

## Phase 3: 主界面仪表盘优化 ⏳
- [ ] DashboardScreen: 数据仪表盘模块
  - [ ] 设备总数量统计
  - [ ] 最近充电设备信息
  - [ ] 所有设备总成本
  - [ ] 最长时间未充电设备
- [ ] DashboardViewModel: 添加统计数据流
- [ ] 分块式布局设计 (仪表盘区/设备列表区)
- [ ] 底部导航栏 (仪表盘/设备/设置)
- [ ] Navigation + MainActivity: 集成底部导航
- [ ] TopAppBar: 统一改为 surfaceContainer 色调
- [ ] 性能优化 (数据预加载/缓存)

## Phase 4: Rust 核心层搭建 ⏳
- [ ] 创建 Rust 库项目 (cargo-lib)
- [ ] 定义 Rust 数据模型 (Device, ChargeRecord, UsageRecord)
- [ ] 迁移 CostCalculator 到 Rust
- [ ] JNI 桥接层 (jni crate)
- [ ] Android 集成 (build.gradle 配置)
- [ ] Kotlin 侧调用 Rust 核心

## Phase 5: 文档更新 ⏳
- [ ] 更新 CONTEXT_HANDOFF.md
- [ ] 更新 DEBUG_LOG.md

---

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|

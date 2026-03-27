# ApisLens Codebase Analysis

## ✅ Already Implemented Features

### Data Layer (Complete)
- Device entity with full CRUD (name, model, icon, category, purchaseDate, price, note)
- ChargeRecord entity (deviceId, startTime, endTime, startLevel, endLevel, durationMinutes)
- UsageRecord entity (deviceId, date, usageMinutes) with unique constraint per device+date
- Room database v1→v2 migration (added category)
- Full DAOs with Flow-based queries
- Repositories for all three entities

### Presentation Layer (Complete)
- DeviceListFragment — RecyclerView with SwipeRefreshLayout, empty state
- DeviceDetailFragment — info card, 3 charts (battery/usage/charge), quick actions
- AddEditDeviceFragment — photo picker, category dropdown, date picker
- AddChargeRecordFragment — datetime pickers, level sliders
- AddUsageRecordFragment — date picker, hours/minutes inputs
- DashboardFragment — total investment, daily cost, device cost list
- SettingsFragment — export/import via SAF

### Infrastructure (Complete)
- Hilt DI (DatabaseModule, all ViewModels)
- Navigation with SafeArgs
- ExportImportManager (JSON with smart dedup on import)
- ChargeReminderWorker (WorkManager, 7-day threshold)
- CostCalculator (daily cost, total depreciation)
- MPAndroidChart integration (3 chart types)
- Material Design 3 with day/night themes

## ⚠️ Issues Found & Missing Features

### Bugs/Issues
1. UsageRecord.usageHours = usageMinutes / 60.0 → integer division issue (Int / Double should work, but verify)
2. Battery health chart plots endLevel — not a real "health" metric
3. Device detail page has no way to view charge/usage record history lists
4. Device photo in AddEditDevice: Glide loaded but not persisted on config change properly
5. Tab layout: chartBattery shown by default, but tab text shows "电池健康" without visual alignment

### Missing Features
1. Device grouping/filtering by category in device list
2. Preset device icon selector (only photo picking exists)
3. Charge/Usage record history list on detail page
4. Weekly/monthly cost trend on dashboard
5. Edit/delete individual charge & usage records
6. ProGuard rules for Glide/MPAndroidChart/Gson/Hilt
7. App theme FabShape style missing in some layouts

### Enhancements
1. Add device grouping with category filter chips
2. Add preset icon picker
3. Add record history view
4. Add proguard-rules.pro
5. Improve battery health calculation
6. Add cost per use metric

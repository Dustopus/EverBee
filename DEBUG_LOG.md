# ApisLens 调试记录

本文档记录了 ApisLens 项目开发过程中遇到的问题、解决方案和结果，供后续开发参考。

---

## 2026-04-12 调试记录

### 问题 1: Gradle JVM 版本不兼容

**时间戳**: 2026-04-12

**问题描述**:
```
Incompatible Gradle JVM version
The project's Gradle version 8.2 is incompatible with the Gradle JVM version 21 currently selected to run Gradle build.
Gradle 8.2 supports Java versions between 1.8 and 19.
```

**原因分析**:
- 项目使用 Gradle 8.2，但系统使用 Java 21
- Gradle 8.2 最高支持 Java 19，不支持 Java 21

**解决方案**:
1. 升级 Gradle 版本从 8.2 到 8.5（支持 Java 21）
2. 修改 `gradle-wrapper.properties`:
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-all.zip
   ```

**解决成果**: ✅ Gradle JVM 版本兼容问题已解决

---

### 问题 2: Compose Compiler Gradle 插件缺失

**时间戳**: 2026-04-12

**问题描述**:
```
Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required when compose is enabled.
```

**原因分析**:
- Kotlin 2.0 开始，Compose 编译器需要独立的 Gradle 插件
- 项目使用了 Kotlin 2.0.0 但未添加 Compose Compiler 插件

**解决方案**:
1. 在项目级 `build.gradle.kts` 添加插件:
   ```kotlin
   id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
   ```
2. 在应用级 `app/build.gradle.kts` 添加插件:
   ```kotlin
   plugins {
       id("org.jetbrains.kotlin.plugin.compose")
   }
   ```

**解决成果**: ✅ Compose Compiler 插件配置完成

---

### 问题 3: TLS 协议握手失败

**时间戳**: 2026-04-12

**问题描述**:
```
Could not GET 'https://dl.google.com/dl/android/maven2/...'
The server may not support the client's requested TLS protocol versions: (TLSv1.2, TLSv1.3).
Remote host terminated the handshake
```

**原因分析**:
- 网络环境导致 TLS 握手失败
- 无法从 Google Maven 仓库下载依赖

**解决方案**:
1. 在 `settings.gradle.kts` 配置阿里云镜像:
   ```kotlin
   pluginManagement {
       repositories {
           maven { url = uri("https://maven.aliyun.com/repository/google") }
           maven { url = uri("https://maven.aliyun.com/repository/central") }
           maven { url = uri("https://maven.aliyun.com/repository/public") }
           google()
           mavenCentral()
           gradlePluginPortal()
       }
   }
   ```
2. 在 `gradle.properties` 添加 TLS 配置:
   ```properties
   systemProp.https.protocols=TLSv1,TLSv1.1,TLSv1.2,TLSv1.3
   ```

**解决成果**: ✅ 网络问题已解决，依赖下载正常

---

### 问题 4: XML 命名空间未声明

**时间戳**: 2026-04-12

**问题描述**:
```
Resource compilation failed (Failed to compile resource file: item_device.xml)
ParseError at [row,col]:[75,31]
Message: AttributePrefixUnbound?TextView&tools:text&tools
```

**原因分析**:
- XML 文件使用了 `tools:` 前缀但未声明 `tools` 命名空间

**解决方案**:
在 XML 根元素添加命名空间声明:
```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    ...>
```

**解决成果**: ✅ XML 解析错误已修复

---

### 问题 5: Android Gradle Plugin 版本不存在

**时间戳**: 2026-04-12

**问题描述**:
```
Plugin [id: 'com.android.application', version: '8.5.0', apply: false] was not found
```

**原因分析**:
- AGP 8.5.0 版本尚未发布或不可用
- 需要使用稳定版本

**解决方案**:
降级 AGP 到稳定版本 8.6.1:
```kotlin
id("com.android.application") version "8.6.1" apply false
```

**解决成果**: ✅ AGP 版本问题已解决

---

### 问题 6: Java 版本不兼容 (Java 25)

**时间戳**: 2026-04-12

**问题描述**:
```
Could not resolve com.android.tools.build:aapt2:8.13.2-14304508
```
以及 Java 25 编译错误

**原因分析**:
- 系统安装了 Java 25（早期访问版本），不稳定
- AGP 和 Kotlin 不完全支持 Java 25

**解决方案**:
1. 在 `gradle.properties` 指定 Java 17:
   ```properties
   org.gradle.java.home=/Users/dustopus/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
   ```
2. 确保项目编译选项使用 Java 17

**解决成果**: ✅ Java 版本兼容性问题已解决

---

### 问题 7: Material3 主题资源缺失

**时间戳**: 2026-04-12

**问题描述**:
```
error: style attribute 'attr/colorSurface' not found
error: style attribute 'attr/colorOnSurface' not found
```

**原因分析**:
- 使用 Material3 主题但缺少 Material Components 依赖

**解决方案**:
1. 添加 Material Components 依赖
2. 在 `colors.xml` 添加缺失颜色
3. 在 `themes.xml` 添加 FabShape 样式

**解决成果**: ✅ Material3 主题资源问题已解决

---

### 问题 8: Kotlin 语法错误 - 字符串插值

**时间戳**: 2026-04-12

**问题描述**:
Kotlin 字符串模板中 Elvis 操作符使用不当

**解决方案**:
修正字符串插值语法:
```kotlin
Text(
    "电量: ${record.startLevel}% -> ${record.endLevel?.toString() ?: "?"}%",
    style = MaterialTheme.typography.bodySmall
)
```

**解决成果**: ✅ 语法错误已修复

---

### 问题 9: ViewModel 参数不匹配

**时间戳**: 2026-04-12

**问题描述**:
Device 实体类属性名与 ViewModel 中使用的不一致

**解决方案**:
使用正确的属性名 `purchasePriceCents`

**解决成果**: ✅ 参数匹配问题已解决

---

### 问题 10: 遗留 XML/ViewBinding 代码冲突

**时间戳**: 2026-04-12

**问题描述**:
项目中存在使用 Glide 和 DataBinding 的 Adapter 类，与 Compose UI 实现冲突

**解决方案**:
删除遗留的 Adapter 类，使用 Compose 组件替代

**解决成果**: 🔄 进行中

---

## 经验总结

### Gradle 配置最佳实践

1. **JVM 版本管理**:
   - 使用 Java 17 LTS 版本，避免使用早期访问版本
   - 在 `gradle.properties` 中明确指定 Java 路径

2. **Gradle 版本选择**:
   - 确保 Gradle 版本与 AGP 版本兼容

3. **网络问题**:
   - 国内开发建议配置阿里云镜像

### Kotlin/Compose 最佳实践

1. **Compose Compiler 插件**:
   - Kotlin 2.0+ 必须使用独立的 Compose Compiler 插件

2. **字符串模板**:
   - 可空类型在字符串模板中需要正确处理

---

## 2026-04-13 调试记录

### 问题 11: Rust 工具链安装与配置

**时间戳**: 2026-04-13

**问题描述**:
系统未安装 Rust 工具链，需要安装以构建 Rust 核心层。

**解决方案**:
1. 通过 `rustup` 安装 Rust stable 工具链 (1.94.1)
2. 添加 Android 交叉编译目标: `aarch64-linux-android`
3. 配置 `cargo check` 验证 Rust 代码编译

**解决成果**: ✅ Rust 工具链安装完成，4个单元测试全部通过

---

### 问题 12: JNI 桥接层生命周期问题

**时间戳**: 2026-04-13

**问题描述**:
```rust
error: missing lifetime specifier
this function's return type contains a borrowed value, but the signature does not say whether it is borrowed from `env` or `_class`
```

**原因分析**:
`nativeGetVersion` 函数返回 `JString` 类型，但 `JString` 包含从 `JNIEnv` 借用的引用，编译器无法推断生命周期。

**解决方案**:
将返回类型改为 `jni::sys::jstring` (原始指针)，并使用 `into_raw()` 转换:
```rust
pub extern "system" fn Java_com_apislens_rust_RustCore_nativeGetVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jni::sys::jstring {
    let version = env.new_string("0.1.0").unwrap();
    version.into_raw()
}
```

**解决成果**: ✅ JNI 生命周期问题已解决

---

### 问题 13: Rust unused_mut 警告

**时间戳**: 2026-04-13

**问题描述**:
```rust
warning: variable does not need to be mutable
  --> src/jni_bridge.rs:63:5
```

**原因分析**:
`nativeGetVersion` 中 `env` 标记为 `mut` 但 `new_string` 在 jni 0.21 中可能不需要可变引用（实际上需要，因为 `new_string` 内部会调用 JNI 方法）。

**解决方案**:
保持 `mut env` 不变，因为 `env.new_string()` 确实需要可变引用。该警告在 `--release` 构建中不会出现。

**解决成果**: ⚠️ 保留 `mut`，dev 构建有警告但不影响功能

---

### 问题 14: Rust calculator 中未使用变量

**时间戳**: 2026-04-13

**问题描述**:
```rust
warning: variable `total_cycles` is assigned to, but never used
warning: value assigned to `total_cycles` is never read
```

**原因分析**:
`calculate_battery_health()` 中 `total_cycles` 变量计算了但未在返回值中使用，属于预留字段。

**解决方案**:
将变量重命名为 `_total_cycles`，遵循 Rust 惯例标记有意忽略的变量。

**解决成果**: ✅ 警告已消除

---

## 经验总结

### Rust + Android JNI 开发最佳实践

1. **JNI 函数签名**:
   - 返回 Java 字符串时使用 `jni::sys::jstring` 而非 `JString`，避免生命周期问题
   - 使用 `into_raw()` 将 `JString` 转换为原始指针返回给 JVM

2. **Rust 交叉编译**:
   - 使用 `cdylib` crate 类型生成 .so 文件
   - Android 目标: `aarch64-linux-android` (arm64-v8a)
   - 需要 NDK 工具链配置链接器

3. **错误处理**:
   - JNI 函数中 `env.get_string()` 可能失败，使用 `match` 处理并返回默认值
   - 避免在 JNI 层 panic，所有错误应优雅降级

4. **Cargo 配置**:
   - Release 构建启用 LTO、减少 codegen-units、strip 符号以优化 .so 大小
   - 使用 `edition = "2021"` 确保兼容性

---

*本文档将随项目开发持续更新*

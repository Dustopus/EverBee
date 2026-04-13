#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
RUST_CORE_DIR="$PROJECT_DIR/rust-core"

NDK_PATH="${ANDROID_NDK_HOME:-$HOME/Library/Android/sdk/ndk/26.1.10909125}"

if [ ! -d "$NDK_PATH" ]; then
    echo "Error: Android NDK not found at $NDK_PATH"
    echo "Set ANDROID_NDK_HOME environment variable"
    exit 1
fi

source "$HOME/.cargo/env"

TOOLCHAIN="$NDK_PATH/toolchains/llvm/prebuilt/darwin-x86_64"
API_LEVEL=26

TARGETS=(
    "aarch64-linux-android:arm64-v8a"
    "armv7-linux-androideabi:armeabi-v7a"
    "x86_64-linux-android:x86_64"
    "i686-linux-android:x86"
)

echo "=== Building ApisLens Rust Core for Android ==="

for ENTRY in "${TARGETS[@]}"; do
    IFS=':' read -r TARGET ARCH <<< "$ENTRY"

    echo ""
    echo "--- Building for $TARGET ($ARCH) ---"

    rustup target add "$TARGET" 2>/dev/null || true

    export CC_"${TARGET//-/_}"="$TOOLCHAIN/bin/$TARGET$API_LEVEL-clang"
    export AR_"${TARGET//-/_}"="$TOOLCHAIN/bin/llvm-ar"
    export CARGO_TARGET_"${TARGET//-/_}_LINKER"="$TOOLCHAIN/bin/$TARGET$API_LEVEL-clang"

    if cargo build --target "$TARGET" --release --manifest-path "$RUST_CORE_DIR/Cargo.toml"; then
        JNI_LIBS_DIR="$PROJECT_DIR/app/src/main/jniLibs/$ARCH"
        mkdir -p "$JNI_LIBS_DIR"
        cp "$RUST_CORE_DIR/target/$TARGET/release/libapislens_core.so" "$JNI_LIBS_DIR/"
        echo "✅ Built successfully for $ARCH"
    else
        echo "❌ Build failed for $ARCH (Kotlin fallback will be used)"
    fi
done

echo ""
echo "=== Build Summary ==="
for ENTRY in "${TARGETS[@]}"; do
    IFS=':' read -r TARGET ARCH <<< "$ENTRY"
    SO_FILE="$PROJECT_DIR/app/src/main/jniLibs/$ARCH/libapislens_core.so"
    if [ -f "$SO_FILE" ]; then
        SIZE=$(du -h "$SO_FILE" | cut -f1)
        echo "  $ARCH: ✅ $SIZE"
    else
        echo "  $ARCH: ❌ missing"
    fi
done

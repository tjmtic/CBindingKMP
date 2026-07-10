# Linking Prebuilt Native Libraries on iOS

Kotlin/Native links C code very differently from Android: there is no CMake step — the
cinterop tool binds against *headers*, and the actual machine code must be handed to the
linker as a prebuilt archive. This page documents both patterns.

## Pattern 1: your own C sources → per-target static archive

Compile the C once per Kotlin/Native target and let the cinterop klib carry the archive
(`staticLibraries`), so every consumer of the klib links it automatically:

```kotlin
// build.gradle.kts (see shared/build.gradle.kts for the working example)
val sdk = if (target.name == "iosArm64") "iphoneos" else "iphonesimulator"
val triple = if (target.name == "iosArm64") "arm64-apple-ios12.0"
             else "arm64-apple-ios12.0-simulator"

tasks.register<Exec>("compileMylib${target.name.replaceFirstChar { it.uppercase() }}") {
    commandLine("bash", "-c",
        "xcrun --sdk $sdk clang -target $triple -O2 -c mylib.c -Iinclude -o out/mylib.o && " +
        "ar rcs out/libmylib.a out/mylib.o")
}
```

```
# mylib.def
headers = mylib.h
staticLibraries = libmylib.a
# libraryPaths differs per target → supply via cinterop extraOpts:
```

```kotlin
val mylib by cinterops.creating {
    defFile(file("src/nativeInterop/cinterop/mylib.def"))
    includeDirs(file("../native/c"))
    extraOpts("-libraryPath", libDir.absolutePath) // per-target directory
}
tasks.named("cinteropMylib<Target>") { dependsOn(compileTask) }
```

Key points:
- The `-target` triple must include the platform *and* environment (`-simulator` suffix
  for simulator slices) and should match Kotlin/Native's minimum iOS (12.0) to avoid
  linker version warnings.
- `staticLibraries` embeds the archive into the klib: consumers need no extra flags.

## Pattern 2: third-party xcframework (e.g. an inference runtime)

A vendor `.xcframework` contains one framework per slice
(`ios-arm64`, `ios-arm64_x86_64-simulator`). For a **static** framework (check with
`file Framework.framework/Framework` — "Mach-O object" = static):

1. cinterop binds against the framework headers only:
   ```
   headers = c_api.h
   ```
   with `includeDirs(".../ios-arm64/TheLib.framework/Headers")` (any slice's headers).
2. **Symbols resolve at the FINAL binary link**, not at klib/framework build. If your
   shared module produces a *static* framework, nothing links until the app does. Add
   the flags at every final binary:
   - Kotlin/Native test binaries:
     ```kotlin
     binaries.getTest("DEBUG").linkerOpts(
         "-F/path/to/TheLib.xcframework/ios-arm64_x86_64-simulator",
         "-framework", "TheLib", "-lc++")
     ```
   - The Xcode app target: `FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]` → device slice,
     `FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]` → simulator slice, plus
     `OTHER_LDFLAGS = -framework TheLib` (and `-lc++` if the vendor requires it).
3. Static frameworks need **no embed phase**; dynamic ones must be embedded and get
   `LD_RUNPATH_SEARCH_PATHS = @executable_path/Frameworks`.
4. Duplicate-symbol hazard: never also add the same library via CocoaPods/SPM in the
   consuming app.

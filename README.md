[![](https://jitpack.io/v/XuQK/NavigationProcessor.svg)](https://jitpack.io/#XuQK/NavigationProcessor)

本库是给 Navigation 添加 APP 内部 DeepLink 路由的注解工具，支持`kapt`和`ksp`。

# 实现原理

只需要在目的地 Fragment 上添加注解`@NavigationDeepLink(path = "/CommonFragment", graphLabel = "GraphA")`，然后`rebuild`，会通过注解库自动生成相关代码，无需在`navigation xml`文件中手写 deepLink。
将默认的`NavHostFragment`中的`NavController`对象替换成`DeepLinkNavController`，以实现 Graph 生成后插入对应的 DeepLink。

# 使用方式：

注意：项目中不能使用官方的`navigation-fragment-ktx`库。

## 引入依赖

### 通用设置

根目录下的`build.gradle`文件中：

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

`settings.gradle`文件，如果上述配置不生效，需要在这个文件顶部添加：

```groovy
dependencyResolutionManagement {
    ...
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

### 如果使用 kapt

使用本库的模块的`build.gradle`文件：

```groovy
plugins {
    ...
    id 'kotlin-kapt'
}

android {
    ...

    defaultConfig {
        ...

        kapt {
            arguments {
                arg("projectDirPath", projectDir.path) // 此处为固定写法
                arg("deepLinkHost", "nav://github.xuqk") // 此处为 deepLink 的 scheme 和 host
            }
        }
    }

    ...
}

dependencies {
    // navigation 依赖
    implementation ("androidx.navigation:navigation-fragment-ktx:2.3.5")
    
    // 本库依赖
    implementation "com.github.XuQK.NavigationProcessor:kd-nav-fragment-ktx:versionCode"
    // 使用kapt
    kapt "com.github.XuQK.NavigationProcessor:kd-nav-processor:versionCode"
}

// 替换官方的 navigation-fragment 包
configurations {
    all*.exclude group: 'androidx.navigation', module: 'navigation-fragment'
}
```

### 如果尝鲜 ksp

已知问题：
1. 如果进行了需要生成新的kt文件的操作，可能需要 run 两次才能正常识别。
2. 由于需要手动将ksp生成的代码目录加入每个buildType的sourceSets，所以编译的时候会有 Redeclaration 报错，请自行根据自己的需求解决。

根目录下的`build.gradle`文件中：

```groovy
buildscript {
    ...
}

plugins {
    id "com.google.devtools.ksp" version "1.5.30-1.0.0"
}
```

使用本库的模块的`build.gradle`文件：

```groovy
plugins {
    ...
    id 'com.google.devtools.ksp'
}

android {
    ...

    defaultConfig {
        ...

        ksp {
            arg("projectDirPath", projectDir.path) // 此处为固定写法
            arg("deepLinkHost", "nav://github.xuqk") // 此处为 deepLink 的 scheme 和 host
        }
    }

    // 因为 ksp 生成的代码所在目录`build/generated/ksp/*/kotlin`不在源文件目录里
    // 所以如果要 IDE 识别出来生成的代码，需要手动将其加入源文件中
    buildTypes {
        debug {
            ...
            sourceSets {
                main {
                    java {
                        srcDir 'build/generated/ksp/debug/kotlin'
                    }
                }
            }
        }
        release {
            ...
            sourceSets {
                main {
                    java {
                        srcDir 'build/generated/ksp/release/kotlin'
                    }
                }
            }
        }
    }

    ...
}

dependencies {
    // navigation 依赖
    implementation ("androidx.navigation:navigation-fragment-ktx:2.3.5")

    // 本库依赖
    implementation "com.github.XuQK.NavigationProcessor:kd-nav-fragment-ktx:versionCode"
    // 使用kapt
    ksp "com.github.XuQK.NavigationProcessor:kd-nav-processor:versionCode"
}

// 替换官方的 navigation-fragment 包
configurations {
    all*.exclude group: 'androidx.navigation', module: 'navigation-fragment'
}
```

## 使用方式

Fragment 中使用：

```kotlin
package github.xuqk.navigationprocessor

import ...
        
@NavigationDeepLink(path = "/CommonFragment", graphLabel = "GraphA")
class CommonFragment : BaseFragment() {
    ...
}
```

rebuild 后，会在同包名路径下生成 kotlin 文件，生成获取 DeepLink Uri 的方法：

```kotlin
package github.xuqk.navigationprocessor

import ...

public class CommonFragmentDeepLinkFactory() {
    public companion object {
        public fun deepLinkInGraphA(): Uri {
            val uriBuilder = Uri.parse("nav://github.xuqk").buildUpon()
                .appendPath("GraphA")
                .appendPath("CommonFragment")
            return uriBuilder.build()
        }
    }
}
```

同时，会生成`androidx.navigation.fragment.kd.DeepLinkList`文件，里面保存有 deepLink 的信息，用于在 Graph 构建后，插入 DeepLink 信息。

# TODO

- [x] 增量编译支持
- [x] ksp 支持

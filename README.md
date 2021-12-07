[![](https://jitpack.io/v/XuQK/NavigationProcessor.svg)](https://jitpack.io/#XuQK/NavigationProcessor)

本库是给 Navigation 添加 APP 内部 DeepLink 路由的注解工具。

# 实现原理

只需要在目的地 Fragment 上添加注解`@NavigationDeepLink(path = "/CommonFragment", graphLabel = "GraphA")`，然后`rebuild`，会通过注解库自动生成相关代码，无需在`navigation xml`文件中手写 deepLink。
将默认的`NavHostFragment`中的`NavController`对象替换成`DeepLinkNavController`，以实现 Graph 生成后插入对应的 DeepLink。

# 使用方式：

注意：项目中不能使用官方的`navigation-fragment-ktx`库。

## 引入依赖

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

项目依赖：

```groovy
dependencies {
    // navigation 依赖
    implementation ("androidx.navigation:navigation-fragment-ktx:2.3.5")
    
    // 本库依赖
    implementation "com.github.XuQK.NavigationProcessor:kd-nav-fragment-ktx:versionCode"
    kapt "com.github.XuQK.NavigationProcessor:kd-nav-processor:versionCode"
}

// 替换官方的 navigation-fragment 包
configurations {
    all*.exclude group: 'androidx.navigation', module: 'navigation-fragment'
}
```

## 使用设置

首先在主工程的`build.gradle`中指定工程路径和`deepLink`的`host`：

```groovy
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

- [ ] 增量编译支持
- [ ] ksp 支持

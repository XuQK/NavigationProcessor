package github.xuqk.kd_nav_processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.dom4j.io.SAXReader
import java.io.File
import javax.lang.model.element.Element

/**
 * Created By：XuQK
 * Created Date：2021/12/7 14:48
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

/**
 * 检测生成的 deepLinkInfo 集合里，是否存在同名 path。
 */
internal fun checkDeepLinkDuplicate(deepLinkInfoList: List<DeepLinkInfoEntity>) {
    val duplicatePathList = mutableListOf<String>()
    deepLinkInfoList.groupBy { it.className }
        .filter { it.value.size > 1 || it.key.isNotEmpty() }
        .forEach { entry ->
            duplicatePathList.addAll(
                entry.value.groupBy { it.deepLinkUrl.split("?")[0].lowercase() }
                    .filter { it.value.size > 1 }
                    .map { it.key }
            )
        }
    require(duplicatePathList.isEmpty()) {
        "存在重复的 deepLink path：${duplicatePathList.joinToString()}"
    }
}

/**
 * 解析 navigation xml 文件，主要是填充每个节点的标识和所需参数
 */
internal fun resolveNavigationFile(projectDirPath: String): List<DeepLinkInfoEntity> {
    val navigationDir =
        File("${projectDirPath}${File.separatorChar}src${File.separatorChar}main${File.separatorChar}res${File.separatorChar}navigation${File.separatorChar}")

    val list = mutableListOf<DeepLinkInfoEntity>()
    val reader = SAXReader()

    navigationDir.listFiles()?.forEach { file ->
        val document = reader.read(file)
        val label = document.rootElement.attributeValue("label")
        val startDestinationIdString =
            document.rootElement.attributeValue("startDestination").drop(4)

        document.rootElement.elements("fragment").forEach { element ->
            val idString = element.attributeValue("id").drop(5)
            val argElements = element.elements("argument")
            val deepLinkBuilder = StringBuilder()

            val deepLinkArgsList = mutableListOf<DeepLinkInfoEntity.DeepLinkArgsEntity>()
            argElements.forEachIndexed { index, argElement ->
                val argName = argElement.attributeValue("name")
                val argType = argElement.attributeValue("argType").lowercase()
                val defaultValue = argElement.attributeValue("defaultValue")
                val nullable = argElement.attributeValue("nullable")?.toBoolean() == true

                deepLinkArgsList.add(
                    DeepLinkInfoEntity.DeepLinkArgsEntity(
                        argName,
                        argType,
                        defaultValue,
                        nullable
                    )
                )

                if (index == 0) {
                    deepLinkBuilder.append("?")
                }
                deepLinkBuilder.append(argName)
                    .append("=")
                    .append("{")
                    .append(argName)
                    .append("}")
                if (index < argElements.size - 1) {
                    deepLinkBuilder.append("&")
                }
            }

            val isStartDest = startDestinationIdString == idString
            list.add(
                DeepLinkInfoEntity(
                    label,
                    isStartDest,
                    element.attributeValue("name"),
                    "",
                    deepLinkBuilder.toString(),
                    "",
                    "",
                    deepLinkArgsList,
                )
            )
        }
    }
    return list
}

/**
 * 生成每个 Fragment 需要的 DeepLink 生成类，免去手写拼接 DeepLink 的步骤
 */
internal fun generateDeepLinkFactoryFileSpec(
    deepLinkHost: String,
    map: Map.Entry<String, List<DeepLinkInfoEntity>>
): FileSpec {
    val pkgName = map.value[0].packageName
    val clsName = map.value[0].classSimpleName + "DeepLinkFactory"

    val classBuilder = TypeSpec.classBuilder(clsName)
        .primaryConstructor(
            FunSpec.constructorBuilder().build()
        )

    val companion = TypeSpec.companionObjectBuilder()

    map.value.forEach { linkInfo ->
        linkInfo.element?.let {
            classBuilder.addOriginatingElement(it)
        }

        val funName = if (linkInfo.graphStartDest) {
            "deepLinkIn" + linkInfo.graphLabel + "AsStartDest"
        } else {
            "deepLinkIn" + linkInfo.graphLabel
        }
        val funSpecBuilder = FunSpec.builder(funName)
            .returns(ClassName.bestGuess("android.net.Uri"))
            .addStatement("""val uriBuilder = Uri.parse("$deepLinkHost").buildUpon()""")
            .addStatement(""".appendEncodedPath("${linkInfo.graphLabel}")""")
        if (!linkInfo.graphStartDest) {
            funSpecBuilder.addStatement(""".appendEncodedPath("${linkInfo.path.dropWhile { it == '/' }}")""")
        }

        linkInfo.deepLinkArgsEntities.forEach { args ->
            val typeName = when (args.argType) {
                "integer" -> Int::class.asTypeName()
                "string" -> if (args.nullable == true) String::class.asTypeName()
                    .copy(nullable = true) else String::class.asTypeName()
                "float" -> Float::class.asTypeName()
                "long" -> Long::class.asTypeName()
                "boolean" -> Boolean::class.asTypeName()
                else -> throw IllegalArgumentException("DeepLink 仅支持传递基本类型和 String 类型参数：${map.key}")
            }

            val parameterSpec = ParameterSpec.builder(args.name, typeName)
            if (args.defaultValue != null) {
                when (args.defaultValue) {
                    // 这是表示null的字符串
                    "null" -> {
                        parameterSpec.defaultValue("\"null\"")
                    }
                    // 这才是真正的null
                    "@null" -> {
                        parameterSpec.defaultValue("null")
                    }
                    // 空字符串
                    "" -> {
                        parameterSpec.defaultValue("\"\"")
                    }
                    else -> {
                        parameterSpec.defaultValue("${args.defaultValue}")
                    }
                }
            }
            funSpecBuilder.addParameter(parameterSpec.build())

            if (args.argType.equals("string", true)) {
                funSpecBuilder.addStatement(
                    """
                if (!${args.name}.isNullOrEmpty()) {
                  uriBuilder.appendQueryParameter("${args.name}", ${args.name}.toString())
                }
                """.trimIndent()
                )
            } else {
                funSpecBuilder.addStatement("uriBuilder.appendQueryParameter(\"${args.name}\", ${args.name}.toString())")
            }
        }

        funSpecBuilder.addStatement("return uriBuilder.build()")
        companion.addFunction(funSpecBuilder.build())
    }

    classBuilder.addType(companion.build())
    return FileSpec.builder(pkgName, clsName)
        .addType(classBuilder.build())
        .build()
}

/**
 * 生成需要 DeepLink 的 Fragment 的 DeepLink 基本信息存储类，用于插入 Graph
 */
internal fun generateDeepLinkInfoMapFileSpec(deepLinkInfoList: List<DeepLinkInfoEntity>): FileSpec {
    val classBuilder = TypeSpec.classBuilder("DeepLinkList")
        .addAnnotation(ClassName.bestGuess("androidx.annotation.Keep"))

    val codeBlock = CodeBlock.builder()
    codeBlock.addStatement("listOf(")
    deepLinkInfoList.forEach { link ->
        codeBlock.addStatement(
            "arrayOf(%S, %S, %S),",
            if (link.graphStartDest) link.graphLabel else link.className,
            link.graphLabel,
            link.deepLinkUrl
        )
    }
    codeBlock.addStatement(")")

    val valueValueTypeName =
        Array::class.asClassName().parameterizedBy(String::class.asClassName())
    val valueTypeName = List::class.asClassName().parameterizedBy(valueValueTypeName)
    val mapPropertySpecBuilder = PropertySpec.builder("list", valueTypeName)
        .initializer(codeBlock.build())
    classBuilder.addProperty(mapPropertySpecBuilder.build())

    return FileSpec.builder("androidx.navigation.fragment.kd", "DeepLinkList")
        .addType(classBuilder.build())
        .build()
}

/**
 * 根据给出的类和注解信息，获取对应的 deepLinkInfo 并补完其信息。
 */
internal fun insertDeepLinkPath(
    deepLinkHost: String,
    deepLinkInfoListWithArgument: List<DeepLinkInfoEntity>,
    pkgName: String,
    fragmentClassQualifiedName: String,
    fragmentClassSimpleName: String,
    graphLabel: String,
    path: String,
    element: Element? = null
): DeepLinkInfoEntity {
    val matchedLinkInfoList: List<DeepLinkInfoEntity> = if (graphLabel.isBlank()) {
        // 注解上 graphLabel 为空的情况，只需要匹配 fragment 类名
        deepLinkInfoListWithArgument.filter { it.className == fragmentClassQualifiedName }
    } else {
        // 注解上 graphLabel 不为空的情况，需要匹配 fragment 类名和 graphLabel
        deepLinkInfoListWithArgument.filter { it.className == fragmentClassQualifiedName && it.graphLabel == graphLabel }
    }

    require(matchedLinkInfoList.isNotEmpty()) {
        "该注解匹配不到节点：$fragmentClassQualifiedName annotationPath: $path annotationGraphLabel: $graphLabel"
    }
    require(matchedLinkInfoList.size == 1) {
        "该注解匹配到了多个节点，请显式指定 graphLabel：$fragmentClassQualifiedName"
    }
    val matchedLinkInfo = matchedLinkInfoList[0]
    require(path.isNotEmpty() || matchedLinkInfo.graphStartDest) {
        "非 Graph startDestination 的节点注解的 path 参数不能为空：$fragmentClassQualifiedName"
    }

    matchedLinkInfo.path = path
    matchedLinkInfo.deepLinkUrl = if (matchedLinkInfo.graphStartDest) {
        "$deepLinkHost/${matchedLinkInfo.graphLabel}${matchedLinkInfo.deepLinkUrl}"
    } else {
        "$deepLinkHost/${matchedLinkInfo.graphLabel}/${matchedLinkInfo.path.dropWhile { it == '/' }}${matchedLinkInfo.deepLinkUrl}"
    }
    matchedLinkInfo.classSimpleName = fragmentClassSimpleName
    matchedLinkInfo.packageName = pkgName
    matchedLinkInfo.element = element
    return matchedLinkInfo
}

/**
 * 组合从 xml 和 class 注解中读取到的 deepLink 信息，并返回
 * @return 完整的 deepLink 信息列表
 */
internal fun combineXmlAndClassDeepLinkInfo(
    deepLinkHost: String,
    deepLinkInfoListFromXml: List<DeepLinkInfoEntity>,
    deepLinkInfoListFromClass: List<DeepLinkInfoEntity>
): List<DeepLinkInfoEntity> {
    require(deepLinkInfoListFromClass.find { it.graphLabel.contains("?") && it.path.contains("?") } == null) {
        "path 或 graphLabel 中不能包含'?'字符"
    }

    deepLinkInfoListFromClass.groupBy { it.className }.forEach { t, u ->
        if (u.size > 1) {
            require(u.find { it.graphLabel.isBlank() } == null) {
                "同时存在多个注解时，graphLabel 参数都必须显式指定：$t"
            }
            require(u.groupBy { it.graphLabel }.size == u.size) {
                "同时存在多个注解时，graphLabel 不能重复：$t"
            }
        }
    }

    val deepLinkInfoListFinal = mutableListOf<DeepLinkInfoEntity>()

    deepLinkInfoListFromClass.forEach {
        deepLinkInfoListFinal.add(
            insertDeepLinkPath(
                deepLinkHost,
                deepLinkInfoListFromXml,
                it.packageName,
                it.className,
                it.classSimpleName,
                it.graphLabel,
                it.path,
            )
        )
    }

    return deepLinkInfoListFinal
}

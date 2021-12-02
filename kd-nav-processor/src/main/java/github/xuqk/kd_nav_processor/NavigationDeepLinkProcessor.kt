package github.xuqk.kd_nav_processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import github.xuqk.kd_nav_annotations.NavigationDeepLink
import org.dom4j.io.SAXReader
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

/**
 * Created By：XuQK
 * Created Date：2021/11/30 14:27
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

@SupportedAnnotationTypes(
    "github.xuqk.kd_nav_annotations.NavigationDeepLink",
    "github.xuqk.kd_nav_annotations.NavigationDeepLinks"
)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class NavigationDeepLinkProcessor : AbstractProcessor() {

    private lateinit var filer: Filer
    private var projectDirPath: String = ""
    private var deepLinkHost: String = ""

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        projectDirPath =
            requireNotNull(processingEnv.options["projectDirPath"]) { "参数未配置：projectDirPath" }
        deepLinkHost =
            requireNotNull(processingEnv.options["deepLinkHost"]) { "参数未配置：deepLinkHost" }
        filer = processingEnv.filer
    }

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        if (annotations.isEmpty()) return false
        // 从 navigation xml 中读取出所有 fragment 节点
        val deepLinkInfoListWithArgument = resolveNavigationFile()
        // 筛选出需要 DeepLink 的节点，填充 DeepLink 信息
        val deepLinkInfoList = filterAndFillDeepLinkInfo(deepLinkInfoListWithArgument, roundEnv)
        // 检查有没有重复的 DeepLink path
        checkDeepLinkDuplicate(deepLinkInfoList)
        // 生成每个节点 Fragment 的 DeepLink Uri 生成类
        deepLinkInfoList.groupBy { it.className }.forEach {
            generateDeepLinkFactory(it)
        }
        // 生成所有节点的 DeepLink 信息，用于将 DeepLink 插入 graph
        generateDeepLinkInfoMapClass(deepLinkInfoList)
        return true
    }

    /**
     * 筛选有 DeepLink 的节点，并填充 DeepLink 信息
     * @param deepLinkInfoListWithArgument 从 navigation xml 文件中读取的所有 fragment 节点
     * @return 根据 [NavigationDeepLink] 注解筛选出 [deepLinkInfoListWithArgument] 中的对应节点，并补充了 DeepLink 信息的列表
     */
    private fun filterAndFillDeepLinkInfo(
        deepLinkInfoListWithArgument: List<DeepLinkInfoEntity>,
        roundEnv: RoundEnvironment
    ): List<DeepLinkInfoEntity> {
        val deepLinkInfoList = mutableListOf<DeepLinkInfoEntity>()
        roundEnv.rootElements.forEach { element ->
            val annotationList = element.getAnnotationsByType(NavigationDeepLink::class.java)

            require(annotationList.find { it.path.contains("?") || it.graphLabel.contains("?") } == null) {
                "path 或 graphLabel 中不能包含'?'字符"
            }
            if (annotationList.size == 1) {
                val annotation = annotationList[0]
                deepLinkInfoList.add(
                    insertDeepLinkPath(deepLinkInfoListWithArgument, element, annotation)
                )
            } else {
                require(annotationList.find { it.graphLabel.isEmpty() } == null) {
                    "同时存在多个注解时，graphLabel 参数都必须显式指定：$element"
                }
                require(annotationList.groupBy { it.graphLabel.lowercase() }.size >= annotationList.size) {
                    "同时存在多个注解时，graphLabel 不能重复：$element"
                }
                annotationList.forEach {
                    deepLinkInfoList.add(
                        insertDeepLinkPath(deepLinkInfoListWithArgument, element, it)
                    )
                }
            }
        }
        return deepLinkInfoList
    }

    private fun checkDeepLinkDuplicate(deepLinkInfoList: List<DeepLinkInfoEntity>) {
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
     * 生成每个 Fragment 需要的 DeepLink 生成类，免去手写拼接 DeepLink 的步骤
     */
    private fun generateDeepLinkFactory(map: Map.Entry<String, List<DeepLinkInfoEntity>>) {
        val pkgName = map.value[0].packageName
        val clsName = map.value[0].classSimpleName + "DeepLinkFactory"

        val classBuilder = TypeSpec.classBuilder(clsName)
            .primaryConstructor(
                FunSpec.constructorBuilder().build()
            )

        val companion = TypeSpec.companionObjectBuilder()

        map.value.forEach { linkInfo ->
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
        FileSpec.builder(pkgName, clsName)
            .addType(classBuilder.build())
            .build()
            .writeTo(filer)
    }

    /**
     * 生成需要 DeepLink 的 Fragment 的 DeepLink 基本信息存储类，用于插入 Graph
     */
    private fun generateDeepLinkInfoMapClass(deepLinkInfoList: List<DeepLinkInfoEntity>) {
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

        FileSpec.builder("androidx.navigation.fragment.kd", "DeepLinkList")
            .addType(classBuilder.build())
            .build()
            .writeTo(filer)
    }

    /**
     * 在读取到的 navigation fragment 节点中，根据注解，插入 DeepLink
     */
    private fun insertDeepLinkPath(
        deepLinkInfoListWithArgument: List<DeepLinkInfoEntity>,
        element: Element,
        annotation: NavigationDeepLink
    ): DeepLinkInfoEntity {
        val matchedLinkInfoList: List<DeepLinkInfoEntity> = if (annotation.graphLabel.isBlank()) {
            // 注解上 graphLabel 为空的情况，只需要匹配 fragment 类名
            deepLinkInfoListWithArgument.filter { it.className == element.toString() }
        } else {
            // 注解上 graphLabel 不为空的情况，需要匹配 fragment 类名和 graphLabel
            deepLinkInfoListWithArgument.filter { it.className == element.toString() && it.graphLabel == annotation.graphLabel }
        }

        require(matchedLinkInfoList.isNotEmpty()) {
            "该注解匹配不到节点：$element annotationPath: ${annotation.path} annotationGraphLabel: ${annotation.graphLabel}"
        }
        require(matchedLinkInfoList.size == 1) {
            "该注解匹配到了多个节点，请显式指定 graphLabel：$element"
        }
        val matchedLinkInfo = matchedLinkInfoList[0]
        require(annotation.path.isNotEmpty() || matchedLinkInfo.graphStartDest) {
            "非 Graph startDestination 的节点注解的 path 参数不能为空：$element"
        }

        matchedLinkInfo.path = annotation.path
        matchedLinkInfo.deepLinkUrl = if (matchedLinkInfo.graphStartDest) {
            "$deepLinkHost/${matchedLinkInfo.graphLabel}${matchedLinkInfo.deepLinkUrl}"
        } else {
            "$deepLinkHost/${matchedLinkInfo.graphLabel}/${matchedLinkInfo.path.dropWhile { it == '/' }}${matchedLinkInfo.deepLinkUrl}"
        }
        matchedLinkInfo.classSimpleName = element.simpleName.toString()
        matchedLinkInfo.packageName = element.enclosingElement.toString()
        return matchedLinkInfo
    }

    /**
     * 解析 navigation xml 文件，主要是填充每个节点的标识和所需参数
     */
    private fun resolveNavigationFile(): List<DeepLinkInfoEntity> {
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
}

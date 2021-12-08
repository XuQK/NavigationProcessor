package github.xuqk.kd_nav_processor.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.FileSpec
import github.xuqk.kd_nav_annotations.NavigationDeepLink
import github.xuqk.kd_nav_processor.*
import kotlin.concurrent.thread

/**
 * Created By：XuQK
 * Created Date：2021/12/7 14:03
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class NavigationDeepLinkProcessorKsp(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val projectDirPath: String,
    private val deepLinkHost: String
) : SymbolProcessor {

    private var invoked = false
    private var codeGenerating = false

    private val annotationQualifiedName = requireNotNull(NavigationDeepLink::class.qualifiedName)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()
        val allFiles = resolver.getAllFiles()
        if (allFiles.count() == 0) return emptyList()

        val t = System.currentTimeMillis()
        // 从 navigation xml 中读取出所有 fragment 节点
        val deepLinkInfoListFromXml = resolveNavigationFile(projectDirPath)
        // 筛选出需要 DeepLink 的节点，填充 DeepLink 信息
        val deepLinkInfoList = filterAndFillDeepLinkInfo(deepLinkInfoListFromXml, resolver)
        logger.warn(deepLinkInfoList.joinToString("\n"))
        invoked = deepLinkInfoList.isNotEmpty()
        // 检查有没有重复的 DeepLink path
        checkDeepLinkDuplicate(deepLinkInfoList)

        thread {
            codeGenerating = true

            // 生成每个节点 Fragment 的 DeepLink Uri 生成类
            deepLinkInfoList.groupBy { it.className }.forEach {
                createNewFile(generateDeepLinkFactoryFileSpec(deepLinkHost, it))
            }
            // 生成所有节点的 DeepLink 信息，用于将 DeepLink 插入 graph
            createNewFile(generateDeepLinkInfoMapFileSpec(deepLinkInfoList))

            codeGenerating = false
        }

        while (codeGenerating) {
            Thread.sleep(100)
            continue
        }

        logger.warn("NavigationDeepLinkProcessorKsp: 注解处理用时 ${System.currentTimeMillis() - t} ms")
        return emptyList()
    }

    /**
     * 筛选有 DeepLink 的节点，并填充 DeepLink 信息
     * @param deepLinkInfoListFromXml 从 navigation xml 文件中读取的所有 fragment 节点
     * @return 根据 [NavigationDeepLink] 注解筛选出 [deepLinkInfoListFromXml] 中的对应节点，并补充了 DeepLink 信息的列表
     */
    private fun filterAndFillDeepLinkInfo(
        deepLinkInfoListFromXml: List<DeepLinkInfoEntity>,
        resolver: Resolver,
    ): List<DeepLinkInfoEntity> {
        val deepLinkInfoListFromClass = getDeepLinkInfoListFromFile(resolver)

        return combineXmlAndClassDeepLinkInfo(deepLinkHost, deepLinkInfoListFromXml, deepLinkInfoListFromClass)
    }

    /**
     * 获取被注解的类上的 deepLink 信息，并返回
     */
    private fun getDeepLinkInfoListFromFile(resolver: Resolver): List<DeepLinkInfoEntity> {
        val deepLinkInfoListFromClass = mutableListOf<DeepLinkInfoEntity>()

        resolver.getAllFiles().flatMap { it.declarations }.forEach { ksDeclaration ->
            ksDeclaration.annotations.forEach { ksAnnotation ->
                if (getAnnotationQualifiedName(ksAnnotation) == annotationQualifiedName) {
                    deepLinkInfoListFromClass.add(
                        DeepLinkInfoEntity(
                            graphLabel = getAnnotationArgumentByName(
                                ksAnnotation,
                                "graphLabel"
                            ).second,
                            path = getAnnotationArgumentByName(ksAnnotation, "path").second,
                            packageName = ksDeclaration.packageName.asString(),
                            className = getDeclarationQualifiedName(ksDeclaration),
                            classSimpleName = ksDeclaration.simpleName.asString(),
                        )
                    )
                }
            }
        }
        return deepLinkInfoListFromClass
    }

    private fun createNewFile(fileSpec: FileSpec) {
        codeGenerator.createNewFile(
            Dependencies(false),
            fileSpec.packageName,
            fileSpec.name
        ).bufferedWriter().use {
            fileSpec.writeTo(it)
        }
    }

    private fun getDeclarationQualifiedName(ksDeclaration: KSDeclaration): String {
        return ksDeclaration.qualifiedName?.asString().orEmpty()
    }

    private fun getAnnotationQualifiedName(annotation: KSAnnotation): String {
        return getDeclarationQualifiedName(annotation.annotationType.resolve().declaration)
    }

    private fun getAnnotationArgumentByName(
        annotation: KSAnnotation,
        name: String
    ): Pair<String, String> {
        val s = annotation.arguments.find { it.name?.asString() == name } ?: return name to ""
        return name to s.value?.toString().orEmpty()
    }
}

class NavigationDeepLinkProcessorKspProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NavigationDeepLinkProcessorKsp(
            environment.codeGenerator,
            environment.logger,
            requireNotNull(environment.options["projectDirPath"]) { "参数未配置：projectDirPath" },
            requireNotNull(environment.options["deepLinkHost"]) { "参数未配置：deepLinkHost" },
        )
    }
}

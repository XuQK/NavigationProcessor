package github.xuqk.kd_nav_processor.kapt

import com.squareup.kotlinpoet.FileSpec
import github.xuqk.kd_nav_annotations.NavigationDeepLink
import github.xuqk.kd_nav_processor.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

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
        val t = System.currentTimeMillis()

        // 从 navigation xml 中读取出所有 fragment 节点
        val deepLinkInfoListWithArgument = resolveNavigationFile(projectDirPath)
        // 筛选出需要 DeepLink 的节点，填充 DeepLink 信息
        val deepLinkInfoList = filterAndFillDeepLinkInfo(deepLinkInfoListWithArgument, roundEnv)
        // 检查有没有重复的 DeepLink path
        checkDeepLinkDuplicate(deepLinkInfoList)
        // 生成每个节点 Fragment 的 DeepLink Uri 生成类
        deepLinkInfoList.groupBy { it.className }.forEach {
            createNewFile(generateDeepLinkFactoryFileSpec(deepLinkHost, it))
        }
        // 生成所有节点的 DeepLink 信息，用于将 DeepLink 插入 graph
        createNewFile(generateDeepLinkInfoMapFileSpec(deepLinkInfoList))

        processingEnv.messager.printMessage(
            Diagnostic.Kind.NOTE,
            "[kapt] NavigationDeepLinkProcessor: 注解处理用时 ${System.currentTimeMillis() - t} ms"
        )
        return true
    }

    /**
     * 筛选有 DeepLink 的节点，并填充 DeepLink 信息
     * @param deepLinkInfoListFromXml 从 navigation xml 文件中读取的所有 fragment 节点
     * @return 根据 [NavigationDeepLink] 注解筛选出 [deepLinkInfoListFromXml] 中的对应节点，并补充了 DeepLink 信息的列表
     */
    private fun filterAndFillDeepLinkInfo(
        deepLinkInfoListFromXml: List<DeepLinkInfoEntity>,
        roundEnv: RoundEnvironment
    ): List<DeepLinkInfoEntity> {
        val deepLinkInfoListFromClass = getDeepLinkInfoListFromFile(roundEnv)

        return combineXmlAndClassDeepLinkInfo(deepLinkHost, deepLinkInfoListFromXml, deepLinkInfoListFromClass)
    }

    /**
     * 获取被注解的类上的 deepLink 信息，并返回
     */
    private fun getDeepLinkInfoListFromFile(roundEnv: RoundEnvironment): List<DeepLinkInfoEntity> {
        val deepLinkInfoListFromClass = mutableListOf<DeepLinkInfoEntity>()

        roundEnv.rootElements.forEach { element ->
            element.getAnnotationsByType(NavigationDeepLink::class.java).forEach {
                deepLinkInfoListFromClass.add(
                    DeepLinkInfoEntity(
                        graphLabel = it.graphLabel,
                        path = it.path,
                        packageName = element.enclosingElement.toString(),
                        className = element.toString(),
                        classSimpleName = element.simpleName.toString()
                    )
                )
            }
        }
        return deepLinkInfoListFromClass
    }

    private fun createNewFile(fileSpec: FileSpec) {
        fileSpec.writeTo(filer)
    }
}

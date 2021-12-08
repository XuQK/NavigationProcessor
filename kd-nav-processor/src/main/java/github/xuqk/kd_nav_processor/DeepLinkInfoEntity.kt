package github.xuqk.kd_nav_processor

import javax.lang.model.element.Element

/**
 * Created By：XuQK
 * Created Date：2021/11/21 20:59
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

data class DeepLinkInfoEntity(
    val graphLabel: String = "",
    val graphStartDest: Boolean = false,
    val className: String = "",
    var path: String = "",
    var deepLinkUrl: String = "",
    var packageName: String = "",
    var classSimpleName: String = "",
    var deepLinkArgsEntities: List<DeepLinkArgsEntity> = listOf(),
    var element: Element? = null,
) {
    data class DeepLinkArgsEntity(
        val name: String,
        val argType: String,
        val defaultValue: Any?,
        val nullable: Boolean?,
    )
}

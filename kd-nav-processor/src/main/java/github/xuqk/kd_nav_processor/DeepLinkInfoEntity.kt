package github.xuqk.kd_nav_processor

/**
 * Created By：XuQK
 * Created Date：2021/11/21 20:59
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

data class DeepLinkInfoEntity(
    val graphLabel: String,
    val graphStartDest: Boolean,
    val className: String,
    var path: String,
    var deepLinkUrl: String,
    var packageName: String,
    var classSimpleName: String,
    var deepLinkArgsEntities: List<DeepLinkArgsEntity>,
) {
    data class DeepLinkArgsEntity(
        val name: String,
        val argType: String,
        val defaultValue: Any?,
        val nullable: Boolean?,
    )
}

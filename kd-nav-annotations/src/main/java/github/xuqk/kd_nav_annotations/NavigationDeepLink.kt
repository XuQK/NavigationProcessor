package github.xuqk.kd_nav_annotations

/**
 * Created By：XuQK
 * Created Date：2021/11/30 14:26
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
@java.lang.annotation.Repeatable(NavigationDeepLinks::class)
annotation class NavigationDeepLink(
    val path: String = "",
    val graphLabel: String = ""
)

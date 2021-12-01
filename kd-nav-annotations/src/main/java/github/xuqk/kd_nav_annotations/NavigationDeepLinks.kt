package github.xuqk.kd_nav_annotations

/**
 * Created By：XuQK
 * Created Date：2021/11/30 14:26
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

@Target(AnnotationTarget.CLASS)
annotation class NavigationDeepLinks(
    val value: Array<NavigationDeepLink>
)

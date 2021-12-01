package github.xuqk.navigationprocessor

/**
 * Created By：XuQK
 * Created Date：2021/11/26 14:39
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

fun String?.toIntSafely(): Int {
    this ?: return 0
    return try {
        this.toInt()
    } catch (e: Exception) {
        0
    }
}
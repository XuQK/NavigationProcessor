package androidx.navigation.fragment.kd

import android.content.Context
import android.os.Bundle
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.fragment.FragmentNavigator
import java.lang.reflect.Method

/**
 * Created By：XuQK
 * Created Date：2021/11/30 14:26
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class DeepLinkNavController(context: Context) : NavHostController(context) {

    @Suppress("UNCHECKED_CAST")
    override fun setGraph(graph: NavGraph, startDestinationArgs: Bundle?) {
        val deepLinkMapClass = Class.forName("androidx.navigation.fragment.kd.DeepLinkList")
        val deepLinkMap = deepLinkMapClass.getDeclaredConstructor().newInstance()

        val listGetMethod: Method = deepLinkMapClass.getDeclaredMethod("getList")
        val list: List<Array<String>> = listGetMethod.invoke(deepLinkMap) as List<Array<String>>

        insertDeepLink(graph, list)
        super.setGraph(graph, startDestinationArgs)
    }

    private fun insertDeepLink(graph: NavGraph, list: List<Array<String>>) {
        graph.forEach { navDest ->
            if (navDest is NavGraph) {
                val graphList = list.filter { navDest.label == it[1] && it[0] == navDest.label }
                require(graphList.size <= 1) {
                    "检测出 Graph 的 startDestination 不止一个：graphLabel = ${navDest.label}"
                }

                graphList.firstOrNull()?.let {
                    navDest.addDeepLink(it[2])
                }
                insertDeepLink(navDest, list)
            } else {
                if (navDest is FragmentNavigator.Destination) {
                    list.filter { it[0] == navDest.className }.forEach { deepLinkInfo ->
                        if (deepLinkInfo[1] == graph.label) {
                            navDest.addDeepLink(deepLinkInfo[2])
                        }
                    }
                }
            }
        }
    }
}
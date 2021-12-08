package github.xuqk.navigationprocessor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Created By：XuQK
 * Created Date：2021/11/23 14:58
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

open class BaseFragment : Fragment() {

    private val navController by lazy { findNavController() }

    protected val propertyString: String
        get() {
            return """
                ClassName: ${this::class.simpleName}
                GraphLabel: ${navController.graph.label}
            """.trimIndent()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("生命周期：", "onCreate ---> ${this::class.simpleName}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("生命周期：", "onCreateView ---> ${this::class.simpleName}")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("生命周期：", "onDestroyView ---> ${this::class.simpleName}")
    }
}
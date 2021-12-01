package github.xuqk.navigationprocessor.graph_b

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import github.xuqk.kd_nav_annotations.NavigationDeepLink
import github.xuqk.navigationprocessor.BaseFragment
import github.xuqk.navigationprocessor.CommonFragmentDeepLinkFactory
import github.xuqk.navigationprocessor.R
import github.xuqk.navigationprocessor.databinding.FragmentGraphBNoParamBinding
import github.xuqk.navigationprocessor.toIntSafely

/**
 * Created By：XuQK
 * Created Date：2021/11/11 18:15
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

@NavigationDeepLink(path = "/GraphBNoParamFragment", graphLabel = "GraphB")
@NavigationDeepLink(path = "/GraphBNoParamFragment", graphLabel = "GraphA")
class GraphBNoParamFragment : BaseFragment() {

    companion object {
        fun startGraphB(navController: NavController) {
            navController.navigate(
                GraphBNoParamFragmentDeepLinkFactory.deepLinkInGraphBAsStartDest()
            )
        }

        fun startInGraphA(navController: NavController) {
            navController.navigate(
                GraphBNoParamFragmentDeepLinkFactory.deepLinkInGraphA()
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding = FragmentGraphBNoParamBinding.inflate(inflater)

        binding.tvInfo.text = """
            GraphLabel: ${findNavController().graph.label}
            FragmentName: ${this.toString()}
        """.trimIndent()

        Log.d("id", R.navigation.nav_a.toString() + "   " + R.navigation.nav_b)

        binding.btnOpenFragmentWithParam.setOnClickListener {
            GraphBParamFragment.start(
                findNavController(),
                binding.etInt.editableText?.toString().toIntSafely(),
                binding.etStringNullable.editableText?.toString(),
                binding.etString.editableText?.toString().orEmpty()
            )
        }

        binding.btnOpenCommon.setOnClickListener {
            findNavController().navigate(
                CommonFragmentDeepLinkFactory.deepLinkInGraphB()
            )
        }

        return binding.root
    }
}
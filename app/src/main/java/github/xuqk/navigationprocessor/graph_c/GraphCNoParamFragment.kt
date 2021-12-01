package github.xuqk.navigationprocessor.graph_c

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import github.xuqk.kd_nav_annotations.NavigationDeepLink
import github.xuqk.navigationprocessor.BaseFragment
import github.xuqk.navigationprocessor.CommonFragmentDeepLinkFactory
import github.xuqk.navigationprocessor.databinding.FragmentGraphCNoParamBinding

/**
 * Created By：XuQK
 * Created Date：2021/11/11 18:15
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

@NavigationDeepLink(path = "/GraphCNoParamFragment")
class GraphCNoParamFragment : BaseFragment() {

    companion object {
        fun start(navController: NavController) {
            navController.navigate(
                GraphCNoParamFragmentDeepLinkFactory.deepLinkInGraphC()
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding = FragmentGraphCNoParamBinding.inflate(inflater)

        binding.tvInfo.text = """
            GraphLabel: ${findNavController().graph.label}
            FragmentName: ${this.toString()}
        """.trimIndent()

        binding.btnOpenCommon.setOnClickListener {
            findNavController().navigate(
                CommonFragmentDeepLinkFactory.deepLinkInGraphC()
            )
        }

        return binding.root
    }
}
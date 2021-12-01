package github.xuqk.navigationprocessor.graph_c

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import github.xuqk.kd_nav_annotations.NavigationDeepLink
import github.xuqk.navigationprocessor.BaseFragment
import github.xuqk.navigationprocessor.CommonFragmentDeepLinkFactory
import github.xuqk.navigationprocessor.databinding.FragmentGraphCParamBinding

/**
 * Created By：XuQK
 * Created Date：2021/11/11 18:15
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

@NavigationDeepLink(path = "/GraphCParamFragment")
class GraphCParamFragment : BaseFragment() {

    companion object {
        fun startGraphC(navController: NavController, argInt: Int, argStringNullable: String?, argString: String) {
            navController.navigate(
                GraphCParamFragmentDeepLinkFactory.deepLinkInGraphCAsStartDest(argInt, argStringNullable, argString)
            )
        }
    }

    private val args by navArgs<GraphCParamFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding = FragmentGraphCParamBinding.inflate(inflater)

        binding.tvInfo.text = """
            GraphLabel: ${findNavController().graph.label}
            FragmentName: ${this.toString()}
            
            argInt: ${args.argInt}
            argStringNonNull: ${args.argStringNonNull}
            argStringNullable: ${args.argStringNullable}
        """.trimIndent()

        binding.btnOpenFragmentWithoutParam.setOnClickListener {
            GraphCNoParamFragment.start(findNavController())
        }

        binding.btnOpenCommon.setOnClickListener {
            findNavController().navigate(
                CommonFragmentDeepLinkFactory.deepLinkInGraphC()
            )
        }

        return binding.root
    }
}
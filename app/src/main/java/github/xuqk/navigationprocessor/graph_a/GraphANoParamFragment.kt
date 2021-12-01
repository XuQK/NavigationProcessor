package github.xuqk.navigationprocessor.graph_a

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import github.xuqk.navigationprocessor.BaseFragment
import github.xuqk.navigationprocessor.CommonFragmentDeepLinkFactory
import github.xuqk.navigationprocessor.R
import github.xuqk.navigationprocessor.databinding.FragmentGraphANoParamBinding
import github.xuqk.navigationprocessor.graph_b.GraphBNoParamFragment
import github.xuqk.navigationprocessor.graph_b.GraphBParamFragment
import github.xuqk.navigationprocessor.graph_c.GraphCNoParamFragment
import github.xuqk.navigationprocessor.graph_c.GraphCParamFragment
import github.xuqk.navigationprocessor.toIntSafely

/**
 * Created By：XuQK
 * Created Date：2021/11/11 18:15
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class GraphANoParamFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding = FragmentGraphANoParamBinding.inflate(inflater)

        binding.tvInfo.text = """
            GraphLabel: ${findNavController().graph.label}
            FragmentName: ${this.toString()}
        """.trimIndent()

        binding.btnOpenFragmentWithParam.setOnClickListener {
            GraphAParamFragment.start(
                findNavController(),
                binding.etInt.editableText?.toString().toIntSafely(),
                binding.etStringNullable.editableText?.toString(),
                binding.etString.editableText?.toString().orEmpty()
            )
        }

        binding.btnOpenGraphBWithoutParam.setOnClickListener {
            GraphBNoParamFragment.startGraphB(findNavController())
        }
        binding.btnOpenGraphBFragmentWithParam.setOnClickListener {
            GraphBParamFragment.start(
                findNavController(),
                binding.etIntB.editableText?.toString().toIntSafely(),
                binding.etStringNullableB.editableText?.toString(),
                binding.etStringB.editableText?.toString().orEmpty()
            )
        }

        binding.btnOpenGraphCWithParam.setOnClickListener {
            GraphCParamFragment.startGraphC(
                findNavController(),
                binding.etIntC.editableText?.toString().toIntSafely(),
                binding.etStringNullableC.editableText?.toString(),
                binding.etStringC.editableText?.toString().orEmpty()
            )
        }
        binding.btnOpenGraphCFragmentWithoutParam.setOnClickListener {
            GraphCNoParamFragment.start(findNavController())
        }
        binding.btnOpenGraphBNoParamFragmentAsThisGraph.setOnClickListener {
            GraphBNoParamFragment.startInGraphA(findNavController())
        }

        binding.btnOpenCommon.setOnClickListener {
            findNavController().navigate(
                CommonFragmentDeepLinkFactory.deepLinkInGraphA()
            )
        }

        return binding.root
    }
}
package github.xuqk.navigationprocessor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import github.xuqk.kd_nav_annotations.NavigationDeepLink
import github.xuqk.navigationprocessor.databinding.FragmentCommonBinding

/**
 * Created By：XuQK
 * Created Date：2021/11/26 13:44
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

@NavigationDeepLink(path = "/xxx/CommonFragment", graphLabel = "GraphA")
@NavigationDeepLink(path = "/CommonFragment", graphLabel = "GraphB")
@NavigationDeepLink(path = "/CommonFragment", graphLabel = "GraphC")
class CommonFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCommonBinding.inflate(inflater)

        binding.tvInfo.text = """
            GraphLabel: ${findNavController().graph.label}
            FragmentName: ${this.toString()}
        """.trimIndent()

        return binding.root
    }
}
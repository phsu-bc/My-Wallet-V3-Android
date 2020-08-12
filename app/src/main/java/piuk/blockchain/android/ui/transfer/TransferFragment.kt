package piuk.blockchain.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_transfer.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.receive.TransferReceiveFragment
import piuk.blockchain.android.ui.transfer.send.TransferSendFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class TransferFragment : Fragment() {

    private val pagerAdapter by lazy {
        TransferPagerAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_transfer)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transfer_pager.adapter = pagerAdapter

        TabLayoutMediator(transfer_tabs, transfer_pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.send)
                1 -> getString(R.string.common_receive)
                else -> ""
            }
        }.attach()
    }
    companion object {
        enum class StartingView {
            SHOW_SEND,
            SHOW_RECEIVE
        }

        private const val START_AT_VIEW = "START_AT_VIEW"
        fun newInstance(startAt: StartingView = StartingView.SHOW_SEND): TransferFragment {
            return TransferFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(START_AT_VIEW, startAt)
                }
            }
        }
    }
}

class TransferPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
        0 -> TransferSendFragment.newInstance()
        1 -> TransferReceiveFragment.newInstance()
        else -> throw IllegalStateException("Only two fragments allowed")
    }
}
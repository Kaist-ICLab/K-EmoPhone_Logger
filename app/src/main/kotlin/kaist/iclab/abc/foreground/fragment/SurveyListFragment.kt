package kaist.iclab.abc.foreground.fragment

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.util.Pair
import android.support.v4.view.ViewCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kaist.iclab.abc.R
import kaist.iclab.abc.common.base.BaseFragment
import kaist.iclab.abc.common.type.LoadStatus
import kaist.iclab.abc.common.ABCException
import kaist.iclab.abc.data.entities.SurveyEntity
import kaist.iclab.abc.foreground.activity.SurveyQuestionActivity
import kaist.iclab.abc.foreground.adapter.SurveyListAdapter
import kaist.iclab.abc.foreground.listener.OnRecyclerViewItemClickListener
import kaist.iclab.abc.foreground.view.SurveyItemView
import kotlinx.android.synthetic.main.fragment_survey_list.*

class SurveyListFragment : BaseFragment(), OnRecyclerViewItemClickListener<SurveyEntity> {
    companion object {
        private val ARG_SHOW_ONLY_UNREAD = "${SurveyListFragment::class.java.canonicalName}.ARG_SHOW_ONLY_UNREAD"

        const val PREFIX_TITLE_VIEW = "TITLE_VIEW"
        const val PREFIX_MESSAGE_VIEW = "MESSAGE_VIEW"
        const val PREFIX_DELIVERED_TIME_VIEW = "DELIVERED_TIME_VIEW"

        fun newInstance(showOnlyUnread: Boolean) = SurveyListFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_SHOW_ONLY_UNREAD, showOnlyUnread)
            }
        }
    }

    private lateinit var recyclerViewAdapter: SurveyListAdapter
    private lateinit var viewModel: SurveyListAdapter.EntityViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_survey_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupObserver()
        setupListener()

        bindView()
    }

    override fun onItemClick(position: Int, item: SurveyEntity?, view: View) {
        Log.d(TAG, "onItemClick(item = $item)")
        if(item == null) return
        val bundle = (view as? SurveyItemView)?.let { itemView ->
            val title = Pair.create(itemView.getTitleView(), ViewCompat.getTransitionName(itemView.getTitleView()) ?: "")
            val message = Pair.create(itemView.getMessageView(), ViewCompat.getTransitionName(itemView.getMessageView()) ?: "")
            val deliveredTime = Pair.create(itemView.getDeliveredTimeView(), ViewCompat.getTransitionName(itemView.getDeliveredTimeView()) ?: "")
            ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), title, message, deliveredTime).toBundle()
        }

        startActivity(SurveyQuestionActivity.newIntent(requireContext(), item), bundle)
    }

    private fun setupView() {
        recyclerViewAdapter = SurveyListAdapter()
    }

    private fun setupObserver() {
        viewModel = SurveyListAdapter.getEntityViewModel(this, arguments?.getBoolean(ARG_SHOW_ONLY_UNREAD) == true)

        viewModel.pagedList.observe(this, Observer {
            if (it != null) {
                recyclerViewAdapter.submitList(it)
            }
        })

        viewModel.initialLoadState.observe(this, Observer {
            swipeLayout.isRefreshing = it?.status == LoadStatus.RUNNING
            recyclerView.visibility = if(it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE
            txtError.visibility = if(it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            txtError.setText(
                if(it?.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })

        viewModel.loadState.observe(this, Observer {
            swipeLayout.isRefreshing = it?.status == LoadStatus.RUNNING
            recyclerView.visibility = if(it?.status == LoadStatus.FAILED) View.GONE else View.VISIBLE
            txtError.visibility = if(it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            txtError.setText(
                if(it?.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
            recyclerViewAdapter.notifyDataSetChanged()
        })
    }

    private fun setupListener() {
        recyclerViewAdapter.setOnRecyclerViewItemClickListener(this)
        swipeLayout.setOnRefreshListener { viewModel.refresh() }
    }

    private fun bindView() {
        recyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            itemAnimator = DefaultItemAnimator()
            adapter = recyclerViewAdapter
        }
    }
}
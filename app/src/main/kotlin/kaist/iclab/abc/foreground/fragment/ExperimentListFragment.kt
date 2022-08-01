package kaist.iclab.abc.foreground.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kaist.iclab.abc.R
import kaist.iclab.abc.common.base.BaseFragment
import kaist.iclab.abc.common.type.LoadState
import kaist.iclab.abc.common.type.LoadStatus
import kaist.iclab.abc.common.ABCException
import kaist.iclab.abc.foreground.activity.ExperimentDetailActivity
import kaist.iclab.abc.foreground.activity.MainActivity
import kaist.iclab.abc.foreground.adapter.ExperimentListAdapter
import kaist.iclab.abc.foreground.listener.OnRecyclerViewItemClickListener
import kaist.iclab.abc.protos.ExperimentProtos
import kotlinx.android.synthetic.main.fragment_experiment_list.*


class ExperimentListFragment : BaseFragment(), OnRecyclerViewItemClickListener<ExperimentProtos.ExperimentEssential> {
    private lateinit var recyclerViewAdapter: ExperimentListAdapter
    private lateinit var viewModel: ExperimentListAdapter.ExperimentDataViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_experiment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupListener()
        setupObserver()

        bindView()
    }

    private fun setupView() {
        recyclerViewAdapter = ExperimentListAdapter().apply {
            setOnItemClickListener(this@ExperimentListFragment)
        }
    }

    private fun setupObserver() {
        viewModel = ViewModelProviders.of(this).get(ExperimentListAdapter.ExperimentDataViewModel::class.java)

        viewModel.pagedList.observe(this, Observer {
            recyclerViewAdapter.submitList(it)
        })

        viewModel.initialLoadState.observe(this, Observer {
            swipeLayout.isRefreshing = it == LoadState.LOADING
            txtError.visibility = if (it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            recyclerView.visibility = if(it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE

            if (it?.error != null) txtError.setText(
                if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })

        viewModel.loadState.observe(this, Observer {
            swipeLayout.isRefreshing = it == LoadState.LOADING
            txtError.visibility = if (it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            recyclerView.visibility = if (it?.status == LoadStatus.FAILED) View.GONE else View.VISIBLE

            if (it?.error != null) txtError.setText(
                if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })
    }

    private fun setupListener() {
        swipeLayout.setOnRefreshListener { viewModel.refresh() }
    }

    private fun bindView() {
        recyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            itemAnimator = DefaultItemAnimator()
            adapter = recyclerViewAdapter
        }
    }

    override fun onItemClick(position: Int, item: ExperimentProtos.ExperimentEssential?, view: View) {
        item?.basic?.uuid?.let {
            startActivity(ExperimentDetailActivity.newIntent(requireContext(), item), ViewCompat.getTransitionName(view)?.let { name ->
                ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, name)
            }?.toBundle())
        }
    }
}
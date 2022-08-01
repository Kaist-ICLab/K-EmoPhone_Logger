package kaist.iclab.abc.foreground.listener

import android.support.v7.widget.RecyclerView
import android.view.View

abstract class BaseViewHolder<V: View, D>(val view: V, val onClick: (position: Int, view: View) -> Unit) : RecyclerView.ViewHolder(view){
    init {
        view.setOnClickListener {
            onClick(adapterPosition, view)
        }
    }

    abstract fun bindView(data: D)

    abstract fun clearView()
}
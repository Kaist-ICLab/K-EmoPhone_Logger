package kaist.iclab.abc.foreground.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog

class YesNoDialogFragment : DialogFragment() {
    companion object {
        private val ARG_TITLE = "${YesNoDialogFragment::class.java.canonicalName}.ARG_TITLE"
        private val ARG_MESSAGE = "${YesNoDialogFragment::class.java.canonicalName}.ARG_MESSAGE"

        fun newInstance(title: String, message: String) = YesNoDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
            }
        }
    }

    private var onDialogOptionSelectedListener: ((isYes: Boolean) -> Unit)? = null


    fun setOnDialogOptionSelectedListener(listener: ((isYes: Boolean) -> Unit)?) {
        onDialogOptionSelectedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return context?.let { context ->
            AlertDialog.Builder(context)
                .setTitle(arguments?.getString(ARG_TITLE) ?: "")
                .setMessage(arguments?.getString(ARG_MESSAGE) ?: "")
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    onDialogOptionSelectedListener?.invoke(true)
                }.setNegativeButton(android.R.string.no) { _, _ ->
                    onDialogOptionSelectedListener?.invoke(false)
                }.create()
        } ?: super.onCreateDialog(savedInstanceState)
    }
}
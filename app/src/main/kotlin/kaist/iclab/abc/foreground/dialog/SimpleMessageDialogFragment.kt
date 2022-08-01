package kaist.iclab.abc.foreground.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import kaist.iclab.abc.R

class SimpleMessageDialogFragment: DialogFragment() {
    companion object {
        private val ARG_TITLE = "${SimpleMessageDialogFragment::class.java.canonicalName}.ARG_TITLE"
        private val ARG_MESSAGE = "${SimpleMessageDialogFragment::class.java.canonicalName}.ARG_MESSAGE"

        fun newInstance(title: String, message: String) = SimpleMessageDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return context?.let { context ->
            AlertDialog.Builder(context)
                .setTitle(arguments?.getString(ARG_TITLE) ?: "")
                .setMessage(arguments?.getString(ARG_MESSAGE) ?: "")
                .setNeutralButton(R.string.general_close) { _, _ -> dismiss() }
                .create()
        } ?: super.onCreateDialog(savedInstanceState)
    }
}
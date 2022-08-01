package kaist.iclab.abc.foreground.listener

import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.TextWatcher

class ErrorWatcher(private val editText: TextInputLayout,
                   private val errorMsg: String,
                   private val checkValidity: (text: String) -> Boolean): TextWatcher {
    override fun afterTextChanged(s: Editable?) { }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val errorTxt = if (s == null || !checkValidity(s.toString())) errorMsg else null
        editText.isErrorEnabled = errorTxt != null
        editText.error = if (s == null || !checkValidity(s.toString())) errorMsg else null
    }
}
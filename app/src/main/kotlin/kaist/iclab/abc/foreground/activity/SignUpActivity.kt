package kaist.iclab.abc.foreground.activity

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.*
import kaist.iclab.abc.R
import kaist.iclab.abc.common.ABCException
import kaist.iclab.abc.common.InvalidContentException
import kaist.iclab.abc.common.NoNetworkAvailableException
import kaist.iclab.abc.common.base.BaseAppCompatActivity
import kaist.iclab.abc.common.type.LoadState
import kaist.iclab.abc.common.type.LoadStatus
import kaist.iclab.abc.common.util.*
import kaist.iclab.abc.foreground.listener.ErrorWatcher
import kotlinx.android.synthetic.main.activity_container_with_toolbar.*
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors


class SignUpActivity : BaseAppCompatActivity() {
    private lateinit var loadStateLiveData: MutableLiveData<LoadState>
    private lateinit var emailWatcher: ErrorWatcher
    private lateinit var passwordWatcher: ErrorWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupContentView()
        setupActionBar()
        setupObservers()
        setupListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }


    private fun setupContentView() {
        setContentView(R.layout.activity_container_with_toolbar)
        container.addView(LayoutInflater.from(this).inflate(R.layout.activity_sign_up, container, false))

        emailWatcher = ErrorWatcher(edtEmail, getString(R.string.edt_error_email_invalid)) {
            FormatUtils.validateEmail(it)
        }
        passwordWatcher = ErrorWatcher(edtPassword, getString(R.string.edt_error_password_invalid)) {
            FormatUtils.validateTextLength(it, 8)
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = getString(R.string.activity_title_sign_up)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupObservers() {
        loadStateLiveData = MutableLiveData()

        loadStateLiveData.observe(this, Observer {
            edtEmail.isEnabled = it?.status != LoadStatus.RUNNING
            edtPassword.isErrorEnabled = it?.status != LoadStatus.RUNNING
            when(it?.status) {
                LoadStatus.RUNNING -> btnSignUp.startWith ()
                LoadStatus.SUCCESS -> btnSignUp.succeedWith(false) {
                    ViewUtils.showToast(this, R.string.msg_auth_request_verify_email)
                    finish()
                }
                LoadStatus.FAILED -> btnSignUp.failedWith {
                    ViewUtils.showToast(this,
                        when (it.error) {
                            is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
                            is FirebaseAuthInvalidCredentialsException -> R.string.error_invalid_email
                            is FirebaseAuthUserCollisionException -> R.string.error_user_already_exists
                            is ABCException -> it.error.getErrorStringRes()
                            else -> R.string.error_general_error
                        }
                    )
                }
                else -> { }
            }
        })
    }

    private fun setupListeners() {
        edtEmail.editText?.addTextChangedListener(emailWatcher)
        edtPassword.editText?.addTextChangedListener(passwordWatcher)

        btnSignUp.setOnClickListener {
            loadStateLiveData.postValue(LoadState.LOADING)

            val executor = Executors.newSingleThreadExecutor()
            Tasks.call(executor, Callable {
                if(!NetworkUtils.isNetworkAvailable(this)) throw NoNetworkAvailableException()
                if(!FormatUtils.validateEmail(edtEmail) || !FormatUtils.validateTextLength(edtPassword, 8)) throw InvalidContentException()

                val email = edtEmail.editText?.text?.toString()!!
                val password = edtPassword.editText?.text?.toString()!!
                Pair(email, password)
            }).onSuccessTask(executor, SuccessContinuation<Pair<String, String>, AuthResult> {result ->
                val auth = FirebaseAuth.getInstance()
                auth.useAppLanguage()
                val email = result?.first!!
                val password = result.second

                auth.createUserWithEmailAndPassword(email, password)
            }).onSuccessTask(executor, SuccessContinuation<AuthResult, Void> { result ->
                result?.user?.sendEmailVerification()!!
            }).addOnSuccessListener {_ ->
                loadStateLiveData.postValue(LoadState.LOADED)
            }.addOnFailureListener { exception ->
                loadStateLiveData.postValue(LoadState.ERROR(exception))
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, SignUpActivity::class.java)
    }
}
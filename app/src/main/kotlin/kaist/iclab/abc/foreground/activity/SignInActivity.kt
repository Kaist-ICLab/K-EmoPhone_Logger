package kaist.iclab.abc.foreground.activity

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kaist.iclab.abc.R
import kaist.iclab.abc.common.*
import kaist.iclab.abc.common.base.BaseAppCompatActivity
import kaist.iclab.abc.common.type.LoadState
import kaist.iclab.abc.common.type.LoadStatus
import kaist.iclab.abc.common.util.*
import kaist.iclab.abc.data.FirestoreAccessor
import kaist.iclab.abc.data.entities.ParticipationEntity
import kaist.iclab.abc.foreground.dialog.YesNoDialogFragment
import kaist.iclab.abc.foreground.listener.ErrorWatcher
import kotlinx.android.synthetic.main.activity_container_without_toolbar.*
import kotlinx.android.synthetic.main.activity_sign_in.*
import java.lang.Exception
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class SignInActivity : BaseAppCompatActivity() {
    private lateinit var loadState: MutableLiveData<LoadState>
    private lateinit var emailWatcher: ErrorWatcher
    private lateinit var passwordWatcher: ErrorWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupContentView()
        setupObservers()
        setupListener()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if(intent?.hasExtra(EXTRA_SIGN_IN_EMAIL) == true) {
            edtEmail.editText?.setText( intent.getStringExtra(EXTRA_SIGN_IN_EMAIL) ?: "", TextView.BufferType.EDITABLE)
        }
    }

    private fun setupContentView() {
        setContentView(R.layout.activity_container_without_toolbar)
        container.addView(layoutInflater.inflate(R.layout.activity_sign_in, container, false))

        emailWatcher = ErrorWatcher(edtEmail, getString(R.string.edt_error_email_invalid)) {
            FormatUtils.validateEmail(it)
        }
        passwordWatcher = ErrorWatcher(edtPassword, getString(R.string.edt_error_password_invalid)) {
            FormatUtils.validateTextLength(it, 8)
        }
    }


    private fun setupObservers() {
        loadState = MutableLiveData()

        loadState.observe(this, Observer {
            edtEmail.isEnabled = it?.status != LoadStatus.RUNNING
            edtPassword.isEnabled = it?.status != LoadStatus.RUNNING

            when(it?.status) {
                LoadStatus.RUNNING -> btnSignIn.startWith()
                LoadStatus.SUCCESS -> btnSignIn.succeedWith(false) {
                    startActivity(
                        MainActivity.newIntent(this, true)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                }
                LoadStatus.FAILED -> btnSignIn.failedWith {
                    ViewUtils.showToast(this,
                        when (it.error) {
                            is FirebaseAuthInvalidUserException -> R.string.error_no_account
                            is FirebaseAuthInvalidCredentialsException -> R.string.error_auth_password_incorrect
                            is ABCException -> it.error.getErrorStringRes()
                            else -> R.string.error_general_error
                        })
                }
                else -> { }
            }
        })
    }

    private fun setupListener() {
        edtEmail.editText?.addTextChangedListener(emailWatcher)
        edtPassword.editText?.addTextChangedListener(passwordWatcher)

        btnCreateAccount.setOnClickListener {
            startActivity(SignUpActivity.newIntent(this))
        }

        btnForgetPassword.setOnClickListener {
            startActivity(PasswordResetActivity.newIntent(this))
        }

        btnSignIn.setOnClickListener {
            loadState.postValue(LoadState.LOADING)

            val executor = Executors.newSingleThreadExecutor()

            Tasks.call(executor, Callable {
                if (!NetworkUtils.isNetworkAvailable(this)) throw NoNetworkAvailableException()
                if (!FormatUtils.validateEmail(edtEmail) || !FormatUtils.validateTextLength(edtPassword, 8)) throw InvalidContentException()
                val email = edtEmail.editText?.text?.toString()!!
                val password = edtPassword.editText?.text?.toString()!!
                Pair(email, password)
            }).onSuccessTask(executor, SuccessContinuation<Pair<String, String>, AuthResult> { result ->
                val email = result?.first!!
                val password = result.second
                val auth = FirebaseAuth.getInstance()
                auth.useAppLanguage()
                auth.signInWithEmailAndPassword(email, password)
            }).onSuccessTask(executor, SuccessContinuation<AuthResult, FirestoreAccessor.SubjectData> { result ->
                val user = result?.user ?: throw NoSignedAccountException()
                if (!user.isEmailVerified) throw NotVerifiedAccountException()
                FirestoreAccessor.get(user.email!!)
            }).onSuccessTask(executor, SuccessContinuation<FirestoreAccessor.SubjectData, ParticipationEntity?> { result ->
                //if(result?.uuid == PreferenceAccessor.getInstance(this).deviceUuid) throw AlreadySignedInAccountException()
                Tasks.call {
                    try {
                    ParticipationEntity.getParticipatedExperimentFromServer(this)
                    } catch (e: Exception) {
                        null
                    }
                }
            }).addOnSuccessListener { _ ->
                loadState.postValue(LoadState.LOADED)
            }.addOnFailureListener {exception ->
                if(exception is AlreadySignedInAccountException) {
                    val title = getString(R.string.dialog_title_already_sign_in)
                    val message = getString(exception.getErrorStringRes())
                    val dialog = YesNoDialogFragment.newInstance(title, message)
                    dialog.setOnDialogOptionSelectedListener { result ->
                        if (result) {
                            loadState.postValue(LoadState.LOADED)
                        } else {
                            loadState.postValue(LoadState.ERROR(SignInCanceledException()))
                        }
                    }
                    dialog.show(supportFragmentManager, TAG)
                } else {
                    loadState.postValue(LoadState.ERROR(exception))
                }
            }
        }
    }

    companion object {
        private val EXTRA_SIGN_IN_EMAIL = "${SignInActivity::class.java.canonicalName}.EXTRA_SIGN_IN_EMAIL"

        fun newIntent(context: Context, email: String? = null) : Intent = Intent(context, SignInActivity::class.java).apply {
            if(!TextUtils.isEmpty(email)) putExtra(EXTRA_SIGN_IN_EMAIL, email)
        }
    }
}
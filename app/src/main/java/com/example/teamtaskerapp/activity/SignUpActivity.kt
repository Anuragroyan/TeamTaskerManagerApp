package com.example.teamtaskerapp.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import com.example.teamtaskerapp.R
import com.example.teamtaskerapp.firebase.FireStoreClass
import com.example.teamtaskerapp.models.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase

class SignUpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setupActionBar()
    }

    fun userRegisteredSuccess(){
        Toast.makeText(
            this@SignUpActivity,
            "You have successfully registered",
            Toast.LENGTH_LONG
        ).show()
        hideProgressDialog()
        FirebaseAuth.getInstance().signOut()
        finish()
    }

    private fun setupActionBar() {

        setSupportActionBar(findViewById(R.id.toolbar_sign_up_activity))

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_chevron_left_24)
        }

        findViewById<Toolbar>(R.id.toolbar_sign_up_activity).setNavigationOnClickListener { onBackPressed() }
        findViewById<Button>(R.id.btn_sign_up).setOnClickListener{
            registerUser()
        }
    }

    private fun registerUser() {
        val name: String = findViewById<TextView>(R.id.et_name).text.toString().trim { it <= ' ' }
        val email: String = findViewById<TextView>(R.id.et_email).text.toString().trim { it <= ' ' }
        val password: String =
            findViewById<TextView>(R.id.et_password).text.toString().trim { it <= ' ' }
        if (validateForm(name, email, password)) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password).addOnCompleteListener(
                    OnCompleteListener<AuthResult> { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = task.result!!.user!!
                            val registeredEmail = firebaseUser.email!!
                            val user = User(firebaseUser.uid, name, registeredEmail)
                            FireStoreClass().registeredUser(this, user)
                        } else {
                            Toast.makeText(
                                this@SignUpActivity,
                                task.exception!!.message.toString(),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
        }
    }

    private fun validateForm(
        name: String, email: String, password: String
    ): Boolean{
       return when{
           TextUtils.isEmpty(name) ->{
               showErrorSnackBar("Please enter a name")
               false
           }
           TextUtils.isEmpty(email) -> {
               showErrorSnackBar("Please enter an email address")
               false
           }
           TextUtils.isEmpty(password) -> {
               showErrorSnackBar("Please enter a password")
               false
           }else -> {
               true
           }
       }
    }



}
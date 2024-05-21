package com.example.teamtaskerapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teamtaskerapp.R
import com.example.teamtaskerapp.adapter.BoardItemAdapter
import com.example.teamtaskerapp.firebase.FireStoreClass
import com.example.teamtaskerapp.models.Board
import com.example.teamtaskerapp.models.User
import com.example.teamtaskerapp.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.messaging.messaging


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object {
        const val MY_PROFILE_REQUEST_CODE : Int = 11
        const val CREATE_BOARD_REQUEST_CODE : Int = 12
    }
    private lateinit var mUsername: String
    private lateinit var mSharedPreferences: SharedPreferences
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupActionBar()
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)
        mSharedPreferences = this.getSharedPreferences(Constants.TEAMTASKER_PREFERENCES, MODE_PRIVATE)
        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)
        if(tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStoreClass().loadUserData(this, true)
        }else{
            Firebase.messaging.getToken().addOnSuccessListener(this@MainActivity) { instanceIdResult ->
                if (instanceIdResult != null) {
                    updateFCMToken(instanceIdResult)
                }
            }
        }
        FireStoreClass().loadUserData(this, true)
        findViewById<FloatingActionButton>(R.id.fab_board_add).setOnClickListener{
              val intent = Intent(this,
                    CreateBoardActivity::class.java)
                intent.putExtra(Constants.NAME, mUsername)
                startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }

    }

    fun populateBoardsListToUI(boardsList: ArrayList<Board>){
        hideProgressDialog()
        if(boardsList.size>0){
            findViewById<RecyclerView>(R.id.rv_boards_list).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_no_boards_available).visibility = View.GONE
            findViewById<RecyclerView>(R.id.rv_boards_list).layoutManager = LinearLayoutManager(this)
            findViewById<RecyclerView>(R.id.rv_boards_list).setHasFixedSize(true)
            val adapter = BoardItemAdapter(this, boardsList)
            findViewById<RecyclerView>(R.id.rv_boards_list).adapter = adapter
            adapter.setOnClickListener(object: BoardItemAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }
            })
        }else{
            findViewById<RecyclerView>(R.id.rv_boards_list).visibility = View.GONE
            findViewById<TextView>(R.id.tv_no_boards_available).visibility = View.VISIBLE
        }
    }

    private fun setupActionBar(){
        setSupportActionBar(findViewById(R.id.toolbar_main_activity))
        findViewById<Toolbar>(R.id.toolbar_main_activity).setNavigationIcon(R.drawable.baseline_menu_24)
        findViewById<Toolbar>(R.id.toolbar_main_activity).setNavigationOnClickListener{
            toggleMenu()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE){
            FireStoreClass().loadUserData(this@MainActivity)
        }else if(resultCode == RESULT_OK && requestCode == CREATE_BOARD_REQUEST_CODE){
            FireStoreClass().getBoardsList(this)
        }
        else{
            Log.e("Cancelled", "Cancelled")
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.nav_my_profile -> {
               startActivityForResult(Intent(this@MainActivity, ProfileActivity::class.java),
                   MY_PROFILE_REQUEST_CODE)
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()
                val intent = Intent(this@MainActivity, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        return true
    }

    private fun toggleMenu(){
        if(findViewById<DrawerLayout>(R.id.drawer_layout).isDrawerOpen(GravityCompat.START)){
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        }else{
            findViewById<DrawerLayout>(R.id.drawer_layout).openDrawer(GravityCompat.START)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if(findViewById<DrawerLayout>(R.id.drawer_layout).isDrawerOpen(GravityCompat.START)){
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        }else{
            doubleBackToExit()
        }
    }

    fun updateNavigationUserDetails(user: User, readBoardsList: Boolean) {
        hideProgressDialog()
      mUsername = user.name
      Glide
          .with(this@MainActivity)
          .load(user.image)
          .centerCrop()
          .placeholder(R.drawable.user)
          .into(findViewById(R.id.nav_user_image))

      findViewById<TextView>(R.id.nav_username).text = user.name
        if(readBoardsList){
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStoreClass().getBoardsList(this)
        }
    }

    fun tokenUpdateSuccess() {
        hideProgressDialog()
        val editor : SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().loadUserData(this, true)
    }

    fun updateFCMToken(token : String){
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().updateUserProfileData(this, userHashMap)

    }
}






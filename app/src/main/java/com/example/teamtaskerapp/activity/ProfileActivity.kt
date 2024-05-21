package com.example.teamtaskerapp.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.teamtaskerapp.R
import com.example.teamtaskerapp.firebase.FireStoreClass
import com.example.teamtaskerapp.models.User
import com.example.teamtaskerapp.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class ProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private var mProfileImageURL : String = ""
    private lateinit var mUserDetails: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_activity)
        setupActionBar()
        FireStoreClass().loadUserData(this)
        findViewById<ImageView>(R.id.profile_user_image).setOnClickListener{
           if(ContextCompat.checkSelfPermission(
               this,
               android.Manifest.permission.READ_EXTERNAL_STORAGE)
               == PackageManager.PERMISSION_GRANTED){
              Constants.showImageChooser(this)
           }else{
               ActivityCompat.requestPermissions(
                       this@ProfileActivity,
                   arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                   Constants.READ_STORAGE_PERMISSION_CODE
               )
           }
        }
        findViewById<Button>(R.id.btn_update_profile).setOnClickListener{
            if(mSelectedImageFileUri!=null){
              uploadUserImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                updateUserProfileData()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.READ_STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Constants.showImageChooser(this)
            }
        }else{
            Toast.makeText(
                this,
                "Oops you just denied the permission for storage. You can allow it from the settings",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK &&
             requestCode == Constants.PICK_IMAGE_REQUEST_CODE &&
            data!!.data != null){
            mSelectedImageFileUri = data.data
            try {
                Glide
                    .with(this@ProfileActivity)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.user)
                    .into(findViewById(R.id.profile_user_image))
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    private fun setupActionBar(){
        setSupportActionBar(findViewById(R.id.toolbar_my_profile_activity))
        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_chevron_left_24)
            actionBar.title = resources.getString(R.string.my_profile)
        }
        findViewById<Toolbar>(R.id.toolbar_my_profile_activity).setNavigationOnClickListener{
            onBackPressed()
        }
    }

    fun setUserDataInUI(user: User){
        mUserDetails = user
        Glide
            .with(this@ProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.user)
            .into(findViewById(R.id.profile_user_image))
        findViewById<TextView>(R.id.et_profile_name).text = user.name
        findViewById<TextView>(R.id.et_profile_email).text = user.email
        if(user.mobile!=0L){
            findViewById<TextView>(R.id.et_profile_mobile).text = user.mobile.toString()
        }
    }

    private fun updateUserProfileData(){
        val userHashMap = HashMap<String, Any>()
        if(mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image){
            userHashMap[Constants.IMAGE]=mProfileImageURL
        }
        if(findViewById<TextView>(R.id.et_profile_name).text.toString() != mUserDetails.name){
            userHashMap[Constants.NAME]=findViewById<TextView>(R.id.et_profile_name).text.toString()
        }
        if(findViewById<TextView>(R.id.et_profile_mobile).text.toString() != mUserDetails.mobile.toString()){
            userHashMap[Constants.MOBILE]=findViewById<TextView>(R.id.et_profile_mobile).text.toString().toLong()
        }
        FireStoreClass().updateUserProfileData(this@ProfileActivity,userHashMap)
    }
    
    private fun uploadUserImage(){
        showProgressDialog(resources.getString(R.string.please_wait))
        if(mSelectedImageFileUri!=null){
            val sRef : StorageReference = FirebaseStorage.getInstance()
                .reference.child(
                "USER_IMAGE" + System.currentTimeMillis()
                    + "." + Constants.getFileExtension(this,mSelectedImageFileUri)
            )
            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener{
                taskSnapShot ->
                Log.i("Firebase Image URL",
                    taskSnapShot.metadata!!.reference!!.downloadUrl.toString()
                    )
                taskSnapShot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                    Log.i("Downloadable Image URL", uri.toString())
                    mProfileImageURL = uri.toString()
                    updateUserProfileData()
                }
            }.addOnFailureListener{
                exception ->
                Toast.makeText(
                    this@ProfileActivity,
                    exception.message,
                    Toast.LENGTH_SHORT
                ).show()
                hideProgressDialog()
            }
        }
    }


    fun profileUpdateSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }
}
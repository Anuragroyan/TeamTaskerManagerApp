package com.example.teamtaskerapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teamtaskerapp.R
import com.example.teamtaskerapp.adapter.MemberListItemsAdapter
import com.example.teamtaskerapp.firebase.FireStoreClass
import com.example.teamtaskerapp.models.Board
import com.example.teamtaskerapp.models.User
import com.example.teamtaskerapp.utils.Constants
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MembersActivity : BaseActivity() {
    private lateinit var mBoardDetails : Board
    private lateinit var mAssignedMembersList: ArrayList<User>
    private var anyChangesMade: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_members)
        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails = intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!
        }
        setupActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().getAssignedMembersListDetails(this, mBoardDetails.assignedTo)
    }

    @SuppressLint("CutPasteId")
    fun setupMembersList(list: ArrayList<User>){
        mAssignedMembersList = list
        hideProgressDialog()
        findViewById<RecyclerView>(R.id.rv_members_list).layoutManager = LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.rv_members_list).setHasFixedSize(true)
        val adapter = MemberListItemsAdapter(this, list)
        findViewById<RecyclerView>(R.id.rv_members_list).adapter = adapter
    }

    fun memberDetails(user: User){
        mBoardDetails.assignedTo.add(user.id)
        FireStoreClass().assignMemberToBoard(this, mBoardDetails, user)

    }

    private fun setupActionBar(){
        setSupportActionBar(findViewById(R.id.toolbar_members_activity))
        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_chevron_left_24)
            actionBar.title = resources.getString(R.string.members)
        }
        findViewById<Toolbar>(R.id.toolbar_members_activity).setNavigationOnClickListener{
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_add_member -> {
               dialogSearchMember()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun dialogSearchMember(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_search_member)
        dialog.findViewById<TextView>(R.id.tv_add).setOnClickListener {
           val email = dialog.findViewById<EditText>(R.id.et_email_search_member).text.toString()
           if(email.isNotEmpty()){
               dialog.dismiss()
               showProgressDialog(resources.getString(R.string.please_wait))
               FireStoreClass().getMemberDetails(this, email)
           }else{
               Toast.makeText(this@MembersActivity, "Please enter email address", Toast.LENGTH_SHORT).show()
           }
        }
        dialog.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if(anyChangesMade){
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    fun memberAssignSuccess(user:User){
        hideProgressDialog()
        mAssignedMembersList.add(user)
        anyChangesMade = true
        setupMembersList(mAssignedMembersList)
        SendNotificationToUserAsyncTask(mBoardDetails.name, user.fcmToken).execute()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class SendNotificationToUserAsyncTask(
        val boardName: String, val token: String): AsyncTask<Any, Void, String>(){
        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog(resources.getString(R.string.please_wait))
        }
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Any?): String {
            var result : String
            var connection : HttpURLConnection? = null
            try{
              val url = URL(Constants.FCM_BASE_URL)
              connection = url.openConnection() as HttpURLConnection
               connection.doOutput = true
               connection.doInput = true
               connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset","utf-8")
                connection.setRequestProperty("Accept","application/json")
                connection.setRequestProperty(
                    Constants.FCM_AUTHORIZATION, "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}"
                )
                connection.useCaches = false
                val wr = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, "Assigned to the board $boardName")
                dataObject.put(Constants.FCM_KEY_MESSAGE,"You have been assigned to the Board by ${mAssignedMembersList[0].name}")
                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)
                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()
                val httpResult : Int = connection.responseCode
                if(httpResult == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(
                        InputStreamReader(inputStream)
                    )
                    val sb = StringBuilder()
                    var line : String?
                    try{
                        while(reader.readLine().also {line=it}!=null){
                            sb.append(line+"\n")
                        }
                    }catch (e: IOException){
                        e.printStackTrace()
                    }finally {
                        try{
                            inputStream.close()
                        }catch (e:IOException){
                            e.printStackTrace()
                        }
                    }
                    result = sb.toString()
                }else{
                    result = connection.responseMessage
                }
            }catch (e: SocketTimeoutException){
                result = "Connection Timeout"
            }catch (e: Exception){
                result = "Error : " + e.message
            }finally {
                connection?.disconnect()
            }
            return result
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideProgressDialog()
            if (result != null) {
                Log.e("JSON Response Result", result)
            }
        }
    }
}
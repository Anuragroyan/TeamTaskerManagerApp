package com.example.teamtaskerapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teamtaskerapp.R
import com.example.teamtaskerapp.adapter.TaskListItemsAdapter
import com.example.teamtaskerapp.firebase.FireStoreClass
import com.example.teamtaskerapp.models.Board
import com.example.teamtaskerapp.models.Card
import com.example.teamtaskerapp.models.Task
import com.example.teamtaskerapp.models.User
import com.example.teamtaskerapp.utils.Constants

class TaskListActivity : BaseActivity() {
    private lateinit var mBoardDetails : Board
    private lateinit var mBoardDocumentId : String
    lateinit var mAssignedMemberDetailsList: ArrayList<User>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_list)
        if(intent.hasExtra(Constants.DOCUMENT_ID)){
            mBoardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }
       showProgressDialog(resources.getString(R.string.please_wait))
       FireStoreClass().getBoardDetails(this@TaskListActivity, mBoardDocumentId)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n     which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == MEMBERS_REQUEST_CODE || requestCode == CARD_DETAILS_REQUEST_CODE){
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStoreClass().getBoardDetails(this@TaskListActivity, mBoardDocumentId)
        }else{
            Log.e("Cancelled", "Cancelled")
        }
    }

    fun cardDetails(taskListPosition: Int, cardPosition: Int){
        val intent = Intent(this, CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION, taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION, cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST, mAssignedMemberDetailsList)
        startActivityForResult(intent, CARD_DETAILS_REQUEST_CODE)
    }

    private fun setupActionBar(){
        setSupportActionBar(findViewById(R.id.toolbar_task_list_activity))
        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_chevron_left_24)
            actionBar.title = mBoardDetails.name
        }
        findViewById<Toolbar>(R.id.toolbar_task_list_activity).setNavigationOnClickListener{
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_members -> {
                val intent =  Intent(this@TaskListActivity, MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
                startActivityForResult(intent, MEMBERS_REQUEST_CODE)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("CutPasteId")
    fun boardDetails(board: Board){
        mBoardDetails = board
        hideProgressDialog()
        setupActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().getAssignedMembersListDetails(this, mBoardDetails.assignedTo)
    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().getBoardDetails(this@TaskListActivity, mBoardDetails.documentId)
    }

    fun createTaskList(taskListName: String){
        val task = Task(taskListName, FireStoreClass().getCurrentUserId())
        mBoardDetails.taskList.add(0, task)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this@TaskListActivity, mBoardDetails)
    }

    fun updateTaskList(position: Int, listName: String, model: Task){
        val task = Task(listName, model.createdBy)

        mBoardDetails.taskList[position]=task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this@TaskListActivity, mBoardDetails)
    }

    fun deleteTaskList(position: Int){
      mBoardDetails.taskList.removeAt(position)
      mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
      showProgressDialog(resources.getString(R.string.please_wait))
      FireStoreClass().addUpdateTaskList(this@TaskListActivity, mBoardDetails)
    }

    fun addCardToTaskList(position: Int, cardName: String){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        val cardAssignedUserList: ArrayList<String> = ArrayList()
        cardAssignedUserList.add(FireStoreClass().getCurrentUserId())
        val card = Card(cardName, FireStoreClass().getCurrentUserId(), cardAssignedUserList)
        val cardsList = mBoardDetails.taskList[position].cards
        cardsList.add(card)
        val task = Task(
            mBoardDetails.taskList[position].title,
            mBoardDetails.taskList[position].createdBy,
            cardsList
        )
        mBoardDetails.taskList[position] = task
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this@TaskListActivity, mBoardDetails)
    }

    @SuppressLint("CutPasteId")
    fun boardMembersDetailsList(list: ArrayList<User>){
        mAssignedMemberDetailsList = list

        hideProgressDialog()
        val addTaskList = Task(resources.getString(R.string.add_list))
        mBoardDetails.taskList.add(addTaskList)
        findViewById<RecyclerView>(R.id.rv_task_list).layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false)
        findViewById<RecyclerView>(R.id.rv_task_list).setHasFixedSize(true)
        val adapter = TaskListItemsAdapter(this, mBoardDetails.taskList)
        findViewById<RecyclerView>(R.id.rv_task_list).adapter = adapter
    }

    fun updateCardsInTaskList(taskListPosition: Int, cards: ArrayList<Card>){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        mBoardDetails.taskList[taskListPosition].cards = cards
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    companion object{
        const val MEMBERS_REQUEST_CODE : Int = 13
        const val CARD_DETAILS_REQUEST_CODE : Int = 14
    }
}
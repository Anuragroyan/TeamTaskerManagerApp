package com.example.teamtaskerapp.activity


import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teamtaskerapp.R
import com.example.teamtaskerapp.adapter.CardMemberListItemsAdapter
import com.example.teamtaskerapp.dialogs.LabelColorListDialog
import com.example.teamtaskerapp.dialogs.MembersListDialog
import com.example.teamtaskerapp.firebase.FireStoreClass
import com.example.teamtaskerapp.models.Board
import com.example.teamtaskerapp.models.Card
import com.example.teamtaskerapp.models.SelectedMembers
import com.example.teamtaskerapp.models.Task
import com.example.teamtaskerapp.models.User
import com.example.teamtaskerapp.utils.Constants
import java.text.SimpleDateFormat
import java.time.Year
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CardDetailsActivity : BaseActivity() {
    private lateinit var mBoardDetails :Board
    private var mTaskListPosition: Int = -1
    private var mCardPosition: Int = -1
    private var mSelectedColor: String = ""
    private lateinit var mMembersDetailList: ArrayList<User>
    private var mSelectedDueDateMilliSeconds: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_card_details)
        getIntentData()
        setupActionBar()
        findViewById<EditText>(R.id.et_name_card_details).setText(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
        findViewById<EditText>(R.id.et_name_card_details).setSelection(findViewById<EditText>(R.id.et_name_card_details).text.toString().length)
        mSelectedColor = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].labelColor
        if(mSelectedColor.isNotEmpty()){
            setColor()
        }
        findViewById<Button>(R.id.btn_update_card_details).setOnClickListener {
            if(findViewById<EditText>(R.id.et_name_card_details).text.toString().isNotEmpty()){
                updateCardDetails()
            }else{
                Toast.makeText(this@CardDetailsActivity, "Please enter a card name", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<TextView>(R.id.tv_select_label_color).setOnClickListener {
            labelColorsDialog()
        }
        findViewById<TextView>(R.id.tv_select_member).setOnClickListener {
            membersListDialog()
        }
        setupSelectedMemberList()
        mSelectedDueDateMilliSeconds = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].dueDate
        if(mSelectedDueDateMilliSeconds > 0){
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.CANADA)
            val selectedDate = simpleDateFormat.format(Date(mSelectedDueDateMilliSeconds))
            findViewById<TextView>(R.id.tv_select_due_date).text = selectedDate
        }
        findViewById<TextView>(R.id.tv_select_due_date).setOnClickListener {
            showDataPicker()
        }
    }

    private fun deleteCard(){
        val cardsList: ArrayList<Card> = mBoardDetails.taskList[mTaskListPosition].cards
        cardsList.removeAt(mCardPosition)
        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size-1)
        taskList[mTaskListPosition].cards = cardsList
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    private fun alertDialogForDeleteCard(cardName: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.alert))
        builder.setMessage(
            resources.getString(
                R.string.confirmation_message_to_delete_card,
                cardName
            )
        )
        builder.setIcon(R.drawable.baseline_add_alert_24)
        builder.setPositiveButton(resources.getString(R.string.yes)){
            dialogInterface, _ ->
            dialogInterface.dismiss()
            deleteCard()
        }
        builder.setNegativeButton(resources.getString(R.string.no)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun setupActionBar(){
        setSupportActionBar(findViewById(R.id.toolbar_card_details_activity))
        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_chevron_left_24)
            actionBar.title = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name
        }
        findViewById<Toolbar>(R.id.toolbar_card_details_activity).setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun getIntentData(){
        if(intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)){
            mTaskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }
        if(intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)){
            mCardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            mBoardDetails = intent.getParcelableExtra(Constants.BOARD_DETAIL)!!
        }
        if(intent.hasExtra(Constants.BOARD_MEMBERS_LIST)){
            mMembersDetailList = intent.getParcelableArrayListExtra(
                Constants.BOARD_MEMBERS_LIST)!!
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_delete_card -> {
                alertDialogForDeleteCard( mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun addUpdateTaskListSuccess(){
       hideProgressDialog()
       setResult(Activity.RESULT_OK)
       finish()
    }

    private fun updateCardDetails(){
        val card = Card(
            findViewById<EditText>(R.id.et_name_card_details).text.toString(),
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].createdBy,
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo,
            mSelectedColor,
            mSelectedDueDateMilliSeconds
        )
        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size-1)
        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition] = card
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    private fun colorsList(): ArrayList<String>{
        val colorsList: ArrayList<String> = ArrayList()
        colorsList.add("#43C86F")
        colorsList.add("#0C90F1")
        colorsList.add("#F72400")
        colorsList.add("#7A8089")
        colorsList.add("#D57C1D")
        colorsList.add("#770000")
        colorsList.add("#0022F8")
        return colorsList
    }

    private fun setColor(){
        findViewById<TextView>(R.id.tv_select_label_color).text = ""
        findViewById<TextView>(R.id.tv_select_label_color).setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    private fun labelColorsDialog(){
        val colorsList : ArrayList<String> = colorsList()
        val listDialog = object : LabelColorListDialog(
            this,
            colorsList,
            resources.getString(R.string.str_select_label_color), mSelectedColor){
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }
        }
        listDialog.show()
    }

    private fun membersListDialog(){
        val cardAssignedMembersList = mBoardDetails
            .taskList[mTaskListPosition].cards[mCardPosition].assignedTo

        if(cardAssignedMembersList.size > 0){
            for(i in mMembersDetailList.indices){
                for(j in cardAssignedMembersList){
                    if(mMembersDetailList[i].id == j){
                        mMembersDetailList[i].selected = true
                    }
                }
            }
        }else{
            for(i in mMembersDetailList.indices){
                mMembersDetailList[i].selected = false
            }
        }
        val listDialog = object : MembersListDialog(
            this,
            mMembersDetailList,
            resources.getString(R.string.str_select_member)
        ){
            override fun onItemSelected(user: User, action: String) {
                if(action == Constants.SELECT){
                    if(!mBoardDetails.taskList[mTaskListPosition]
                        .cards[mCardPosition].assignedTo
                        .contains(user.id)){
                        mBoardDetails.taskList[mTaskListPosition]
                            .cards[mCardPosition].assignedTo.add(user.id)
                    }
                }
                else{
                    mBoardDetails.taskList[mTaskListPosition]
                        .cards[mCardPosition].assignedTo.remove(user.id)
                    for(i in mMembersDetailList.indices){
                        if(mMembersDetailList[i].id == user.id){
                            mMembersDetailList[i].selected = false
                        }
                    }
                }
                setupSelectedMemberList()
            }
        }
        listDialog.show()
    }

    @SuppressLint("CutPasteId")
    private fun setupSelectedMemberList(){
        val cardAssignedMemberList = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo
        val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()
        for(i in mMembersDetailList.indices){
            for(j in cardAssignedMemberList){
                if(mMembersDetailList[i].id == j){
                    val selectedMember = SelectedMembers(
                        mMembersDetailList[i].id,
                        mMembersDetailList[i].image
                    )
                    selectedMembersList.add(selectedMember)
                }
            }
        }
        if(selectedMembersList.size > 0){
            selectedMembersList.add(SelectedMembers("",""))
            findViewById<TextView>(R.id.tv_select_member).visibility = View.GONE
            findViewById<RecyclerView>(R.id.rv_selected_members_list).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.rv_selected_members_list).layoutManager = GridLayoutManager(
                this, 6
            )
            val adapter = CardMemberListItemsAdapter(
                this, selectedMembersList, true)
            findViewById<RecyclerView>(R.id.rv_selected_members_list).adapter = adapter
            adapter.setOnClickListener(
                object : CardMemberListItemsAdapter.OnClickListener {
                    override fun onClick() {
                        membersListDialog()
                    }
                }
            )
        }else{
            findViewById<TextView>(R.id.tv_select_member).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.rv_selected_members_list).visibility = View.GONE
        }
    }

    private fun showDataPicker(){
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val dpd = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener{
                _, year, monthOfYear, dayOfMonth ->
                val sDayOfMonth =
                    if (dayOfMonth<10){
                    "0$dayOfMonth"
                }
                else{
                    "0$dayOfMonth"
                }
                val sMonthYear =
                 if((monthOfYear + 1) < 10){
                     "0${monthOfYear+1}"
                 }else{
                     "${monthOfYear+1}"
                 }
                val selectedDate = "$sDayOfMonth/$sMonthYear/$year"
                findViewById<TextView>(R.id.tv_select_due_date).text = selectedDate
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.CANADA)
                val theDate = sdf.parse(selectedDate)
                mSelectedDueDateMilliSeconds = theDate!!.time
            },
            year,
            month,
            day
        )
        dpd.show()
    }

}
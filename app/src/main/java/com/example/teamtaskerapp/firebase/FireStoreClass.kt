package com.example.teamtaskerapp.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.teamtaskerapp.activity.CardDetailsActivity
import com.example.teamtaskerapp.activity.CreateBoardActivity
import com.example.teamtaskerapp.activity.MainActivity
import com.example.teamtaskerapp.activity.MembersActivity
import com.example.teamtaskerapp.activity.ProfileActivity
import com.example.teamtaskerapp.activity.SignInActivity
import com.example.teamtaskerapp.activity.SignUpActivity
import com.example.teamtaskerapp.activity.TaskListActivity
import com.example.teamtaskerapp.models.Board
import com.example.teamtaskerapp.models.User
import com.example.teamtaskerapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject

class FireStoreClass {
    private val mFireStore = FirebaseFirestore.getInstance()
    fun registeredUser(activity: SignUpActivity, userInfo: User){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId()).set(userInfo,
                SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }.addOnFailureListener{
                e ->
                Log.e(activity.javaClass.simpleName, "Error while registering the user",e)
            }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board){
        mFireStore.collection(Constants.BOARDS)
            .document().set(board,
                SetOptions.merge())
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName , "Board created successfully")
                Toast.makeText(activity, "Board created successfully",Toast.LENGTH_SHORT).show()
                activity.boardCreateSuccessfully()
            }.addOnFailureListener{
                    e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating user board",e)
            }
    }

    fun getBoardsList(activity: MainActivity){
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get()
            .addOnSuccessListener {
                document ->
                Log.i(activity.javaClass.simpleName, document.documents.toString())
                val boardList: ArrayList<Board> = ArrayList()
                for(i in document.documents){
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardList.add(board)
                }
                activity.populateBoardsListToUI(boardList)
            }.addOnFailureListener{ e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating or display board list",e)
            }
    }

    fun updateUserProfileData(activity: Activity,
                              userHashMap: HashMap<String, Any>){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap).addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Profile Data is updated successfully!")
                Toast.makeText(activity, "Profile updated successfully",Toast.LENGTH_SHORT).show()
               when(activity) {
                   is MainActivity -> {
                       activity.tokenUpdateSuccess()
                   }

                   is ProfileActivity -> {
                       activity.profileUpdateSuccess()
                   }
               }
            }.addOnFailureListener{
                e ->
                   when(activity) {
                       is MainActivity -> {
                           activity.hideProgressDialog()
                       }

                       is ProfileActivity -> {
                           activity.hideProgressDialog()
                       }
                   }
                Log.e(activity.javaClass.simpleName, "Error while updating the profile",e)
                Toast.makeText(activity, "Error while updating the profile", Toast.LENGTH_SHORT).show()
            }
    }

    fun loadUserData(activity: Activity, readBoardsList: Boolean = false){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId()).get()
            .addOnSuccessListener { document ->
                val loggedInUser =  document.toObject(User::class.java)!!
                when(activity){
                    is SignInActivity -> {
                        activity.signInSuccess(loggedInUser)
                    }
                    is MainActivity -> {
                        activity.updateNavigationUserDetails(loggedInUser, readBoardsList)
                    }
                    is ProfileActivity -> {
                      activity.setUserDataInUI(loggedInUser)
                    }
                }
            }.addOnFailureListener{
                    e ->
                when(activity){
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is ProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "Error while registering the user",e)
            }
    }

    fun getCurrentUserId(): String{
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserId = ""
        if(currentUser!=null){
            currentUserId = currentUser.uid
        }
        return currentUserId
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String){
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener {
                    document ->
                Log.i(activity.javaClass.simpleName, document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id
                activity.boardDetails(board)
            }.addOnFailureListener{ e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating or display board list",e)
            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board){
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList
        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName,"TaskList updated Successfully")
                if(activity is TaskListActivity){
                    activity.addUpdateTaskListSuccess()
                }else if(activity is CardDetailsActivity){
                    activity.addUpdateTaskListSuccess()
                }
            }.addOnFailureListener{
                e ->
                if(activity is TaskListActivity)
                activity.hideProgressDialog()
                else if(activity is CardDetailsActivity)
                  activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating or display board list",e)
            }
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>){
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener {
                document ->
                Log.e(activity.javaClass.simpleName, document.documents.toString())
                val userList: ArrayList<User> = ArrayList()
                for(i in document.documents){
                    val user = i.toObject(User::class.java)!!
                    userList.add(user)
                }
                if(activity is MembersActivity){
                    activity.setupMembersList(userList)
                }
                else if(activity is TaskListActivity){
                    activity.boardMembersDetailsList(userList)
                }

            }
            .addOnFailureListener{
                e ->
                if(activity is MembersActivity){
                    activity.hideProgressDialog()
                }
                else if(activity is TaskListActivity){
                    activity.hideProgressDialog()
                }
                Log.e(activity.javaClass.simpleName, "Error while getting assigned members list",e)
            }
    }

    fun getMemberDetails(activity: MembersActivity, email: String){
      mFireStore.collection(Constants.USERS)
          .whereEqualTo(Constants.EMAIL, email)
          .get()
          .addOnSuccessListener {
              document ->
              if(document.documents.size > 0){
                  val user = document.documents[0].toObject(User::class.java)!!
                  activity.memberDetails(user)
              }else{
                  activity.hideProgressDialog()
                  activity.showErrorSnackBar("No such member found")
              }
          }.addOnFailureListener{ e ->
              activity.hideProgressDialog()
             Log.e(activity.javaClass.simpleName, "Error while getting user details",e)
          }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User){
        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo
        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }.addOnFailureListener{
                e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating board",e)
            }
    }

}
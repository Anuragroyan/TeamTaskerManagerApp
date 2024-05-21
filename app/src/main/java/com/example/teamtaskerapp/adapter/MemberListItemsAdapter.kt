package com.example.teamtaskerapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teamtaskerapp.R
import com.example.teamtaskerapp.models.User
import com.example.teamtaskerapp.utils.Constants

open class MemberListItemsAdapter(
    private val context : Context,
    private val memberList : ArrayList<User>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var onClickListener: OnClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_member,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = memberList[position]
        if(holder is MyViewHolder){
            Glide
                .with(context)
                .load(model.image)
                .centerCrop()
                .placeholder(R.drawable.user)
                .into(holder.itemView.findViewById(R.id.iv_member_image))
            holder.itemView.findViewById<TextView>(R.id.tv_member_name).text = model.name
            holder.itemView.findViewById<TextView>(R.id.tv_member_email).text = model.email
            if(model.selected){
              holder.itemView.findViewById<ImageView>(R.id.iv_selected_member).visibility = View.VISIBLE
            }else{
                holder.itemView.findViewById<ImageView>(R.id.iv_selected_member).visibility = View.GONE
            }
            holder.itemView.setOnClickListener{
                if(onClickListener!=null){
                    if(model.selected){
                        onClickListener!!.onClick(position, model, Constants.UN_SELECT)
                    }
                    else{
                        onClickListener!!.onClick(position, model, Constants.SELECT)
                    }
                }
            }
        }
    }

    interface OnClickListener{
        fun onClick(position: Int, user: User, action: String)
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    override fun getItemCount(): Int {
        return memberList.size
    }

    class MyViewHolder(view: View): RecyclerView.ViewHolder(view)

}
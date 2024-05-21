package com.example.teamtaskerapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teamtaskerapp.R
import com.example.teamtaskerapp.models.Board

open class BoardItemAdapter(
    private val context: Context,
    private var list: ArrayList<Board>):
RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var onClickListener: OnClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(LayoutInflater
            .from(context)
            .inflate(R.layout.item_board,
                parent, false))
    }

    override fun getItemCount(): Int {
       return list.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if(holder is MyViewHolder){
            Glide
                .with(context)
                .load(model.image)
                .centerCrop()
                .placeholder(R.drawable.user)
                .into(holder.itemView.findViewById(R.id.iv_item_board_image))
            holder.itemView.findViewById<TextView>(R.id.tv_item_name).text = model.name
            holder.itemView.findViewById<TextView>(R.id.tv_item_createdBy).text = "CreatedBy: ${model.createdBy}"
            holder.itemView.findViewById<TextView>(R.id.tv_item_name).setOnClickListener{
                if(onClickListener!=null){
                    onClickListener!!.onClick(position,model)
                }
            }
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    interface OnClickListener{
        fun onClick(position: Int, model: Board)
    }

    private class MyViewHolder(view: View):RecyclerView.ViewHolder(view)

}

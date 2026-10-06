package com.bridge.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timeText: TextView = view.findViewById(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.text
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
        holder.timeText.text = time

        if (message.isUser) {
            holder.messageText.setBackgroundResource(R.drawable.bg_user_message)
            holder.messageText.setTextColor(0xFFFFFFFF.toInt())
        } else {
            holder.messageText.setBackgroundResource(R.drawable.bg_bot_message)
            holder.messageText.setTextColor(0xFF000000.toInt())
        }
    }

    override fun getItemCount() = messages.size
}

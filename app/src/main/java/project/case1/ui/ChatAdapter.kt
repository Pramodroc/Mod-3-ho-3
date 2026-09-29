package project.case1.ui

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import project.case1.R
import project.case1.data.ChatMessage

class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val layoutMessage: LinearLayout = itemView.findViewById(R.id.layoutMessage)

        fun bind(message: ChatMessage) {
            tvMessage.text = message.text

            val params = layoutMessage.layoutParams as LinearLayout.LayoutParams

            if (message.isUser) {
                layoutMessage.setBackgroundResource(R.drawable.bg_user_message)
                params.gravity = Gravity.END
                tvMessage.setTextColor(Color.WHITE)
            } else {
                layoutMessage.setBackgroundResource(R.drawable.bg_bot_message)
                params.gravity = Gravity.START
                tvMessage.setTextColor(Color.WHITE)
            }
            layoutMessage.layoutParams = params
        }
    }
}
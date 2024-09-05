package com.pos.hardware.connection.demo.ui.comm

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.pos.hardware.connection.demo.App
import com.pos.hardware.connection.demo.R
import com.pos.hardware.connection.demo.help.DateHelper

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dataSources: MutableList<ChatBean> = mutableListOf()

    fun setData(list: List<ChatBean>) {
        dataSources.clear()
        dataSources.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            FROM_USER_TEXT -> {
                val view = View.inflate(parent.context, R.layout.item_chat_from_user, null)
                FromUserViewHolder(view)
            }
            else -> {
                val view = View.inflate(parent.context, R.layout.item_chat_to_user, null)
                ToUserViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataSources[position]
        val viewType = getItemViewType(position)
        when (viewType) {
            FROM_USER_TEXT -> {
                val fromUserViewHolder = holder as FromUserViewHolder
                fromUserViewHolder.bind(item)
            }
            else -> {
                val toUserViewHolder = holder as ToUserViewHolder
                toUserViewHolder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = dataSources.size

    override fun getItemViewType(position: Int): Int {
        val item = dataSources[position]
        val toUser = if (App.server) {
            item.sender == "Server"
        } else {
            item.sender == "Client"
        }
        return if (toUser) {
            TO_USER_TEXT
        } else {
            FROM_USER_TEXT
        }
    }

    inner class FromUserViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        private val time: TextView = rootView.findViewById(R.id.time_text)
        private val name: TextView = rootView.findViewById(R.id.name_text)
        private val cardView: CardView = rootView.findViewById(R.id.card_view)
        private val content: TextView = rootView.findViewById(R.id.content_text)

        fun bind(item: ChatBean) {
            cardView.alpha = 0.6f
            content.text = item.content
            name.text = item.sender.substring(0, 1)
            time.text = DateHelper.getDateFormatString(item.time, "MM/dd HH:mm:ss")
        }
    }

    inner class ToUserViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        private val time: TextView = rootView.findViewById(R.id.time_text)
        private val name: TextView = rootView.findViewById(R.id.name_text)
        private val cardView: CardView = rootView.findViewById(R.id.card_view)
        private val content: TextView = rootView.findViewById(R.id.content_text)

        fun bind(item: ChatBean) {
            cardView.alpha = 1f
            content.text = item.content
            name.text = item.sender.substring(0, 1)
            time.text = DateHelper.getDateFormatString(item.time, "MM/dd HH:mm:ss")
        }
    }

    companion object {
        const val FROM_USER_TEXT = 1
        const val FROM_USER_IMAGE = 3
        const val FROM_USER_FILES = 3
        const val FROM_USER_VOICE = 4

        const val TO_USER_TEXT = 11
        const val TO_USER_IMAGE = 12
        const val TO_USER_FILES = 13
        const val TO_USER_VOICE = 14
    }

}
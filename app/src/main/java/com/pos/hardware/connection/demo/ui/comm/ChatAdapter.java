package com.pos.hardware.connection.demo.ui.comm;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;


import com.pos.hardware.connection.demo.App;
import com.pos.hardware.connection.demo.R;
import com.pos.hardware.connection.demo.help.DateHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * @author: Dadong
 * @date: 2024/11/21
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatBean> dataSources = new ArrayList<>();

    public void setData(List<ChatBean> list) {
        dataSources.clear();
        dataSources.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return dataSources.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatBean item = dataSources.get(position);
        boolean toUser = App.server ? "Server".equals(item.getSender()) : "Client".equals(item.getSender());
        return toUser ? TO_USER_TEXT : FROM_USER_TEXT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == FROM_USER_TEXT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_from_user, parent, false);
            return new FromUserViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_to_user, parent, false);
            return new ToUserViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatBean item = dataSources.get(position);
        int viewType = getItemViewType(position);
        if (viewType == FROM_USER_TEXT) {
            FromUserViewHolder fromUserViewHolder = (FromUserViewHolder) holder;
            fromUserViewHolder.bind(item);
        } else {
            ToUserViewHolder toUserViewHolder = (ToUserViewHolder) holder;
            toUserViewHolder.bind(item);
        }
    }

    public class FromUserViewHolder extends RecyclerView.ViewHolder {

        private TextView time;
        private TextView name;
        private CardView cardView;
        private TextView content;

        public FromUserViewHolder(View rootView) {
            super(rootView);
            time = rootView.findViewById(R.id.time_text);
            name = rootView.findViewById(R.id.name_text);
            cardView = rootView.findViewById(R.id.card_view);
            content = rootView.findViewById(R.id.content_text);
        }

        public void bind(ChatBean item) {
            cardView.setAlpha(0.6f);
            content.setText(item.getContent());
            name.setText(item.getSender().substring(0, 1));
            time.setText(DateHelper.getDateFormatString(item.getTime(), "MM/dd HH:mm:ss"));
        }
    }

    public class ToUserViewHolder extends RecyclerView.ViewHolder {

        private TextView time;
        private TextView name;
        private CardView cardView;
        private TextView content;

        public ToUserViewHolder(View rootView) {
            super(rootView);
            time = rootView.findViewById(R.id.time_text);
            name = rootView.findViewById(R.id.name_text);
            cardView = rootView.findViewById(R.id.card_view);
            content = rootView.findViewById(R.id.content_text);
        }

        public void bind(ChatBean item) {
            cardView.setAlpha(1f);
            content.setText(item.getContent());
            name.setText(item.getSender().substring(0, 1));
            time.setText(DateHelper.getDateFormatString(item.getTime(), "MM/dd HH:mm:ss"));
        }
    }

    public static final int FROM_USER_TEXT = 1;
    public static final int FROM_USER_IMAGE = 3;
    public static final int FROM_USER_FILES = 3;
    public static final int FROM_USER_VOICE = 4;

    public static final int TO_USER_TEXT = 11;
    public static final int TO_USER_IMAGE = 12;
    public static final int TO_USER_FILES = 13;
    public static final int TO_USER_VOICE = 14;
}

package com.pos.hardware.connection.demo.ui.comm;

/**
 * @author: Dadong
 * @date: 2024/11/21
 */
public class ChatBean {
    private String id;
    private long time;
    private String receiver;
    private String content;
    private String sender;

    public ChatBean(
            String receiver,
            String content,
            String sender) {
        this.receiver = receiver;
        this.content = content;
        this.sender = sender;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
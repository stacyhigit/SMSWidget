package com.stacyhi.smswidget;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Message {
    private final String TAG = "Message";
    private final String id;
    private final Long date;
    private final String content;
    private final List<Recipient> recipientList = new ArrayList<>();

    public Message(String id, Long date, String content) {
        this.id = id;
        this.date = date;
        this.content = content;
    }

    public void addRecipient(Recipient recipient) {
        this.recipientList.add(recipient);
    }

    public String getRecipientNumbers() {
        return recipientList.stream().map(Recipient::getNumber).collect(Collectors.joining(","));
    }

    public String getRecipientDisplayNames() {
        return recipientList.stream().map(Recipient::getHeader).collect(Collectors.joining(", "));
    }

    public String getHeader() {
        if (recipientList.size() == 1 ) {
            return recipientList.get(0).getHeader();
        } else {
            return "MMS";
        }
    }

    public String getId(){
        return id;
    }

    public Long getDate() {
        return date;
    }

    public String getContent() {
        if (content == null) {
            if (recipientList.size() > 1) {
                return getRecipientDisplayNames();
            } else {
                return "MMS";
            }
        }
        return content;
    }
}

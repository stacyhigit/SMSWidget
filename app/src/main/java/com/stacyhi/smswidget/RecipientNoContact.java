package com.stacyhi.smswidget;

public class RecipientNoContact extends Recipient {
    public static final String TAG = "Recipeint";

    public RecipientNoContact(String name, String nameType, String number) {
        super(name, nameType, number);
    }

    public String getHeader() {
        if (getNumber().equals("0")) {
            return "Error retrieving number";
        } else {
            return SMSDataProvider.formatNumber(getNumber());
        }
    }
}

package com.stacyhi.smswidget;

public class Recipient {
    private final String name;
    private final String nameType;
    private final String number;

    public Recipient(String name, String nameType, String number) {
        this.name = name;
        this.nameType = nameType;
        this.number = number;
    }

    public String getName() { return name; }
    public String getNumber() { return number; }

    public String getHeader() {
        if (nameType != null && !nameType.equals("")) {
            return name + " (" + nameType + ")";
        }
        return name;
    }
}

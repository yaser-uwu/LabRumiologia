package com.uteq.software.labrumiologia.network;

import java.util.List;

public class ChatRequest {
    public String question;
    public String equipment_class;

    public ChatRequest(String question, String equipmentClass) {
        this.question = question;
        this.equipment_class = equipmentClass;
    }
}

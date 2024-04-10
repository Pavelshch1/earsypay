package com.example.earsypay;

import com.google.gson.annotations.SerializedName;

public class ServerResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }
}
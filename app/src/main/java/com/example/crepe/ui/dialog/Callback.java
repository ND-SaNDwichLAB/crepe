package com.example.crepe.ui.dialog;
import java.io.Serializable;

public interface Callback extends Serializable {
    void onDataReceived(String query, String targetText);
}


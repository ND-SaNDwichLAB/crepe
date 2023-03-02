package com.example.crepe.ui.dialog;
import java.io.Serializable;

public interface GraphQueryCallback extends Serializable {
    void onDataReceived(String query);
}


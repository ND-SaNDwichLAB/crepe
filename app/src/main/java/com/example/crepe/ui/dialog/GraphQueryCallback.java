package com.example.crepe.ui.dialog;

import android.util.Log;

import java.io.Serializable;

public class GraphQueryCallback implements Callback, Serializable {
    @Override
    public void onDataReceived(String query) {
        Log.d("graphQueryCallback", "onDataReceived: " + query);
//            datafields.add(new Datafield("DatafieldID","1",query,"name",true));
    }
}
package com.example.crepe.network;

import com.example.crepe.database.Collector;
import com.example.crepe.database.Data;
import com.example.crepe.database.Datafield;
import com.example.crepe.database.User;

public interface FirebaseCallback<T> {
    void onResponse(T result);
    void onErrorResponse(Exception e);


}

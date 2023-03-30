package com.example.crepe.network;

import com.example.crepe.database.Collector;
import com.example.crepe.database.Data;
import com.example.crepe.database.Datafield;
import com.example.crepe.database.User;

public interface FirebaseCallback {
    void onCollectorResponse(Collector result);

    void onUserResponse(User result);

    void onDataResponse(Data result);

    void onDatafieldResponse(Datafield result);


}

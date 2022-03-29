package com.example.crepe.network;

import android.content.Context;
import android.media.MediaRouter;
import android.media.metrics.Event;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.crepe.database.Collector;
import com.google.gson.Gson;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class ServerCollectorCommunicationManager extends AppCompatActivity {
    private Context context;
    private RequestQueue queue;

    // Create some member variables for the ExecutorService
    // and for the Handler that will update the UI from the main thread
//    ExecutorService mExecutor = Executors.newSingleThreadExecutor();
//    Handler mHandler = new Handler(Looper.getMainLooper());

    public ServerCollectorCommunicationManager(Context context){
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
    }

    public void uploadJsonToServer(Collector collector){
//        Gson gson = new Gson();
//        String collectorJson = gson.toJson(collector);
//        HttpURLConnection httpURLConnection = null;
//        try {
//            httpURLConnection = (HttpURLConnection) new URL(params[0]).openConnection();
//            httpURLConnection.setRequestMethod("POST");
//
//            httpURLConnection.setDoOutput(true);
//
//            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
//            wr.writeBytes("PostData=" + params[1]);
//            wr.flush();
//            wr.close();
//
//            InputStream in = httpURLConnection.getInputStream();
//            InputStreamReader inputStreamReader = new InputStreamReader(in);
//
//            int inputStreamData = inputStreamReader.read();
//            while (inputStreamData != -1) {
//                char current = (char) inputStreamData;
//                inputStreamData = inputStreamReader.read();
//                data += current;
//
//        } catch (ProtocolException e) {
//            e.printStackTrace();
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


    }

    public void downloadJsonFromServer(final VolleyCallback callback, String url){
        Gson g = new Gson();
        String[] collectorInfo = new String[1];
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                collectorInfo[0] = response.toString();
                Collector collector = g.fromJson(collectorInfo[0], Collector.class);
                callback.onSuccess(collector);

                Log.e("GET Request", "Received success");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                collectorInfo[0] = error.toString();
                Log.e("GET Request", error.toString());
            }
        });

        queue.add(jsonObjectRequest);




    }

}

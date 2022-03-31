package com.example.crepe.network;

import android.content.Context;
import android.util.Log;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.crepe.database.Collector;
import com.google.gson.Gson;


import java.util.HashMap;
import java.util.Map;

import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerCollectorCommunicationManager extends AppCompatActivity {
    private Context context;
    private RequestQueue queue;

    public ServerCollectorCommunicationManager(Context context){
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
    }




    public void uploadJsonToServer(Collector collector) throws JSONException {

        JSONObject params = new JSONObject();
        params.put("collectorId", collector.getCollectorId());
        params.put("creatorUserId", collector.getCreatorUserId() == null ? "1" : collector.getCreatorUserId());
        params.put("appName", collector.getAppName());
        params.put("description", collector.getDescription());
        params.put("mode", collector.getMode());
        params.put("targetServerIp", collector.getTargetServerIp() == null ? "1" : collector.getTargetServerIp());
        params.put("collectorStartTime", collector.getCollectorStartTime());
        params.put("collectorEndTime", collector.getCollectorEndTime());
        params.put("collectorGraphQuery", collector.getCollectorGraphQuery());
        params.put("collectorAppDataFields", collector.getCollectorAppDataFields());
        params.put("collectorStatus", collector.getCollectorStatus());

        String url = "http://35.222.12.92:8000/";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("POST Request", "Sent success");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("POST Request", error.toString());
            }

        }){
            @Override
            public Map<String, String> getHeaders()  {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(jsonObjectRequest);

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

package edu.nd.crepe.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import edu.nd.crepe.database.Collector;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerCollectorCommunicationManager {
    private static final String TAG = "ServerCollectorComm";

    private Context context;
    private RequestQueue queue;

    public ServerCollectorCommunicationManager(Context context){
        // the queue lives as long as this manager, do not keep an activity reachable through it
        this.context = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        this.queue = Volley.newRequestQueue(this.context);
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
        params.put("collectorStatus", collector.getCollectorStatus());

        String url = "http://35.222.12.92:8000/";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(TAG, "POST Request: sent successfully");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "POST Request failed: " + error);
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
        final Gson g = new Gson();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Collector collector;
                try {
                    collector = g.fromJson(response.toString(), Collector.class);
                } catch (JsonParseException e) {
                    Log.e(TAG, "GET Request: could not parse the collector", e);
                    callback.onError(e);
                    return;
                }
                if (collector == null) {
                    callback.onError(new Exception("The server returned an empty collector"));
                    return;
                }
                Log.i(TAG, "GET Request: received successfully");
                callback.onSuccess(collector);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "GET Request failed: " + error);
                callback.onError(error);
            }
        });

        queue.add(jsonObjectRequest);

    }

}

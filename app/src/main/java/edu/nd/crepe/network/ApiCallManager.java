package edu.nd.crepe.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.nd.crepe.BuildConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiCallManager {

    private Context context;
    private OkHttpClient httpClient;
    private ExecutorService executorService;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;


    public ApiCallManager(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void getResponse(String prompt, ApiCallback callback) {
        executorService.execute(() -> {
            RequestBody body = RequestBody.create(JSON, buildOpenAIJsonBody(prompt));
            Request request = new Request.Builder()
                    .url(OPENAI_API_URL)
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.code() == 401) {
                    handleError("Authentication error: Invalid API key", callback);
                    return;
                }

                if (response.code() == 429) {
                    handleError("Rate limit exceeded", callback);
                    return;
                }

                if (!response.isSuccessful()) {
                    handleError("Unexpected response code: " + response.code(), callback);
                    return;
                }

                JsonObject jsonResponse = parseResponseToJson(response);
                String content = extractContentFromOpenAIResponse(jsonResponse);
                Log.i("ApiCallManager", "Response received from OpenAI");
                callback.onResponse(content);
            } catch (IOException e) {
                handleError("API call failed: " + e.getMessage(), callback);
            }
        });
    }

    private void handleError(String errorMessage, ApiCallback callback) {
        Log.e("ApiCallManager", errorMessage);
        callback.onErrorResponse(new Exception(errorMessage));
    }

    private String buildOpenAIJsonBody(String prompt) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", "gpt-3.5-turbo"); // You can change the model as needed

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);

        jsonBody.add("messages", messages);
        return jsonBody.toString();
    }

    private JsonObject parseResponseToJson(Response response) throws IOException {
        String responseBody = response.body().string();
        Gson gson = new Gson();
        return gson.fromJson(responseBody, JsonObject.class);
    }

    private String extractContentFromOpenAIResponse(JsonObject jsonResponse) {
        try {
            return jsonResponse
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
        } catch (Exception e) {
            Log.e("ApiCallManager", "Error parsing OpenAI response: " + e.getMessage());
            return "Error parsing response";
        }
    }

    public interface ApiCallback {
        void onResponse(String response);
        void onErrorResponse(Exception e);
    }
}
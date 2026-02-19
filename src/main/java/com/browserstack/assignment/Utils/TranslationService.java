package com.browserstack.assignment.Utils;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

public class TranslationService {
    private static final String API_URL = "https://google-translate1.p.rapidapi.com/language/translate/v2";
    private final OkHttpClient client;
    private final Gson gson;
    private final String apiKey;

    public TranslationService(String apiKey) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.apiKey = apiKey;
    }

    public String translate(String text, String targetLang) {
        if (apiKey == null || apiKey.isEmpty()) {
            return translateFree(text, targetLang);
        }

        try {
            // RapidAPI / Google Translate Implementation
            RequestBody body = new FormBody.Builder()
                    .add("q", text)
                    .add("target", targetLang)
                    .add("source", "es") // Assuming source is Spanish
                    .build();

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("content-type", "application/x-www-form-urlencoded")
                    .addHeader("Accept-Encoding", "application/gzip")
                    .addHeader("X-RapidAPI-Key", apiKey)
                    .addHeader("X-RapidAPI-Host", "google-translate1.p.rapidapi.com")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    if (json.has("data")) {
                        JsonObject data = json.getAsJsonObject("data");
                        if (data.has("translations")) {
                            JsonArray translations = data.getAsJsonArray("translations");
                            if (translations.size() > 0) {
                                return translations.get(0).getAsJsonObject().get("translatedText").getAsString();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("RapidAPI Translation failed. Falling back to free endpoint. Error: " + e.getMessage());
        }

        // Fallback if API fails
        return translateFree(text, targetLang);
    }

    /**
     * Uses the unofficial Google Translate web endpoint (Free).
     * URL:
     * https://translate.googleapis.com/translate_a/single?client=gtx&sl=es&tl=en&dt=t&q=...
     */
    private String translateFree(String text, String targetLang) {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://translate.googleapis.com/translate_a/single")
                    .newBuilder();
            urlBuilder.addQueryParameter("client", "gtx");
            urlBuilder.addQueryParameter("sl", "es"); // Source Language: Spanish
            urlBuilder.addQueryParameter("tl", targetLang); // Target Language
            urlBuilder.addQueryParameter("dt", "t");
            urlBuilder.addQueryParameter("q", text);

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    // Response is a JSON Array: [[["Translated Text","Original Text",...],...],...]
                    String responseBody = response.body().string();
                    JsonArray jsonArray = gson.fromJson(responseBody, JsonArray.class);

                    if (jsonArray.size() > 0) {
                        JsonArray firstBlock = jsonArray.get(0).getAsJsonArray();
                        if (firstBlock.size() > 0) {
                            return firstBlock.get(0).getAsJsonArray().get(0).getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Free Translation failed: " + e.getMessage());
        }
        return "[Translation Failed] " + text;
    }
}

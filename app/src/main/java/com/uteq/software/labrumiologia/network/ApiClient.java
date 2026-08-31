package com.uteq.software.labrumiologia.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.uteq.software.labrumiologia.BuildConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Cliente HTTP del asistente. Prueba varias URLs (LAN, USB, emulador). */
public final class ApiClient {
    private static final String TAG = "LabRag";
    private static volatile String workingBase;
    private static OkHttpClient http;
    private static String cachedBase;
    private static RagApi cachedApi;

    private ApiClient() {}

    public static void chat(ChatRequest request, Callback<ChatResponse> callback) {
        tryAt(bases(), 0, request, callback);
    }

    private static List<String> bases() {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (workingBase != null) urls.add(workingBase);
        for (String raw : new String[]{
                BuildConfig.RAG_BASE_URL,
                "http://127.0.0.1:8000/",
                "http://10.0.2.2:8000/"
        }) {
            if (raw == null || raw.isBlank()) continue;
            String u = raw.trim();
            urls.add(u.endsWith("/") ? u : u + "/");
        }
        return new ArrayList<>(urls);
    }

    private static RagApi api(String base) {
        if (http == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BASIC);
            http = new OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(log)
                    .build();
        }
        if (cachedApi != null && base.equals(cachedBase)) return cachedApi;
        cachedBase = base;
        cachedApi = new Retrofit.Builder()
                .baseUrl(base)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RagApi.class);
        return cachedApi;
    }

    private static void tryAt(List<String> urls, int i, ChatRequest request, Callback<ChatResponse> cb) {
        if (i >= urls.size()) {
            String last = urls.isEmpty() ? "http://127.0.0.1:8000/" : urls.get(0);
            cb.onFailure(api(last).chat(request), new RuntimeException(
                    "No se alcanzó el backend. Deje el servidor en el PC, mismo Wi-Fi, e instale de nuevo la app."));
            return;
        }
        String base = urls.get(i);
        Log.i(TAG, "POST chat → " + base);
        api(base).chat(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    workingBase = base;
                    cb.onResponse(call, response);
                } else {
                    tryAt(urls, i + 1, request, cb);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                Log.w(TAG, "falló " + base + ": " + t.getMessage());
                workingBase = null;
                tryAt(urls, i + 1, request, cb);
            }
        });
    }
}

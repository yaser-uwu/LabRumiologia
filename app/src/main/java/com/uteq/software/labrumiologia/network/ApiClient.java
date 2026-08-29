package com.uteq.software.labrumiologia.network;

import com.uteq.software.labrumiologia.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static RagApi api;

    private ApiClient() {}

    public static synchronized RagApi get() {
        if (api == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl(BuildConfig.RAG_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(RagApi.class);
        }
        return api;
    }
}

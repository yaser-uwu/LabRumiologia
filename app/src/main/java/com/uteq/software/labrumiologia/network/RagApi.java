package com.uteq.software.labrumiologia.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface RagApi {
    @POST("chat")
    Call<ChatResponse> chat(@Body ChatRequest request);

    @GET("health")
    Call<HealthResponse> health();
}

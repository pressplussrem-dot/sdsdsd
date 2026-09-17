package com.smartcalc.ai.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SolverApi {

    @POST("api/solve")
    suspend fun solve(@Body body: SolveRequestDto): Response<SolveResponseDto>

    /** Simple reachability probe. Only the HTTP status is used. */
    @GET("api/health")
    suspend fun health(): Response<Unit>
}

package com.twinhearts.app.feature.help.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Customer care endpoints of the existing Express server (routes/support.js), used as they are:
 * no new backend route is needed. The server has no tokens; the ids travel in the body (R-64).
 */
interface HelpApi {
    @GET("api/support/faqs")
    suspend fun faqs(): FaqsResponse

    @POST("api/support/ticket")
    suspend fun openTicket(@Body body: TicketRequest): TicketResponse

    @GET("api/support/ticket/{ticketId}")
    suspend fun ticket(@Path("ticketId") ticketId: String): TicketResponse

    @POST("api/support/ticket/{ticketId}/message")
    suspend fun sendMessage(
        @Path("ticketId") ticketId: String,
        @Body body: SendMessageRequest,
    ): SendMessageResponse

    @POST("api/support/ticket/{ticketId}/switch-mode")
    suspend fun switchMode(
        @Path("ticketId") ticketId: String,
        @Body body: SwitchModeRequest,
    ): TicketResponse
}

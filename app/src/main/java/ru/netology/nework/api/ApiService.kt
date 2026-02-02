package ru.netology.nework.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.PushToken
import ru.netology.nework.dto.Token
import ru.netology.nework.dto.User

interface ApiService {

    // ========== POSTS ==========
    @GET("api/posts")
    suspend fun getAllPosts(): Response<List<Post>>

    @GET("api/posts/latest")
    suspend fun getLatestPosts(@Query("count") count: Int): Response<List<Post>>

    @GET("api/posts/{id}/newer")
    suspend fun getPostsNewerThan(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("api/posts/{id}/before")
    suspend fun getPostsBefore(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("api/posts/{id}")
    suspend fun getPostById(@Path("id") id: Long): Response<Post>

    @POST("api/posts")
    suspend fun savePost(@Body post: Post): Response<Post>

    @DELETE("api/posts/{id}")
    suspend fun deletePost(@Path("id") id: Long): Response<Unit>

    @POST("api/posts/{id}/likes")
    suspend fun likePost(@Path("id") id: Long): Response<Post>

    @DELETE("api/posts/{id}/likes")
    suspend fun dislikePost(@Path("id") id: Long): Response<Post>

    // ========== EVENTS ==========
    @GET("api/events")
    suspend fun getAllEvents(): Response<List<Event>>

    @GET("api/events/latest")
    suspend fun getLatestEvents(@Query("count") count: Int): Response<List<Event>>

    @GET("api/events/{id}/newer")
    suspend fun getEventsNewerThan(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Event>>

    @GET("api/events/{id}/before")
    suspend fun getEventsBefore(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Event>>

    @GET("api/events/{id}")
    suspend fun getEventById(@Path("id") id: Long): Response<Event>

    @POST("api/events")
    suspend fun saveEvent(@Body event: Event): Response<Event>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Response<Unit>

    @POST("api/events/{id}/likes")
    suspend fun likeEvent(@Path("id") id: Long): Response<Event>

    @DELETE("api/events/{id}/likes")
    suspend fun dislikeEvent(@Path("id") id: Long): Response<Event>

    @POST("api/events/{id}/participants")
    suspend fun participateEvent(@Path("id") id: Long): Response<Event>

    @DELETE("api/events/{id}/participants")
    suspend fun cancelParticipation(@Path("id") id: Long): Response<Event>

    // ========== USERS ==========
    @GET("api/users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<User>

    @FormUrlEncoded
    @POST("api/users/authentication")
    suspend fun authenticateUser(
        @Field("login") login: String,
        @Field("pass") pass: String
    ): Response<Token>
    @Multipart
    @POST("api/users/registration")
    suspend fun registerUser(
        @Part("login") login: RequestBody,
        @Part("pass") pass: RequestBody,
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part? = null
    ): Response<Token>

    @POST("api/users/push-tokens")
    suspend fun savePushToken(@Body token: PushToken): Response<Unit>

    // ========== JOBS ==========
    @GET("api/my/jobs")
    suspend fun getMyJobs(): Response<List<Job>>

    @GET("api/{userId}/jobs")
    suspend fun getUserJobs(@Path("userId") userId: Long): Response<List<Job>>

    @POST("api/my/jobs")
    suspend fun saveJob(@Body job: Job): Response<Job>

    @DELETE("api/my/jobs/{id}")
    suspend fun deleteJob(@Path("id") id: Long): Response<Unit>

    // ========== MEDIA ==========
    @Multipart
    @POST("api/media")
    suspend fun upload(@Part file: MultipartBody.Part): Response<Media>

    // ========== WALL ==========
    @GET("api/{authorId}/wall")
    suspend fun getUserWall(@Path("authorId") authorId: Long): Response<List<Post>>
}
package project.case1.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import project.case1.data.ChatResponse

interface ApiService {

    @Multipart
    @POST("upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @POST("chat")
    suspend fun askQuestion(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @GET("documents")
    suspend fun getDocuments(): Response<DocumentsResponse>

    @POST("clear")
    suspend fun clearDocuments(): Response<ClearResponse>
}

// Response Data Classes
data class UploadResponse(
    val message: String,
    val filename: String
)

data class DocumentsResponse(
    val documents: List<String>
)

data class ClearResponse(
    val message: String
)
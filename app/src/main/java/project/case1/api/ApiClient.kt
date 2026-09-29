package project.case1.api

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import project.case1.data.ChatResponse
import retrofit2.Response
import java.io.File
import java.util.concurrent.TimeUnit

class ApiClient {
    companion object {
        // Default URL for Android Emulator. Change to http://192.168.1.189:5000/ if using physical device.
        const val BASE_URL = "http://10.0.2.2:5000/"
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    suspend fun uploadDocument(file: File): Response<UploadResponse> {
        val requestBody = file.asRequestBody("application/pdf".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return apiService.uploadDocument(body)
    }

    suspend fun askQuestion(request: ChatRequest): Response<ChatResponse> {
        return apiService.askQuestion(request)
    }

    suspend fun getDocuments(): Response<DocumentsResponse> {
        return apiService.getDocuments()
    }

    suspend fun clearDocuments(): Response<ClearResponse> {
        return apiService.clearDocuments()
    }
}

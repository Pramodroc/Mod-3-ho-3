package project.case1

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import project.case1.api.ApiClient
import project.case1.api.ChatRequest
import project.case1.data.ChatMessage
import project.case1.data.ChatResponse
import project.case1.ui.ChatAdapter
import project.case1.utils.FileUtils
import project.case1.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()
    private val apiClient = ApiClient()

    private val pickDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { handleDocumentSelection(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickDocumentLauncher.launch("*/*")
        } else {
            Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()

        // Add welcome message
        addChatMessage(ChatMessage("Welcome! Upload a document (PDF/TXT) and ask questions to compare RAG document answers with Direct LLM answers.", isUser = false))
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnUpload.setOnClickListener {
            requestDocumentPermission()
        }

        binding.btnSend.setOnClickListener {
            val question = binding.etQuestion.text.toString().trim()
            if (question.isNotEmpty()) {
                askQuestion(question)
            } else {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClear.setOnClickListener {
            clearChat()
        }
    }

    private fun requestDocumentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickDocumentLauncher.launch("*/*")
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pickDocumentLauncher.launch("*/*")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun handleDocumentSelection(uri: Uri) {
        try {
            val file = FileUtils.getFileFromUri(this, uri)
            if (file != null) {
                uploadDocument(file)
            } else {
                Toast.makeText(this, "Failed to get file from URI", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling document", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadDocument(file: File) {
        addChatMessage(ChatMessage("Uploading document: ${file.name}...", isUser = false))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiClient.uploadDocument(file)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && (response.body()?.message != null)) {
                        val msg = response.body()?.message ?: "Document uploaded successfully"
                        addChatMessage(ChatMessage("✅ $msg", isUser = false))
                        addChatMessage(ChatMessage("You can now ask questions about this document.", isUser = false))
                        Toast.makeText(
                            this@MainActivity,
                            "Document uploaded successfully",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        val errBody = response.errorBody()?.string()
                        val errorMsg = if (!errBody.isNullOrBlank()) errBody else "Failed to upload document"
                        addChatMessage(ChatMessage("❌ Error: $errorMsg", isUser = false))
                        Toast.makeText(
                            this@MainActivity,
                            "Upload failed",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addChatMessage(ChatMessage("❌ Connection Error: ${e.message}\nEnsure backend server is running at ${ApiClient.BASE_URL}", isUser = false))
                    Toast.makeText(
                        this@MainActivity,
                        "Connection Error",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun askQuestion(question: String) {
        addChatMessage(ChatMessage(question, isUser = true))
        binding.etQuestion.text?.clear()
        binding.btnSend.isEnabled = false

        addChatMessage(ChatMessage("🤔 Thinking (Comparing RAG vs Direct LLM)...", isUser = false))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiClient.askQuestion(ChatRequest(question))
                withContext(Dispatchers.Main) {
                    // Remove "Thinking..." placeholder message
                    if ((chatMessages.lastOrNull()?.isUser == false) &&
                        (chatMessages.lastOrNull()?.text?.contains("Thinking") == true)) {
                        chatMessages.removeAt(chatMessages.lastIndex)
                        chatAdapter.notifyItemRemoved(chatMessages.size)
                    }

                    if (response.isSuccessful && (response.body() != null)) {
                        val answer = response.body()!!.getFormattedText()
                        addChatMessage(ChatMessage(answer, isUser = false))
                    } else {
                        val errBody = response.errorBody()?.string()
                        val errorMsg = if (!errBody.isNullOrBlank()) errBody else "Unable to get response from server."
                        addChatMessage(ChatMessage("❌ Server Error: $errorMsg", isUser = false))
                    }
                    binding.btnSend.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Remove "Thinking..." placeholder message
                    if ((chatMessages.lastOrNull()?.isUser == false) &&
                        (chatMessages.lastOrNull()?.text?.contains("Thinking") == true)) {
                        chatMessages.removeAt(chatMessages.lastIndex)
                        chatAdapter.notifyItemRemoved(chatMessages.size)
                    }

                    addChatMessage(ChatMessage("❌ Connection Error: ${e.message}\nMake sure backend is running at ${ApiClient.BASE_URL}", isUser = false))
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }

    private fun addChatMessage(message: ChatMessage) {
        chatMessages.add(message)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        binding.rvChat.scrollToPosition(chatMessages.size - 1)
    }

    private fun clearChat() {
        val size = chatMessages.size
        chatMessages.clear()
        chatAdapter.notifyItemRangeRemoved(0, size)
        addChatMessage(ChatMessage("Chat cleared. Resetting backend documents...", isUser = false))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiClient.clearDocuments()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error clearing remote documents", e)
            }
        }
    }
}

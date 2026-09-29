package project.case1.data

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val response: String? = null,
    @SerializedName("rag_response") val ragResponse: String? = null,
    @SerializedName("direct_response") val directResponse: String? = null,
    @SerializedName("comparison_summary") val comparisonSummary: String? = null,
    val sources: List<String>? = null
) {
    fun getFormattedText(): String {
        if (!ragResponse.isNullOrBlank() && !directResponse.isNullOrBlank()) {
            val sb = StringBuilder()
            sb.append("🔍 RAG Response (With Document Context):\n")
            sb.append(ragResponse.trim())
            sb.append("\n\n💬 Direct LLM Response (Without Context):\n")
            sb.append(directResponse.trim())

            if (!comparisonSummary.isNullOrBlank()) {
                sb.append("\n\n📊 Key Difference:\n")
                sb.append(comparisonSummary.trim())
            }

            if (!sources.isNullOrEmpty()) {
                sb.append("\n\n📚 Sources: ")
                sb.append(sources.joinToString(", "))
            }
            return sb.toString()
        }
        
        if (!response.isNullOrBlank()) {
            var result = response
            if (!sources.isNullOrEmpty()) {
                result += "\n\n📚 Sources: " + sources.joinToString(", ")
            }
            return result
        }

        return "No response generated."
    }
}
import com.example.data.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val apiKey = System.getenv("GEMINI_API_KEY") ?: "NO_KEY"
    val req = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = "Hello!"))))
    )
    try {
        val res = RetrofitClient.service.generateContent(apiKey, req)
        println(res)
    } catch (e: Exception) {
        println("ERROR: ${e.message}")
        e.printStackTrace()
    }
}

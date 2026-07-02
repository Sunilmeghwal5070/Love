package com.example.ui

import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ChatRepository
import com.example.data.Content
import com.example.data.GenerateContentRequest
import com.example.data.Part
import com.example.data.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.asSharedFlow

class LoveViewModel(
    private val repository: ChatRepository,
    private val tts: TextToSpeech?
) : ViewModel() {

    val chatHistory = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _showGlobalPopup = MutableStateFlow(false)
    val showGlobalPopup: StateFlow<Boolean> = _showGlobalPopup

    private val _actionExecuting = MutableStateFlow<String?>(null)
    val actionExecuting: StateFlow<String?> = _actionExecuting

    private val _actionTrigger = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val actionTrigger = _actionTrigger.asSharedFlow()

    fun toggleGlobalPopup(show: Boolean) {
        _showGlobalPopup.value = show
    }

    fun clearAction() {
        _actionExecuting.value = null
    }

    fun startListening() {
        _isListening.value = true
    }

    fun stopListening(query: String? = null) {
        _isListening.value = false
        if (!query.isNullOrEmpty()) {
            processUserQuery(query)
        }
    }

    fun processUserQuery(query: String) {
        val fillers = listOf("Ek second boss...", "Abhi dekhta hoon boss...", "Ji boss...", "Processing...")
        tts?.speak(fillers.random(), TextToSpeech.QUEUE_FLUSH, null, "love_processing")
        
        viewModelScope.launch {
            repository.insertMessage("User", query)
            _isProcessing.value = true
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "placeholder") {
                    val msg = "Please set your Gemini API Key in the AI Studio Secrets panel."
                    repository.insertMessage("System", msg)
                    tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "love_api_error")
                    _isProcessing.value = false
                    return@launch
                }
                
                // Add system instructions to give it Jarvis persona
                val systemInstruction = Content(
                    parts = listOf(Part(text = "You are a personal AI Voice Assistant named Love, similar to Jarvis. The user likes to be called 'Boss'. Keep responses EXTREMELY short and concise (max 1 or 2 sentences), so it's fast to read aloud. If the user speaks in Hindi, reply in Hinglish so standard TTS can read it. If the user asks to open an app (e.g., WhatsApp, YouTube, Camera) or perform an action, you MUST output a JSON block at the very end of your response exactly like this: `{\"action\": \"open_app\", \"target\": \"AppName\"}`. Before the JSON block, you MUST say something acknowledging the action like 'Yes Boss, opening AppName.' Do NOT include markdown blocks around the JSON."))
                )

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = query)))),
                    systemInstruction = systemInstruction
                )
                
                val response = RetrofitClient.service.generateContent(apiKey, request)
                var responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Sorry, I didn't catch that."
                
                var actionTarget: String? = null
                
                // Parse potential JSON block at the end
                val jsonStartIndex = responseText.lastIndexOf("{")
                val jsonEndIndex = responseText.lastIndexOf("}")
                if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
                    try {
                        val jsonString = responseText.substring(jsonStartIndex, jsonEndIndex + 1)
                        val jsonObject = org.json.JSONObject(jsonString)
                        if (jsonObject.has("action") && jsonObject.getString("action") == "open_app") {
                            actionTarget = jsonObject.optString("target")
                            // Remove JSON from the spoken response
                            var cleanText = responseText.substring(0, jsonStartIndex).trim()
                            if (cleanText.endsWith("```json")) {
                                cleanText = cleanText.substring(0, cleanText.length - 7).trim()
                            } else if (cleanText.endsWith("```")) {
                                cleanText = cleanText.substring(0, cleanText.length - 3).trim()
                            }
                            responseText = cleanText
                        }
                    } catch (e: Exception) {
                        // Not a valid JSON, ignore
                    }
                }
                    
                repository.insertMessage("Love", responseText)
                tts?.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, "love_response")
                
                if (actionTarget != null) {
                    _actionExecuting.value = actionTarget
                    _actionTrigger.emit(actionTarget)
                }
            } catch (e: Exception) {
                repository.insertMessage("System", "Error communicating with Gemini: ${e.message}")
                tts?.speak("I'm sorry, I encountered an error connecting to my servers.", TextToSpeech.QUEUE_FLUSH, null, "love_error")
            } finally {
                _isProcessing.value = false
            }
        }
    }
}

package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Mic
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.ui.LoveViewModel
import com.example.ui.LoveViewModelFactory
import com.example.ui.navigation.HistoryRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.PermissionsRoute
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
  private var tts: TextToSpeech? = null
  private lateinit var viewModel: LoveViewModel
  private var speechRecognizer: android.speech.SpeechRecognizer? = null

  private fun startInternalListening(isHotword: Boolean) {
      if (speechRecognizer == null) return
      if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
          val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
              putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
              putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
              if (isHotword) {
                  putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
              } else {
                  putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                  putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                  putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000L)
              }
          }
          try {
              speechRecognizer?.startListening(intent)
          } catch (e: Exception) {
              e.printStackTrace()
          }
      }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    tts = TextToSpeech(this, this)
    
    // Initialize Speech Recognizer
    if (android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val isActiveMode = viewModel.isListening.value
                val isPopupOpen = viewModel.showGlobalPopup.value
                
                if (isActiveMode) {
                    if (error != android.speech.SpeechRecognizer.ERROR_CLIENT && error != android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
                        tts?.speak("I didn't quite catch that, boss.", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                    viewModel.stopListening(null)
                } else if (!isPopupOpen) {
                    // Hotword mode restart
                    if (error != android.speech.SpeechRecognizer.ERROR_CLIENT) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            startInternalListening(isHotword = true)
                        }, 500)
                    }
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.lowercase() ?: ""
                val isActiveMode = viewModel.isListening.value
                val isPopupOpen = viewModel.showGlobalPopup.value

                if (isActiveMode) {
                    if (text.isNotEmpty()) {
                        viewModel.stopListening(text)
                    } else {
                        tts?.speak("I didn't quite catch that, boss.", TextToSpeech.QUEUE_FLUSH, null, null)
                        viewModel.stopListening(null)
                    }
                } else if (!isPopupOpen) {
                    // Hotword mode
                    if (text.contains("hello love") || text.contains("hello lab") || text.contains("hi love") || text.contains("hey love")) {
                        val command = text.replace(Regex("(hello|hey|hi) l(ove|ab)"), "").trim()
                        viewModel.toggleGlobalPopup(true)
                        if (command.isNotEmpty()) {
                            viewModel.processUserQuery(command)
                        } else {
                            tts?.speak("Yes Boss?", TextToSpeech.QUEUE_FLUSH, null, "hotword_reply")
                            viewModel.startListening()
                        }
                    } else {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            startInternalListening(isHotword = true)
                        }, 500)
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    val database = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java, "love-database"
    ).build()
    val repository = ChatRepository(database.chatDao())
    val factory = LoveViewModelFactory(repository, tts)
    
    viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[LoveViewModel::class.java]
    
    handleAssistantIntent(intent)

    setContent {
      MyApplicationTheme {
        val permissionsToRequest = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.SEND_SMS
        )
        
        val permissionsState = androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(
                permissionsToRequest.all { 
                    androidx.core.content.ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
                }
            )
        }

        val multiplePermissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissionsState.value = permissionsToRequest.all {
                androidx.core.content.ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
            }
        }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (!permissionsState.value) {
                multiplePermissionsLauncher.launch(permissionsToRequest)
            }
        }
        
        if (!permissionsState.value) {
            // Show a blocked screen until permissions are granted
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    androidx.compose.material3.Text("Permissions required to use the app.", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(onClick = { multiplePermissionsLauncher.launch(permissionsToRequest) }) {
                        androidx.compose.material3.Text("Grant Permissions")
                    }
                }
            }
            return@MyApplicationTheme
        }

        val navController = rememberNavController()
        val showGlobalPopup by viewModel.showGlobalPopup.collectAsStateWithLifecycle()
        val isListening by viewModel.isListening.collectAsStateWithLifecycle()
        val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
        val actionExecuting by viewModel.actionExecuting.collectAsStateWithLifecycle()
        
        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // If it was just granted and we are supposed to be listening, we could start here, 
                // but the user might need to press the mic again. 
                // Let's just let them press it again for simplicity, or we can toggle it off and on.
                viewModel.stopListening(null)
                android.widget.Toast.makeText(this@MainActivity, "Tap mic again to speak", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                viewModel.stopListening(null)
            }
        }

        androidx.compose.runtime.LaunchedEffect(isListening, showGlobalPopup, permissionsState.value) {
            if (permissionsState.value) {
                if (isListening) {
                    startInternalListening(isHotword = false)
                } else if (!showGlobalPopup) {
                    startInternalListening(isHotword = true)
                } else {
                    speechRecognizer?.stopListening()
                }
            }
        }
        
        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewModel.actionTrigger.collect { targetApp ->
                // Basic implementation to launch an app by name
                try {
                    val pm = packageManager
                    val packages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                    var launched = false
                    for (packageInfo in packages) {
                        val appName = pm.getApplicationLabel(packageInfo).toString()
                        if (appName.equals(targetApp, ignoreCase = true) || appName.contains(targetApp, ignoreCase = true)) {
                            val launchIntent = pm.getLaunchIntentForPackage(packageInfo.packageName)
                            if (launchIntent != null) {
                                startActivity(launchIntent)
                                launched = true
                                break
                            }
                        }
                    }
                    if (!launched) {
                        // Fallback: search play store
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("market://search?q=$targetApp")
                        }
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    viewModel.clearAction()
                    viewModel.toggleGlobalPopup(false)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = HomeRoute) {
                composable<HomeRoute> {
                    val history by viewModel.chatHistory.collectAsStateWithLifecycle()
                    HomeScreen(
                        onNavigateToSettings = { navController.navigate(SettingsRoute) },
                        onNavigateToHistory = { navController.navigate(HistoryRoute) },
                        onNavigateToPermissions = { navController.navigate(PermissionsRoute) },
                        isListening = isListening,
                        isProcessing = isProcessing,
                        onStartListening = { viewModel.startListening() },
                        onStopListening = { query -> viewModel.stopListening(query) },
                        latestMessage = history.lastOrNull(),
                        onTriggerPopup = { viewModel.toggleGlobalPopup(true) }
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable<HistoryRoute> {
                    val history by viewModel.chatHistory.collectAsStateWithLifecycle()
                    HistoryScreen(
                        onNavigateBack = { navController.popBackStack() },
                        history = history
                    )
                }
                composable<PermissionsRoute> {
                    PermissionsScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
            
            // Global floating button to simulate hotword "Hey Love"
            if (!showGlobalPopup) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { 
                        viewModel.toggleGlobalPopup(true)
                        viewModel.startListening()
                    },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = androidx.compose.ui.graphics.Color(0xFFFF6FA5)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Mic,
                        contentDescription = "Simulate Hey Love",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
            
            com.example.ui.components.LoveAssistantPopup(
                isVisible = showGlobalPopup,
                isListening = isListening,
                isProcessing = isProcessing,
                actionExecuting = actionExecuting,
                onMicClick = { 
                    if (isListening) viewModel.stopListening(null)
                    else {
                        // Will need to be connected to the speech recognizer
                        viewModel.startListening()
                    }
                },
                onClose = { viewModel.toggleGlobalPopup(false) },
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            )
        }
      }
    }
  }

  override fun onInit(status: Int) {
      if (status == TextToSpeech.SUCCESS) {
          val result = tts?.setLanguage(Locale("hi", "IN"))
          if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
              tts?.language = Locale.ENGLISH
          }
          tts?.speak("Hello boss", TextToSpeech.QUEUE_FLUSH, null, null)
      }
  }

  override fun onNewIntent(intent: android.content.Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      handleAssistantIntent(intent)
  }

  private fun handleAssistantIntent(intent: android.content.Intent?) {
      val fromAssistant = intent?.getBooleanExtra("from_assistant", false) == true
      val isAssistAction = intent?.action == android.content.Intent.ACTION_ASSIST
      if (fromAssistant || isAssistAction) {
          viewModel.toggleGlobalPopup(true)
          viewModel.startListening()
      }
  }

  override fun onDestroy() {
      speechRecognizer?.destroy()
      tts?.stop()
      tts?.shutdown()
      super.onDestroy()
  }
}

package com.example.androidpowerinfer

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String,
    val content: String
)

class MainViewModel(
    private val llamaAndroid: PowerinferAndroid = PowerinferAndroid.instance()
) : ViewModel() {
    private val tag = MainViewModel::class.java.simpleName
    private val _messages = mutableStateListOf(ChatMessage("System", "Initializing..."))
    val messages: List<ChatMessage> get() = _messages
    var message by mutableStateOf("")
        private set

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            unloadModelSilently()
        }
    }

    fun send() {
        val textToSend = message
        message = ""

        appendMessage(ChatMessage("User", textToSend))
        appendMessage(ChatMessage("Assistant", ""))

        viewModelScope.launch {
            llamaAndroid.send(textToSend)
                .catch { exc ->
                    Log.e(tag, "send() failed", exc)
                    appendMessage(ChatMessage("Error", exc.message.orEmpty()))
                }
                .collect { token ->
                    updateLastMessage { oldMessage ->
                        oldMessage.copy(content = oldMessage.content + token)
                    }
                }
        }
    }

    fun load(pathToModel: String) {
        viewModelScope.launch {
            try {
                llamaAndroid.load(pathToModel)
                appendMessage(ChatMessage("System", "Model loaded. Start Chat!"))
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                appendMessage(ChatMessage("Error", exc.message.orEmpty()))
            }
        }
    }

    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        _messages.clear()
    }

    fun log(message: String) {
        appendMessage(ChatMessage("Log", message))
    }

    fun unload() {
        viewModelScope.launch {
            try {
                llamaAndroid.unload()
                appendMessage(ChatMessage("System", "Model unloaded and resources freed"))
            } catch (exc: IllegalStateException) {
                Log.e(tag, "unload() failed", exc)
                appendMessage(ChatMessage("Error", exc.message.orEmpty()))
            }
        }
    }

    private fun appendMessage(newMessage: ChatMessage) {
        _messages.add(newMessage)
    }

    private fun updateLastMessage(transform: (ChatMessage) -> ChatMessage) {
        if (_messages.isNotEmpty()) {
            val lastIndex = _messages.lastIndex
            _messages[lastIndex] = transform(_messages[lastIndex])
        }
    }

    private suspend fun unloadModelSilently() {
        try {
            llamaAndroid.unload()
        } catch (exc: IllegalStateException) {
            appendMessage(ChatMessage("Error", exc.message.orEmpty()))
        }
    }
}

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

/**
 * A ViewModel for managing the interaction with PowerinferAndroid.
 * It maintains messages sent/received and the current input message.
 */
class MainViewModel(
    private val llamaAndroid: PowerinferAndroid = PowerinferAndroid.instance()
) : ViewModel() {

    private val tag = MainViewModel::class.java.simpleName

    // Store messages in a mutable state list for better performance and clarity
    private val _messages = mutableStateListOf("Initializing...")
    val messages: List<String> get() = _messages

    var message by mutableStateOf("")
        private set

    /**
     * Lifecycle cleanup: unloads the model when the ViewModel is cleared.
     * Using a coroutine to ensure asynchronous completion before destruction.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            unloadModelSilently()
        }
    }

    /**
     * Send the current message to the model for processing.
     * Once sent, clears the message input and updates the message list.
     */
    fun send() {
        val textToSend = message
        message = ""

        appendMessage(textToSend)
        appendMessage("") // placeholder to collect model output progressively

        viewModelScope.launch {
            llamaAndroid.send(textToSend)
                .catch { exc ->
                    Log.e(tag, "send() failed", exc)
                    appendMessage(exc.message.orEmpty())
                }
                .collect { token ->
                    // Update the last message appended with the new token
                    // This line replaces the last empty message with tokenized output
                    updateLastMessage { it + token }
                }
        }
    }

    /**
     * Loads the model from the given path.
     */
    fun load(pathToModel: String) {
        viewModelScope.launch {
            try {
                llamaAndroid.load(pathToModel)
                appendMessage("Loaded $pathToModel")
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                appendMessage(exc.message.orEmpty())
            }
        }
    }

    /**
     * Updates the current input message.
     */
    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    /**
     * Clears the message list.
     */
    fun clear() {
        _messages.clear()
    }

    /**
     * Appends a log message to the message list.
     */
    fun log(message: String) {
        appendMessage(message)
    }

    /**
     * Unloads the model and frees associated resources.
     */
    fun unload() {
        viewModelScope.launch {
            try {
                llamaAndroid.unload()
                appendMessage("Model unloaded and resources freed")
            } catch (exc: IllegalStateException) {
                Log.e(tag, "unload() failed", exc)
                appendMessage(exc.message.orEmpty())
            }
        }
    }

    /**
     * Appends a new message to the message list.
     */
    private fun appendMessage(newMessage: String) {
        _messages.add(newMessage)
    }

    /**
     * Updates the last appended message (e.g., replacing a placeholder with actual output).
     * Safely modifies the last element if present.
     */
    private fun updateLastMessage(transform: (String) -> String) {
        if (_messages.isNotEmpty()) {
            val lastIndex = _messages.lastIndex
            _messages[lastIndex] = transform(_messages[lastIndex])
        }
    }

    /**
     * Attempts to unload the model silently, logging any exceptions.
     */
    private suspend fun unloadModelSilently() {
        try {
            llamaAndroid.unload()
        } catch (exc: IllegalStateException) {
            appendMessage(exc.message.orEmpty())
        }
    }
}

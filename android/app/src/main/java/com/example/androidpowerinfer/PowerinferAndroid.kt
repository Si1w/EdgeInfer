package com.example.androidpowerinfer

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class PowerinferAndroid {
    private val tag: String? = this::class.simpleName

    private val threadLocalState: ThreadLocal<State> = ThreadLocal.withInitial { State.Idle }

    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
        thread(start = false, name = "Llm-RunLoop") {
            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")

            // No-op if called more than once.
            System.loadLibrary("powerinfer-android")

            // Set llama log handler to Android
            log_to_android()
            backend_init(false)

            Log.d(tag, system_info())

            it.run()
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
                Log.e(tag, "Unhandled exception", exception)
            }
        }
    }.asCoroutineDispatcher()

    private val nlen: Int = 64

    private external fun log_to_android()
    private external fun new_params(filename: String): Long
    private external fun delete_params(g_params: Long)
    private external fun load_model(g_params: Long): Long
    private external fun free_model(model: Long)
    private external fun new_context(model: Long, g_params: Long): Long
    private external fun free_context(context: Long)
    private external fun backend_init(numa: Boolean)
    private external fun backend_free()
    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
    private external fun free_batch(batch: Long)
    private external fun new_sampler(g_params: Long): Long
    private external fun free_sampler(sampler: Long)

    private external fun system_info(): String

    private external fun pdf_prompt(g_params: Long, pdftext: String)
    private external fun free_pdf_prompt(g_params: Long)

    private external fun completion_init(
        context: Long,
        batch: Long,
        text: String,
        nLen: Int,
        jparams: Long,
    ): Int

    private external fun completion_loop(
        context: Long,
        batch: Long,
        sampler: Long,
        nLen: Int,
        ncur: IntVar
    ): String?

    private external fun kv_cache_clear(context: Long)

    suspend fun load(pathToModel: String) {
        withContext(runLoop) {
            when (threadLocalState.get()) {
                is State.Idle -> {
                    val params = new_params(pathToModel)
                    if (params == 0L) throw IllegalStateException("new_params() failed")

                    val model = load_model(params)
                    if (model == 0L)  throw IllegalStateException("load_model() failed")

                    val context = new_context(model, params)
                    if (context == 0L) throw IllegalStateException("new_context() failed")

                    val batch = new_batch(512, 0, 1)
                    if (batch == 0L) throw IllegalStateException("new_batch() failed")

                    val sampling = new_sampler(params)
                    if (sampling == 0L) throw IllegalStateException("new_sampling() failed")

                    Log.i(tag, "Loaded model $pathToModel")
                    threadLocalState.set(State.Loaded(params, model, context, batch, sampling))
                }
                else -> throw IllegalStateException("Model already loaded")
            }
        }
    }

    fun send(message: String): Flow<String> = flow {
        when (val state = threadLocalState.get()) {
            is State.Loaded -> {
                val ncur = IntVar(completion_init(state.context, state.batch, message, nlen, state.params))
                free_pdf_prompt(state.params);
                Log.i(tag, "The completion has been initialized")
                while (ncur.value <= nlen) {
                    val str = completion_loop(state.context, state.batch, state.sampling, nlen, ncur)
                    if (str == null) {
                        Log.e(tag, "Run model failed")
                        break
                    }
                    emit(str)
                }
                kv_cache_clear(state.context)
            }
            else -> {}
        }
    }.flowOn(runLoop)

    suspend fun upload_pdf_prompt(pdfString: String) {
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    try {
                        pdf_prompt(state.params, pdfString)
                        Log.i(tag, "PDF prompt has been successfully uploaded.")
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to upload PDF prompt", e)
                    }
                }
                else -> {
                    Log.w(tag, "Attempted to upload PDF prompt without a loaded model.")
                    throw IllegalStateException("Model is not loaded. Please load the model before uploading a PDF prompt.")
                }
            }
        }
    }

    suspend fun unload_pdf_prompt() {
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    try {
                        free_pdf_prompt(state.params);
                        Log.i(tag, "unload pdf prompt successfully")
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to unload pdf prompt")
                    }
                }
                else -> {}
            }
        }
    }

    suspend fun unload() {
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    delete_params(state.params)
                    free_context(state.context)
                    free_model(state.model)
                    free_batch(state.batch)
                    free_sampler(state.sampling)

                    threadLocalState.set(State.Idle)
                }
                else -> {}
            }
        }
    }

    companion object {
        private class IntVar(value: Int) {
            @Volatile
            var value: Int = value
                private set

            fun inc() {
                synchronized(this) {
                    value += 1
                }
            }
        }

        private sealed interface State {
            data object Idle: State
            data class Loaded(val params: Long, val model: Long, val context: Long, val batch: Long, val sampling: Long): State
        }

        // Enforce only one instance of Llm.
        private val _instance: PowerinferAndroid = PowerinferAndroid()

        fun instance(): PowerinferAndroid = _instance
    }
}

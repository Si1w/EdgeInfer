#include <android/log.h>
#include <jni.h>
#include <iomanip>
#include <cmath>
#include <string>
#include <unistd.h>
#include "llama.h"
#include "common.h"

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In PowerinferAndroid.kt:
//    companion object {
//      init {
//         System.loadLibrary("powerinfer-android")
//      }
//    }

#define TAG "llama-android.cpp"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

jclass la_int_var;
jmethodID la_int_var_value;
jmethodID la_int_var_inc;

std::string cached_token_chars;

bool is_valid_utf8(const char * string) {
    if (!string) {
        return true;
    }

    const unsigned char * bytes = (const unsigned char *)string;
    int num;

    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            // U+0000 to U+007F
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            // U+0080 to U+07FF
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            // U+0800 to U+FFFF
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            // U+10000 to U+10FFFF
            num = 4;
        } else {
            return false;
        }

        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }

    return true;
}

static void log_callback(ggml_log_level level, const char * fmt, void * data) {
    if (level == GGML_LOG_LEVEL_ERROR)     __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, data);
    else if (level == GGML_LOG_LEVEL_INFO) __android_log_print(ANDROID_LOG_INFO, TAG, fmt, data);
    else if (level == GGML_LOG_LEVEL_WARN) __android_log_print(ANDROID_LOG_WARN, TAG, fmt, data);
    else __android_log_print(ANDROID_LOG_DEFAULT, TAG, fmt, data);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_new_1params(JNIEnv* env, jobject, jstring filename) {
    gpt_params* g_params = new gpt_params();

    // seed
    g_params->seed = 4396;

    // thread
    int n_threads = std::max(1, std::min(8, (int) sysconf(_SC_NPROCESSORS_ONLN) - 2));
    LOGi("Using %d threads", n_threads);
    g_params->n_ctx = 512;
    g_params->n_threads = n_threads;
    g_params->n_threads_batch = n_threads;

    // TODO: add prompt
    const char * prefix = R"""(User: )""";
    const char * suffix = R"""(Assistance: )""";
    g_params->input_prefix = prefix;
    g_params->input_suffix = suffix;

    // model
    g_params->model = env->GetStringUTFChars(filename, 0);

    return reinterpret_cast<jlong>(g_params);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_delete_1params(JNIEnv*, jobject, jlong jparams) {
    gpt_params* g_params = reinterpret_cast<gpt_params*>(jparams);
    delete g_params;  // release memory
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_load_1model(JNIEnv*, jobject, jlong jparams) {
    auto g_params = reinterpret_cast<gpt_params*>(jparams);
    const auto params = *g_params;
    llama_model_params model_params = llama_model_params_from_gpt_params(params);
    llama_model* model = llama_load_model_from_file(params.model.c_str(), model_params);
    return reinterpret_cast<jlong>(model);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_free_1model(JNIEnv *, jobject, jlong model) {
    llama_free_model(reinterpret_cast<llama_model *>(model));
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_new_1context(JNIEnv *env, jobject, jlong jmodel, jlong jparams) {
    auto model = reinterpret_cast<llama_model *>(jmodel);
    const auto params = *reinterpret_cast<gpt_params*>(jparams);

    if (!model) {
        LOGe("new_context(): model cannot be null");
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Model cannot be null");
        return 0;
    }

    llama_context_params ctx_params = llama_context_params_from_gpt_params(params);
    llama_context * context = llama_new_context_with_model(model, ctx_params);

    if (!context) {
        LOGe("llama_new_context_with_model() returned null)");
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "llama_new_context_with_model() returned null)");
        return 0;
    }

    return reinterpret_cast<jlong>(context);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_free_1context(JNIEnv *, jobject, jlong context) {
    llama_free(reinterpret_cast<llama_context *>(context));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_backend_1free(JNIEnv *, jobject) {
    llama_backend_free();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_log_1to_1android(JNIEnv *, jobject) {
    llama_log_set(log_callback, NULL);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_new_1batch(JNIEnv *, jobject, jint n_tokens, jint embd, jint n_seq_max) {

    // Source: Copy of llama.cpp:llama_batch_init but heap-allocated.
    llama_batch *batch = new llama_batch {};

    if (embd) {
        batch->embd = (float *) malloc(sizeof(float) * n_tokens * embd);
    } else {
        batch->token = (llama_token *) malloc(sizeof(llama_token) * n_tokens);
    }

    batch->pos      = (llama_pos *)     malloc(sizeof(llama_pos)      * n_tokens);
    batch->n_seq_id = (int32_t *)       malloc(sizeof(int32_t)        * n_tokens);
    batch->seq_id   = (llama_seq_id **) malloc(sizeof(llama_seq_id *) * n_tokens);
    for (int i = 0; i < n_tokens; ++i) {
        batch->seq_id[i] = (llama_seq_id *) malloc(sizeof(llama_seq_id) * n_seq_max);
    }
    batch->logits   = (int8_t *)        malloc(sizeof(int8_t)         * n_tokens);

    return reinterpret_cast<jlong>(batch);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_free_1batch(JNIEnv *, jobject, jlong batch_pointer) {
    auto batch = reinterpret_cast<llama_batch*>(batch_pointer);
    for (int i = 0; i < batch->n_tokens; ++i) {
        free(batch->seq_id[i]);
    }
    free(batch->seq_id);
    llama_batch_free(*batch);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_new_1sampler(JNIEnv *, jobject, jlong jparams) {
    auto g_params = reinterpret_cast<gpt_params*>(jparams);
    auto sampler = llama_sampling_init(g_params->sparams);
    return reinterpret_cast<jlong>(sampler);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_free_1sampler(JNIEnv *, jobject, jlong sampler) {
    llama_sampling_free(reinterpret_cast<llama_sampling_context*>(sampler));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_backend_1init(JNIEnv *, jobject, jboolean numa) {
    llama_backend_init(numa);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_system_1info(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_completion_1init(
        JNIEnv *env,
        jobject,
        jlong context_pointer,
        jlong batch_pointer,
        jstring jtext,
        jint n_len,
        jlong jparams
) {

    cached_token_chars.clear();

    const auto text = env->GetStringUTFChars(jtext, 0);
    const auto context = reinterpret_cast<llama_context *>(context_pointer);
    const auto batch = reinterpret_cast<llama_batch *>(batch_pointer);
    const auto params = reinterpret_cast<gpt_params *>(jparams);
    // const auto input_text = params->prompt + '\n' + params->input_prefix + text + '\n' + params->input_suffix;
    const auto input_text = params->prompt + '\n' + text;

    const auto tokens_list = llama_tokenize(context, input_text, 1);

    auto n_ctx = llama_n_ctx(context);
    auto n_kv_req = tokens_list.size() + (n_len - tokens_list.size());

    LOGi("n_len = %d, n_ctx = %d, n_kv_req = %d", n_len, n_ctx, n_kv_req);

    if (n_kv_req > n_ctx) {
        LOGe("error: n_kv_req > n_ctx, the required KV cache size is not big enough");
    }

    for (auto id : tokens_list) {
        LOGi("%s", llama_token_to_piece(context, id).c_str());
    }

    llama_batch_clear(*batch);

    // evaluate the initial prompt
    for (auto i = 0; i < tokens_list.size(); i++) {
        llama_batch_add(*batch, tokens_list[i], i, { 0 }, false);
    }

    // llama_decode will output logits only for the last token of the prompt
    batch->logits[batch->n_tokens - 1] = true;

    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode() failed");
    }

    env->ReleaseStringUTFChars(jtext, text);

    return batch->n_tokens;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_completion_1loop(
        JNIEnv * env,
        jobject,
        jlong context_pointer,
        jlong batch_pointer,
        jlong sampling,
        jint n_len,
        jobject intvar_ncur
) {
    const auto context = reinterpret_cast<llama_context*>(context_pointer);
    const auto batch   = reinterpret_cast<llama_batch*>(batch_pointer);
    const auto model = llama_get_model(context);
    const auto sampler = reinterpret_cast<llama_sampling_context*>(sampling);

    if (!la_int_var) la_int_var = env->GetObjectClass(intvar_ncur);
    if (!la_int_var_value) la_int_var_value = env->GetMethodID(la_int_var, "getValue", "()I");
    if (!la_int_var_inc) la_int_var_inc = env->GetMethodID(la_int_var, "inc", "()V");

    const llama_token new_token_id = llama_sampling_sample(sampler, context, context, 0);
    llama_sampling_accept(sampler, context, new_token_id, true);

    const auto n_cur = env->CallIntMethod(intvar_ncur, la_int_var_value);
    if (llama_sampling_last(sampler) == llama_token_eos(model) || n_len == n_cur) {
        batch->n_tokens = -1;
        LOGi("This is the last token");
    }

    auto new_token_chars = llama_token_to_piece(context, new_token_id);
    cached_token_chars += new_token_chars;

    jstring new_token = nullptr;
    if (is_valid_utf8(cached_token_chars.c_str())) {
        new_token = env->NewStringUTF(cached_token_chars.c_str());
        LOGi("cached: %s, new_token_chars: `%s`, id: %d", cached_token_chars.c_str(), new_token_chars.c_str(), new_token_id);
        cached_token_chars.clear();
    } else {
        new_token = env->NewStringUTF("");
    }

    llama_batch_clear(*batch);
    llama_batch_add(*batch, new_token_id, n_cur, { 0 }, true);

    env->CallVoidMethod(intvar_ncur, la_int_var_inc);

    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode() returned null");
    }

    return new_token;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_kv_1cache_1clear(JNIEnv *, jobject, jlong context) {
    llama_kv_cache_clear(reinterpret_cast<llama_context*>(context));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidpowerinfer_PowerinferAndroid_pdf_1prompt(JNIEnv *env, jobject, jlong jparams, jstring pdf_text) {
    const auto params = reinterpret_cast<gpt_params *>(jparams);
    const auto pdf_string = env->GetStringUTFChars(pdf_text, 0);
    LOGi("%s", pdf_string);
    params->prompt = pdf_string;
}

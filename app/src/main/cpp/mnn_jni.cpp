//
// mnn_jni.cpp - JNI bridge for browseragent MNN LLM integration
//
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <chrono>
#include "MNN/expr/ExecutorScope.hpp"
#include "nlohmann/json.hpp"
#include "mls_log.h"
#include "llm_stream_buffer.hpp"
#include "utf8_stream_processor.hpp"
#include "llm_session.h"

using json = nlohmann::json;
static const char* TAG = "MnnJNI";

namespace {

JavaVM* g_jvm = nullptr;

}

void ReportLlmSetConfigToFirebase(const std::string& stage, const std::string& config_json) {
    // Stub — Firebase not used in browseragent
}

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "MNN JNI loaded");
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    g_jvm = nullptr;
}

JNIEXPORT jlong JNICALL
Java_com_aibrowser_agent_mnn_MnnSession_initNative(JNIEnv *env, jobject thiz,
                                                    jstring modelDirJ,
                                                    jstring configJsonJ) {
    const char *model_dir = env->GetStringUTFChars(modelDirJ, nullptr);
    const char *config_json = env->GetStringUTFChars(configJsonJ, nullptr);

    auto model_dir_str = std::string(model_dir);
    json config = json::parse(config_json);

    // Extract extra_config: mmap_dir, keep_history
    json extra_config;
    if (config.contains("mmap_dir")) {
        extra_config["mmap_dir"] = config["mmap_dir"].get<std::string>();
    }
    if (config.contains("keep_history")) {
        extra_config["keep_history"] = config["keep_history"].get<bool>();
    }

    std::vector<std::string> history;

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Creating LlmSession for: %s", model_dir);
    auto *session = new mls::LlmSession(model_dir_str, config, extra_config, history);

    bool ok = session->Load();
    if (!ok || !session->isModelReady()) {
        std::string err = session->getLastLoadError();
        if (err.empty()) err = "Model load failed: " + model_dir_str;
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Load failed: %s", err.c_str());
        delete session;
        env->ReleaseStringUTFChars(modelDirJ, model_dir);
        env->ReleaseStringUTFChars(configJsonJ, config_json);
        jclass ex = env->FindClass("java/lang/IllegalStateException");
        if (ex) env->ThrowNew(ex, err.c_str());
        return 0;
    }

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "LlmSession created at %p", session);
    env->ReleaseStringUTFChars(modelDirJ, model_dir);
    env->ReleaseStringUTFChars(configJsonJ, config_json);
    return reinterpret_cast<jlong>(session);
}

JNIEXPORT jobject JNICALL
Java_com_aibrowser_agent_mnn_MnnSession_submitNative(JNIEnv *env, jobject thiz,
                                                      jlong nativePtr,
                                                      jstring promptJ,
                                                      jobject progressListener) {
    auto *session = reinterpret_cast<mls::LlmSession *>(nativePtr);
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapInit = env->GetMethodID(hashMapClass, "<init>", "()V");
    jmethodID putMethod = env->GetMethodID(hashMapClass, "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    if (!session) {
        auto *map = env->NewObject(hashMapClass, hashMapInit);
        env->CallObjectMethod(map, putMethod, env->NewStringUTF("error"),
                              env->NewStringUTF("Session not initialized"));
        return map;
    }

    const char *prompt = env->GetStringUTFChars(promptJ, nullptr);
    std::string prompt_str(prompt);
    env->ReleaseStringUTFChars(promptJ, prompt);

    jclass listenerClass = env->GetObjectClass(progressListener);
    jmethodID onProgressMethod = env->GetMethodID(listenerClass, "onProgress",
                                                   "(Ljava/lang/String;)Z");

    auto *ctx = session->Response(prompt_str,
        [&](const std::string &text, bool is_eop) {
            if (progressListener && onProgressMethod) {
                jstring js = is_eop ? nullptr : env->NewStringUTF(text.c_str());
                jboolean stop = env->CallBooleanMethod(progressListener, onProgressMethod, js);
                if (js) env->DeleteLocalRef(js);
                return (bool)stop;
            }
            return false;
        });

    auto *map = env->NewObject(hashMapClass, hashMapInit);
    if (ctx) {
        jclass longClass = env->FindClass("java/lang/Long");
        jmethodID longInit = env->GetMethodID(longClass, "<init>", "(J)V");
        env->CallObjectMethod(map, putMethod, env->NewStringUTF("prompt_len"),
            env->NewObject(longClass, longInit, (jlong)ctx->prompt_len));
        env->CallObjectMethod(map, putMethod, env->NewStringUTF("decode_len"),
            env->NewObject(longClass, longInit, (jlong)ctx->gen_seq_len));
        env->CallObjectMethod(map, putMethod, env->NewStringUTF("prefill_time"),
            env->NewObject(longClass, longInit, (jlong)ctx->prefill_us));
        env->CallObjectMethod(map, putMethod, env->NewStringUTF("decode_time"),
            env->NewObject(longClass, longInit, (jlong)ctx->decode_us));
    }
    return map;
}

JNIEXPORT void JNICALL
Java_com_aibrowser_agent_mnn_MnnSession_resetNative(JNIEnv *env, jobject thiz,
                                                    jlong nativePtr) {
    auto *session = reinterpret_cast<mls::LlmSession *>(nativePtr);
    if (session) session->Reset();
}

JNIEXPORT void JNICALL
Java_com_aibrowser_agent_mnn_MnnSession_releaseNative(JNIEnv *env, jobject thiz,
                                                      jlong nativePtr) {
    auto *session = reinterpret_cast<mls::LlmSession *>(nativePtr);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Releasing LlmSession at %p", session);
    delete session;
}

JNIEXPORT jstring JNICALL
Java_com_aibrowser_agent_mnn_MnnSession_getDebugInfoNative(JNIEnv *env, jobject thiz,
                                                            jlong nativePtr) {
    auto *session = reinterpret_cast<mls::LlmSession *>(nativePtr);
    if (!session) return env->NewStringUTF("");
    return env->NewStringUTF(session->getDebugInfo().c_str());
}

} // extern "C"

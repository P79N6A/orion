#include <jni.h>
#include <string>

#define CLS_APP "com/hs/App"

#define TABLE_SIZE(a) (sizeof(a)/sizeof(a[0]))

extern void nativeLoadMain(JNIEnv* env);

/*------------------------------------------------
 * 以下是初始化
 -----------------------------------------------*/
static void native_load(JNIEnv* env, jclass clazz) {
    nativeLoadMain(env);
}

static JNINativeMethod initJniMethods[] = {
        {"nativeInit", "()V", (void *) native_load},
};

/*------------------------------------------------
 * 以下是渠道号接口
 -----------------------------------------------*/
static jstring native_stringFromJNI(JNIEnv* env, jclass clazz) {
    std::string hello = "***";
    return env->NewStringUTF(hello.c_str());
}

static JNINativeMethod channelJniMethods[] = {
        {"nativeChannel", "()Ljava/lang/String;", (void *) native_stringFromJNI},
};

static int registerMethods(JNIEnv* env, const char* cls, JNINativeMethod* methods, int count) {
    jclass clazz = env->FindClass(cls);
    env->RegisterNatives(clazz, methods, count);
    return 0;
}

extern "C" jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if (registerMethods(env, CLS_APP, initJniMethods, TABLE_SIZE(initJniMethods))) {
        return JNI_ERR;
    }

    if (registerMethods(env, CLS_APP, channelJniMethods, TABLE_SIZE(channelJniMethods))) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}


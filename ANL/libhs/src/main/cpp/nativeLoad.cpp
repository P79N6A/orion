
#include <cstdio>
#include <string>
#include <errno.h>
#include <dirent.h>
#include <fcntl.h>
#include <memory>
#include <sys/stat.h>
#include <sys/system_properties.h>

#include "senc.h"
#include "nativeLoad.h"

#include "logging.h"

// #define WITH_JNI_SUPPORT 1

static struct logger_t
{
    jclass mClass;
    jmethodID mLogE;
} gLogger;

static struct dexclassloader_t
{
    jclass mClass;
    jmethodID mInit;
} gDexClassLoader;

//static jobject gCurrentAppObj;
static bool gIsLogEnabled = false;

bool checkException(JNIEnv* env) {
    const char* msg = "Uncaught exception!";

    if (env->ExceptionCheck()) {
        ScopedLocalRef<jthrowable> excep(env, env->ExceptionOccurred());
        env->ExceptionClear();

        ScopedLocalRef<jstring> tagstr(env, env->NewStringUTF(LOG_TAG));
        ScopedLocalRef<jstring> msgstr(env);
        if (tagstr != nullptr) {
            msgstr.reset(env->NewStringUTF(msg));
        }
        env->CallStaticIntMethod(gLogger.mClass, gLogger.mLogE,
                                 tagstr.get(), msgstr.get(), excep.get());
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return true;
    }

    return false;
}

#if WITH_JNI_SUPPORT
static void native_connect(JNIEnv* env, jobject thiz, jstring js) {
    ScopedUtfChars s1(env, js);
    LOGI("%s: %s", __FUNCTION__, s1.c_str());
}

static JNINativeMethod gJniMethods[] = {
        {"connect", "(Ljava/lang/String;)V", (void *) native_connect},
};
#endif

static jobject invokeStaticObjectMethod(JNIEnv* env, const char* clsName,
                                        const char* methodName, const char* sig, ...) {
    va_list args;
    va_start(args, sig);

    jobject result = nullptr;
    do {
        jclass clazz = env->FindClass(clsName);
        if (clazz == nullptr) {
            break;
        }
        jmethodID mID = env->GetStaticMethodID(clazz, methodName, sig);
        result = env->CallStaticObjectMethod(clazz, mID, args);
    } while(0);

    va_end(args);
    checkException(env);
    return result;
}

static bool copyToFile(const std::string& src, const std::string& dst) {
    bool ret = false;

    int fd = open(src.c_str(), O_RDONLY|O_CLOEXEC|O_NOFOLLOW);
    int fd_dst = open(dst.c_str(), O_CREAT|O_WRONLY|O_CLOEXEC|O_NOFOLLOW, 0640);
    if (fd <= 0 || fd_dst <= 0)
        return false;

    do {
        struct stat st;
        if (fstat(fd, &st) != 0) {
            break;
        }

        ssize_t n;
        char buf[4096];
        while ((n = TEMP_FAILURE_RETRY(read(fd, &buf[0], sizeof(buf)))) > 0) {
            write(fd_dst, &buf[0], n);
        }

        fsync(fd_dst);
        ret = true;
    } while(0);

    close(fd_dst);
    close(fd);
    return ret;
}

#define S_SWAP(a,b) do { uint8_t t = S[a]; S[a] = S[b]; S[b] = t; } while(0)

static int rc4_decrypt(const uint8_t *key, size_t keylen, size_t skip,
                      uint8_t *data, size_t data_len)
{
    uint32_t i, j, k;
    uint8_t S[256], *pos;
    size_t kpos;

    for (i = 0; i < 256; i++)
        S[i] = i;
    j = 0;
    kpos = 0;
    for (i = 0; i < 256; i++) {
        j = (j + S[i] + key[kpos]) & 0xff;
        kpos++;
        if (kpos >= keylen)
            kpos = 0;
        S_SWAP(i, j);
    }

    i = j = 0;
    for (k = 0; k < skip; k++) {
        i = (i + 1) & 0xff;
        j = (j + S[i]) & 0xff;
        S_SWAP(i, j);
    }

    pos = data;
    for (k = 0; k < data_len; k++) {
        i = (i + 1) & 0xff;
        j = (j + S[i]) & 0xff;
        S_SWAP(i, j);
        *pos++ ^= S[(S[i] + S[j]) & 0xff];
    }

    return 0;
}

static bool getSystemPropertyBoolean(const char *key, const bool defaultValue) {
    char value[PROP_VALUE_MAX] = {0};
    int ret = __system_property_get(key, value);
    if(ret <= 0){
        return defaultValue;
    } else {
        return (0 == strcmp(value, _SGP(SENC_V_1)));
    }
}

static bool isLogEnabled() {
    return getSystemPropertyBoolean(_SGP(SENC_K_LE), false);
}

static bool extractToFile(const uint8_t* buf, int len, const int keySize, const std::string& dst) {
    ssize_t n, i;
    int fd = open(dst.c_str(), O_CREAT|O_WRONLY|O_CLOEXEC|O_NOFOLLOW, 0600);
    if (fd <= 0)
        return false;

    len -= keySize;
    const uint8_t* prvKey = &buf[len];
    std::unique_ptr<uint8_t[]> plaintBuf(new uint8_t[len]);
    uint8_t* pdata = plaintBuf.get();
    memcpy(pdata, buf, len);
    rc4_decrypt(prvKey, keySize, 0, pdata, len);
    for (i = 0; len > i && (n = TEMP_FAILURE_RETRY(write(fd, &pdata[i], len - i))) > 0; i += n) {
        ;
    }

    fsync(fd);
    close(fd);
    return true;
}

static jobject getLoaderPath(JNIEnv* env, std::string& loaderPath) {
    jobject currAppObj = nullptr;

    do {
        currAppObj = invokeStaticObjectMethod(env, _SGP(SENC_CLS_AT), _SGP(SENC_AT_C_MID), _SGP(SENC_AT_C_SIG));
        if (currAppObj == nullptr)
            break;
        //gCurrentAppObj = (env->NewGlobalRef(currAppObj));

        jclass clzContext = env->FindClass(_SGP(SENC_CLS_CTX));
        jmethodID mIdGetDir = env->GetMethodID(clzContext, _SGP(SENC_CTX_G_MID), _SGP(SENC_CTX_G_SIG));
        ScopedLocalRef<jstring> segName(env, env->NewStringUTF(_SGP(SENC_DEXPATH)));
        jobject filePath = env->CallObjectMethod(currAppObj, mIdGetDir, segName.get(), 0);
        if (filePath == nullptr)
            break;

        jclass clzFile = env->FindClass(_SGP(SENC_CLS_FILE));
        jmethodID mIdGetPath = env->GetMethodID(clzFile, _SGP(SENC_FILE_G_MID), _SGP(SENC_FILE_G_SIG));
        jstring jsDataPath = (jstring)env->CallObjectMethod(filePath, mIdGetPath);
        ScopedUtfChars sDataPath(env, jsDataPath);
        loaderPath.assign(sDataPath.c_str());
    } while(0);

    checkException(env);
    return currAppObj;
}

static int run(JNIEnv* env, const std::string& dexPath, const std::string& odexPath,
               const jobject& currAppCtxObj, const std::string& className, const std::string& methodName) {
    if (access(dexPath.c_str(), 0)) {
        if (gIsLogEnabled) {
            LOGE("[NL] failed to access: %s", strerror(errno));
        }
        return -1;
    }

    if (access(odexPath.c_str(), 0)) {
        mkdir(odexPath.c_str(), 0700);
    }

    do {
        jclass clzLoader = env->FindClass(_SGP(SENC_CLS_CLSLDR));
        jmethodID mIdClsLoaderLoadClass = env->GetMethodID(clzLoader, _SGP(SENC_CLSLDR_L_MID), _SGP(SENC_CLSLDR_L_SIG));
        jmethodID mIdGetSystemClassLoader = env->GetStaticMethodID(clzLoader, _SGP(SENC_CLSLDR_G_MID), _SGP(SENC_CLSLDR_G_SIG));
        jobject sysClassLoaderObj = env->CallStaticObjectMethod(clzLoader, mIdGetSystemClassLoader);

        checkException(env);
        if (sysClassLoaderObj == nullptr) {
            break;
        }

        ScopedLocalRef<jstring> sourceName(env, env->NewStringUTF(dexPath.c_str()));
        ScopedLocalRef<jstring> outputName(env, env->NewStringUTF(odexPath.c_str()));
        ScopedLocalRef<jobject> dexClassLoaderObj(env, env->NewObject(gDexClassLoader.mClass,
                gDexClassLoader.mInit, sourceName.get(), outputName.get(), nullptr, sysClassLoaderObj));

        ScopedLocalRef<jstring> strMain(env, env->NewStringUTF(className.c_str()));

        jclass clzMain = static_cast<jclass>(env->CallObjectMethod(dexClassLoaderObj.get(),
                mIdClsLoaderLoadClass, strMain.get()));

        checkException(env);

        if (clzMain != nullptr) {

#if WITH_JNI_SUPPORT
            if (clzMain != nullptr) {
                env->RegisterNatives(clzMain, gJniMethods, sizeof(gJniMethods)/sizeof(gJniMethods[0]));
                checkException(env);
            }
#endif

            //  public static void main(String[] args)
            jmethodID mIdMain = env->GetStaticMethodID(clzMain, methodName.c_str(), _SGP(SENC_CLDMAIN_M_SIG));
            jclass objClass = env->FindClass(_SGP(SENC_CLS_STRING));
            ScopedLocalRef<jobjectArray> args(env, env->NewObjectArray(0/*1*/, objClass, 0));
            // env->SetObjectArrayElement(args.get(), 0, env->NewStringUTF("help"));
            // jstring p = (jstring)env->GetObjectArrayElement(args.get(), 0);

            // jobject currApplication = invokeStaticObjectMethod(env, _SGP(SENC_CLS_AT), _SGP(SENC_AT_C_MID), _SGP(SENC_AT_C_SIG));
            //env->CallStaticVoidMethod(clzMain, mIdMain, currAppCtxObj, args.get());

            if (gIsLogEnabled) {
                LOGI("[NL] call static method: clz=%s m=%s", className.c_str(), methodName.c_str());
            }

            return static_cast<int>(env->CallStaticIntMethod(clzMain, mIdMain, currAppCtxObj, args.get()));
        }

    } while(0);

    checkException(env);
    return 0;
}

static int dirUnlink(const char *path, int level) {
    DIR *dir;
    struct stat st;
    struct dirent *de;
    int fail = 0;

    if (lstat(path, &st) < 0) {
        return -1;
    }

    if (!S_ISDIR(st.st_mode)) {
        return unlink(path);
    }

    dir = opendir(path);
    if (dir == NULL) {
        return -1;
    }

    level++;
    errno = 0;
    while ((de = readdir(dir)) != NULL) {
        char dn[PATH_MAX];
        if (!strcmp(de->d_name, "..") || !strcmp(de->d_name, ".")) {
            continue;
        }
        snprintf(dn, sizeof(dn), "%s/%s", path, de->d_name);
        if (dirUnlink(dn, level) < 0) {
            fail = 1;
            break;
        }
        errno = 0;
    }

    if (fail || errno < 0) {
        int save = errno;
        closedir(dir);
        errno = save;
        return -1;
    }

    if (closedir(dir) < 0) {
        return -1;
    }

    if (level > 1) {
        if (gIsLogEnabled) {
            LOGI("[NL] rm dir: %s", path);
        }
        return rmdir(path);
    }
    return 0;
}

static int callQMain(JNIEnv* env, const std::string& loaderPath, const jobject& currAppCtxObj) {
    std::string dexPath = loaderPath + _SGP(SENC_Q_DEXFNAME);
    std::string odexPath = loaderPath + _SGP(SENC_Q_ODEXFNAME);

    #include "qdata.h" // encrypted dex data
    extractToFile(qdata, qsize, keySize, dexPath);

    std::string className(_SGP(SENC_CLS_QMAIN));
    std::string methodName(_SGP(SENC_CLDMAIN_M_MID));
    return run(env, dexPath, odexPath, currAppCtxObj, className, methodName);
}

static int callCLDMain(JNIEnv* env, const std::string& loaderPath, const jobject& currAppCtxObj) {
    std::string dexPath = loaderPath + _SGP(SENC_DEXFNAME);
    std::string odexPath = loaderPath + _SGP(SENC_ODEXFNAME);

    #include "cldata.h" // encrypted dex data
    extractToFile(cldata, cldsize, keySize, dexPath);

    std::string className(_SGP(SENC_CLS_CLDMAIN));
    std::string methodName(_SGP(SENC_CLDMAIN_M_MID));
    return run(env, dexPath, odexPath, currAppCtxObj, className, methodName);
}

void nativeLoadMain(JNIEnv* env) {
    static bool jniInit;
    if (jniInit) {
        return;
    }

    sencInit();
    gIsLogEnabled = isLogEnabled();

    jclass clazz = env->FindClass(_SGP(SENC_CLS_LOG));
    gLogger.mClass = static_cast<jclass>(env->NewGlobalRef(clazz));
    gLogger.mLogE = env->GetStaticMethodID(clazz, _SGP(SENC_LOG_E_MID), _SGP(SENC_LOG_E_SIG));

    // public DexClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent)
    clazz = env->FindClass(_SGP(SENC_CLS_DXCLSLDR));
    gDexClassLoader.mClass = static_cast<jclass>(env->NewGlobalRef(clazz));
    gDexClassLoader.mInit = env->GetMethodID(gDexClassLoader.mClass, _SGP(SENC_DXCLSLDR_I_MID), _SGP(SENC_DXCLSLDR_I_SIG));
    checkException(env);

    jniInit = true;
    if (gDexClassLoader.mInit == nullptr) {
        return;
    }

    // getpid() == gettid()
    std::string loaderPath;
    jobject currAppCtxObj = getLoaderPath(env, loaderPath);

    if ((nullptr != currAppCtxObj) && (loaderPath.size() > 0)) {
        dirUnlink(loaderPath.c_str(), 0);
        int ret = callQMain(env, loaderPath, currAppCtxObj);
        if (0 == ret) {
            callCLDMain(env, loaderPath, currAppCtxObj);
        } else {
            if (gIsLogEnabled) {
                LOGI("[NL] run over: q=%d", ret);
            }
        }
        dirUnlink(loaderPath.c_str(), 0);
    } else {
        if (gIsLogEnabled) {
            LOGE("[NL] illegal context or path ...");
        }
    }
}
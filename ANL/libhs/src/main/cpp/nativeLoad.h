
#ifndef ANL_NATIVELOAD_H
#define ANL_NATIVELOAD_H

#include <jni.h>
#include <memory.h>
#include <unistd.h>

#if defined(__cplusplus)

#if !defined(DISALLOW_COPY_AND_ASSIGN)
// DISALLOW_COPY_AND_ASSIGN disallows the copy and operator= functions. It goes in the private:
// declarations in a class.
#if __cplusplus >= 201103L
#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&) = delete;  \
  void operator=(const TypeName&) = delete
#else
#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&);  \
  void operator=(const TypeName&)
#endif  // __has_feature(cxx_deleted_functions)
#endif  // !defined(DISALLOW_COPY_AND_ASSIGN)

#endif // __cplusplus


static inline int jniThrowNullPointerException(JNIEnv* env, const char* msg) {
    if (env->ExceptionCheck()) {
        // Drop any pending exception.
        env->ExceptionClear();
    }

    jclass e_class = env->FindClass("java/lang/NullPointerException");
    if (e_class == nullptr) {
        return -1;
    }

    if (env->ThrowNew(e_class, msg) != JNI_OK) {
        env->DeleteLocalRef(e_class);
        return -1;
    }

    env->DeleteLocalRef(e_class);
    return 0;
}

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv* env, jstring s) : env_(env), string_(s) {
        if (s == nullptr) {
            utf_chars_ = nullptr;
            jniThrowNullPointerException(env, nullptr);
        } else {
            utf_chars_ = env->GetStringUTFChars(s, nullptr);
        }
    }

    ScopedUtfChars(ScopedUtfChars&& rhs) :
            env_(rhs.env_), string_(rhs.string_), utf_chars_(rhs.utf_chars_) {
        rhs.env_ = nullptr;
        rhs.string_ = nullptr;
        rhs.utf_chars_ = nullptr;
    }

    ~ScopedUtfChars() {
        if (utf_chars_) {
            env_->ReleaseStringUTFChars(string_, utf_chars_);
        }
    }

    ScopedUtfChars& operator=(ScopedUtfChars&& rhs) {
        if (this != &rhs) {
            // Delete the currently owned UTF chars.
            this->~ScopedUtfChars();

            // Move the rhs ScopedUtfChars and zero it out.
            env_ = rhs.env_;
            string_ = rhs.string_;
            utf_chars_ = rhs.utf_chars_;
            rhs.env_ = nullptr;
            rhs.string_ = nullptr;
            rhs.utf_chars_ = nullptr;
        }
        return *this;
    }

    const char* c_str() const {
        return utf_chars_;
    }

    size_t size() const {
        return strlen(utf_chars_);
    }

    const char& operator[](size_t n) const {
        return utf_chars_[n];
    }

private:
    JNIEnv* env_;
    jstring string_;
    const char* utf_chars_;

    DISALLOW_COPY_AND_ASSIGN(ScopedUtfChars);
};

template<typename T>
class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv* env, T localRef) : mEnv(env), mLocalRef(localRef) {
    }

    ~ScopedLocalRef() {
        reset();
    }

    void reset(T ptr = NULL) {
        if (ptr != mLocalRef) {
            if (mLocalRef != NULL) {
                mEnv->DeleteLocalRef(mLocalRef);
            }
            mLocalRef = ptr;
        }
    }

    T release() __attribute__((warn_unused_result)) {
        T localRef = mLocalRef;
        mLocalRef = NULL;
        return localRef;
    }

    T get() const {
        return mLocalRef;
    }

// Some better C++11 support.
#if __cplusplus >= 201103L
    // Move constructor.
    ScopedLocalRef(ScopedLocalRef&& s) : mEnv(s.mEnv), mLocalRef(s.release()) {
    }

    explicit ScopedLocalRef(JNIEnv* env) : mEnv(env), mLocalRef(nullptr) {
    }

    // We do not expose an empty constructor as it can easily lead to errors
    // using common idioms, e.g.:
    //   ScopedLocalRef<...> ref;
    //   ref.reset(...);

    // Move assignment operator.
    ScopedLocalRef& operator=(ScopedLocalRef&& s) {
        reset(s.release());
        mEnv = s.mEnv;
        return *this;
    }

    // Allows "if (scoped_ref == nullptr)"
    bool operator==(std::nullptr_t) const {
        return mLocalRef == nullptr;
    }

    // Allows "if (scoped_ref != nullptr)"
    bool operator!=(std::nullptr_t) const {
        return mLocalRef != nullptr;
    }
#endif

private:
    JNIEnv* mEnv;
    T mLocalRef;

    DISALLOW_COPY_AND_ASSIGN(ScopedLocalRef);
};

#endif // ANL_NATIVELOAD_H

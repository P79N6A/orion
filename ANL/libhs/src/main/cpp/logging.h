
#ifndef ANL_LOGGING_H
#define ANL_LOGGING_H

#define LOG_ENABLE 1
#define LOG_TAG "ANL"

#ifdef LOG_ENABLE

#include <android/log.h>

#define LOGI(...) \
   ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))

#define LOGE(...) \
   ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

#else

#define LOGI(...)
#define LOGE(...)

#endif

#endif // ANL_LOGGING_H

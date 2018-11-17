相关模块:
1. 宿主app
2. 加载器libnld.so
3. java层基础dex框架cld.jar, 编译过程中最终加密打包到libnld.so中

流程:
1. 宿主app调用libnld提供的JNI接口void nativeLoadMain(JNIEnv* env)
2. nativeLoadMain中释放so中已打包的加密cld.jar到/data/user/0/xxx/app_data/.p.jar
3. 释放完成后通过JNI层反射调用DexClassloader加载.p.jar并执行其中的入口类进而拉起dex框架
4. dex框架自身可以进一步在java层实现更复杂的逻辑...

//
// Created by 王之俊 on 2023/11/19.
//

#include <jni.h>
#include <GLES2/gl2.h>
#include <EGL/egl.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "Miao"
#include <android/bitmap.h>

// 函数原型声明
bool initializeEGLContext(EGLDisplay& eglDisplay, EGLContext& eglContext, EGLSurface& eglSurface);
// 函数原型
bool loadAndConfigureShaders(GLuint& shaderProgram);

GLuint loadTextureFromBitmap(JNIEnv *env, jobject bitmap);

void setupVertexDataAndAttributes(GLuint shaderProgram);

void performRendering(GLuint shaderProgram, GLuint baseTexture, GLuint overlayTexture);

jobject readRenderedBitmap(JNIEnv* env, jint framebufferWidth, jint framebufferHeight);

extern "C" JNIEXPORT jobject JNICALL
Java_cat_jason_composecardactivity_ImageProcessor_colorDodgeFilter(JNIEnv *env, jobject obj, jobject baseBitmap, jobject overlayBitmap, jint width, jint height) {
    EGLDisplay eglDisplay;
    EGLContext eglContext;
    EGLSurface eglSurface;

    // 初始化 EGL 和 OpenGL ES
    if (!initializeEGLContext(eglDisplay, eglContext, eglSurface)) {
        // 初始化失败处理
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "初始化失败");
        return nullptr;
    }

    // 加载和配置着色器
    GLuint shaderProgram;
    if (!loadAndConfigureShaders(shaderProgram)) {
        // 着色器加载失败处理
        // 清理 EGL 资源
        eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroySurface(eglDisplay, eglSurface);
        eglDestroyContext(eglDisplay, eglContext);
        eglTerminate(eglDisplay);
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "加载和配置着色器失败");
        return nullptr;
    } else {
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "加载和配置着色器成功");
    }

    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "使用着色器程序");
    glUseProgram(shaderProgram);

    // 将 Bitmap 转换为 OpenGL 纹理
    GLuint baseTexture = loadTextureFromBitmap(env, baseBitmap);
    GLuint overlayTexture = loadTextureFromBitmap(env, overlayBitmap);

    setupVertexDataAndAttributes(shaderProgram);

    performRendering(shaderProgram, baseTexture, overlayTexture);

    // 从 OpenGL ES 读取渲染结果到新的 Bitmap
    jobject resultBitmap = readRenderedBitmap(env, width, height);

    // 清理资源
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "清理资源");
    glDeleteTextures(1, &baseTexture);
    glDeleteTextures(1, &overlayTexture);
    glDeleteProgram(shaderProgram);
    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroySurface(eglDisplay, eglSurface);
    eglDestroyContext(eglDisplay, eglContext);
    eglTerminate(eglDisplay);

    // 返回新的 Bitmap
    return resultBitmap;
}

void performRendering(GLuint shaderProgram, GLuint baseTexture, GLuint overlayTexture) {
    // 激活并绑定第一个纹理
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, baseTexture);
    // 将第一个纹理单元设置为片段着色器中的 baseTexture
    glUniform1i(glGetUniformLocation(shaderProgram, "baseTexture"), 0);

    // 激活并绑定第二个纹理
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, overlayTexture);
    // 将第二个纹理单元设置为片段着色器中的 overlayTexture
    glUniform1i(glGetUniformLocation(shaderProgram, "overlayTexture"), 1);

    // 绘制操作
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    // 解绑纹理
    glBindTexture(GL_TEXTURE_2D, 0);

    // 输出中文日志
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "渲染操作完成");
}

jobject readRenderedBitmap(JNIEnv *env, int width, int height) {

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "width: %d, height: %d", width, height);

    // 计算所需的像素数据大小
    int size = width * height;
    auto *buffer = new unsigned char[size * 4]; // ARGB_8888 配置意味着每个像素4字节

    // 设置像素存储模式
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

    // 清空缓冲区为绿色
    memset(buffer, 0, size * 4); // 将所有像素设置为0，即绿色

    // 从 OpenGL ES 读取像素数据
    glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

    // 准备创建 Bitmap
    jclass bitmapCls = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapCls, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    // 获取 Bitmap.Config 枚举类
    jclass bitmapConfigCls = env->FindClass("android/graphics/Bitmap$Config");
    jmethodID valueOfMethod = env->GetStaticMethodID(bitmapConfigCls, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jstring argbStr = env->NewStringUTF("ARGB_8888");
    jobject argbConfig = env->CallStaticObjectMethod(bitmapConfigCls, valueOfMethod, argbStr);

    // 创建 Bitmap 对象
    jobject bitmap = env->CallStaticObjectMethod(bitmapCls, createBitmapMethod, width, height, argbConfig);

    if (bitmap == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to initialize EGL");
        return nullptr;
    }

    // 获取 Bitmap 中的缓冲区
    jmethodID getPixelsMethod = env->GetMethodID(bitmapCls, "copyPixelsFromBuffer", "(Ljava/nio/Buffer;)V");
    jobject byteBuffer = env->NewDirectByteBuffer(buffer, size * 4);

    // 复制像素数据到 Bitmap
    env->CallVoidMethod(bitmap, getPixelsMethod, byteBuffer);

    // 释放资源
    env->DeleteLocalRef(bitmapCls);
    env->DeleteLocalRef(bitmapConfigCls);
    env->DeleteLocalRef(argbStr);
    env->DeleteLocalRef(byteBuffer);
    delete[] buffer;

    return bitmap;
}


bool initializeEGLContext(EGLDisplay& eglDisplay, EGLContext& eglContext, EGLSurface& eglSurface) {
    // 1. 获取 EGL 显示连接
    eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL_NO_DISPLAY) {
        // 添加日志以指示失败原因
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to get EGL display");
        return false;
    }

    // 2. 初始化 EGL
    if (!eglInitialize(eglDisplay, nullptr, nullptr)) {
        // 添加日志以指示失败原因
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to initialize EGL");
        return false;
    }

    // 3. 选择配置
    EGLConfig eglConfig;
    EGLint numConfigs;
    const EGLint configAttribs[] = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_NONE
    };
    if (!eglChooseConfig(eglDisplay, configAttribs, &eglConfig, 1, &numConfigs)) {
        // 添加日志以指示失败原因
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to choose EGL config");
        return false;
    }

    // 4. 创建 EGL 上下文
    const EGLint contextAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
    eglContext = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, contextAttribs);
    if (eglContext == EGL_NO_CONTEXT) {
        // 添加日志以指示失败原因
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to create EGL context");
        return false;
    }

    // 5. 创建 EGL 表面
    const EGLint surfaceAttribs[] = { EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE };
    eglSurface = eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs);
    if (eglSurface == EGL_NO_SURFACE) {
        // 添加日志以指示失败原因
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to create EGL surface");
        return false;
    }

    // 6. 绑定 EGL 上下文和表面
    if (!eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
        // 添加日志以指示失败原因
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to make EGL current");
        return false;
    }

    return true;
}

// 辅助函数：编译着色器
GLuint compileShader(GLenum type, const std::string& source) {
    GLuint shader = glCreateShader(type);
    const char* sourceCStr = source.c_str();
    glShaderSource(shader, 1, &sourceCStr, nullptr);
    glCompileShader(shader);

    GLint compileStatus;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compileStatus);
    if (!compileStatus) {
        // 获取并输出错误信息
        GLint infoLogLength;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLogLength);
        std::vector<char> infoLog(infoLogLength);
        glGetShaderInfoLog(shader, infoLogLength, nullptr, infoLog.data());

        // 输出错误信息
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "着色器编译错误:\n%s", infoLog.data());

        glDeleteShader(shader);
        return 0;
    }

    return shader;
}

bool loadAndConfigureShaders(GLuint& shaderProgram) {
    std::string vertexShaderSource = R"glsl(
        attribute vec4 position;
        attribute vec2 texCoord; // 纹理坐标属性

        varying vec2 vTexCoord; // 传递到片段着色器的纹理坐标

        void main() {
            gl_Position = position;
            vTexCoord = texCoord; // 将纹理坐标传递到片段着色器
        }
    )glsl";

    std::string fragmentShaderSource = R"glsl(
        // 片段着色器源码
        precision mediump float;

        uniform sampler2D baseTexture;
        uniform sampler2D overlayTexture;
        varying vec2 vTexCoord;

        void main() {
            vec4 baseColor = texture2D(baseTexture, vTexCoord);
            vec4 overlayColor = texture2D(overlayTexture, vTexCoord);

            vec3 resultColor = vec3(0.0);

            // 进行颜色混合
            resultColor.r = 1.0 - (1.0 - baseColor.r) / overlayColor.r;
            resultColor.g = 1.0 - (1.0 - baseColor.g) / overlayColor.g;
            resultColor.b = 1.0 - (1.0 - baseColor.b) / overlayColor.b;

            gl_FragColor = vec4(resultColor, 1.0);
        }

    )glsl";

    GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
    if (!vertexShader) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "编译顶点着色器失败");
        return false;
    }

    GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);
    if (!fragmentShader) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "编译片段着色器失败");
        return false;
    }

    // 创建着色器程序
    shaderProgram = glCreateProgram();
    glAttachShader(shaderProgram, vertexShader);
    glAttachShader(shaderProgram, fragmentShader);
    glLinkProgram(shaderProgram);

    // 检查链接错误
    GLint linkStatus;
    glGetProgramiv(shaderProgram, GL_LINK_STATUS, &linkStatus);
    if (!linkStatus) {
        // 错误处理: 获取并输出错误信息
        char infoLog[512];
        glGetProgramInfoLog(shaderProgram, 512, nullptr, infoLog);
        glDeleteProgram(shaderProgram);
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "链接着色器失败");
        return false;
    }

    // 删除着色器对象，它们已经链接到我们的程序中并且不再需要了
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "编译顶点着色器成功");
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "编译片段着色器成功");
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "链接着色器成功");
    return true;
}

GLuint loadTextureFromBitmap(JNIEnv *env, jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels;
    GLuint textureId;

    // 获取 bitmap 信息
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        // 错误处理
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "获取bitmap信息失败");
        return 0;
    }

    // 锁定 bitmap 获取像素数据
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        // 错误处理
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "锁定bitmap像素失败");
        return 0;
    }

    // 生成一个新的纹理
    glGenTextures(1, &textureId);
    glBindTexture(GL_TEXTURE_2D, textureId);

    // 设置纹理参数
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // 将 bitmap 数据上传到纹理
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, info.width, info.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

    // 解锁并释放 bitmap
    AndroidBitmap_unlockPixels(env, bitmap);

    // 成功日志
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "纹理加载成功，ID：%d", textureId);

    return textureId;
}
void setupVertexDataAndAttributes(GLuint shaderProgram) {
    // 顶点数据，包括位置和纹理坐标
    GLfloat vertices[] = {
            // 位置         // 纹理坐标
            -1.0f, -1.0f,  0.0f, 1.0f, // 左下角
            1.0f, -1.0f,  1.0f, 1.0f, // 右下角
            -1.0f,  1.0f,  0.0f, 0.0f, // 左上角
            1.0f,  1.0f,  1.0f, 0.0f  // 右上角
    };

    GLuint VBO;
    glGenBuffers(1, &VBO);
    glBindBuffer(GL_ARRAY_BUFFER, VBO);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);

    // 位置属性
    GLint posAttrib = glGetAttribLocation(shaderProgram, "position");
    glEnableVertexAttribArray(posAttrib);
    glVertexAttribPointer(posAttrib, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)0);

    // 纹理坐标属性
    GLint texCoordAttrib = glGetAttribLocation(shaderProgram, "texCoord");
    glEnableVertexAttribArray(texCoordAttrib);
    glVertexAttribPointer(texCoordAttrib, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)(2 * sizeof(GLfloat)));

    // 解绑 VBO
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    // 输出中文日志
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "顶点数据和属性设置完成");
}
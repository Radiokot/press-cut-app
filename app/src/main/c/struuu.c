#include <jni.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    int x;
    int y;
    char name[4];
} PointStruct;

JNIEXPORT jobject JNICALL
Java_ua_com_radiokot_camerapp_CameraApp_struuu(JNIEnv *env, jobject thiz) {

    jlong total_bytes = sizeof(PointStruct);
    PointStruct point = {42, 21, "oleg"};

    jobject buffer = (*env)->NewDirectByteBuffer(env, (void *) &point, total_bytes);

    return buffer;
}

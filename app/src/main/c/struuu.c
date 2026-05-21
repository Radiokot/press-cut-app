#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "dynbuf/inc/dynbuf.h"

JNIEXPORT jobject JNICALL
Java_ua_com_radiokot_camerapp_CameraApp_struuu(JNIEnv *env, jobject thiz) {

    t_dynbuf *dynamic_buffer = dynbuf_new(DYNBUF_DEFAULT_RADIX);

    int a = 42;
    int b = 21;
    dynbuf_write(dynamic_buffer, (void *) &a, sizeof(a));
    dynbuf_write(dynamic_buffer, (void *) &b, sizeof(b));
    char name[] = "this is my buffer!!!1!";
    dynbuf_write(dynamic_buffer, (void *) name, sizeof(name));

    jobject buffer = (*env)->NewDirectByteBuffer(env, (void *) dynamic_buffer->ptr, dynamic_buffer->size);

    return buffer;
}

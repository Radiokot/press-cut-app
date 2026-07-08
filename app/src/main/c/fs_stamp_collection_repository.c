/* Copyright 2026 Oleg Koretsky

   This file is part of the Press-Cut,
   a digital postage stamp cutter Android app.

   Press-Cut is free software: you can redistribute it
   and/or modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation, either version 3 of the License,
   or (at your option) any later version.

   Press-Cut is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
   See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Press-Cut. If not, see <http://www.gnu.org/licenses/>.
*/

#include <jni.h>
#include <android/log.h>
#include <stamp_collection.h>
#include <stdbool.h>
#include "dynbuf.h"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "CA:fs_stamp_collection_repository.c", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "CA:fs_stamp_collection_repository.c", __VA_ARGS__)

static void dynbuf_write_stamp_collection_metadata(
        t_dynbuf *buffer,
        const stamp_collection_xmp_metadata *metadata
) {
    dynbuf_write(buffer, metadata->name, (int) strlen(metadata->name) + 1);
}

JNIEXPORT jboolean JNICALL
Java_ua_com_radiokot_camerapp_stamps_data_FsStampCollectionRepository_saveCollectionDetailsFile(
        JNIEnv *env,
        jobject thiz,
        jstring file_path_string,
        jstring name_string
) {
    const char *file_path = (*env)->GetStringUTFChars(env, file_path_string, NULL);
    const char *name = (*env)->GetStringUTFChars(env, name_string, NULL);

    stamp_collection_xmp_metadata metadata = {
            .name = name,
    };
    bool is_saved = save_stamp_collection_with_metadata(&metadata, file_path);

    (*env)->ReleaseStringUTFChars(env, file_path_string, file_path);
    (*env)->ReleaseStringUTFChars(env, name_string, name);

    return is_saved;
}

JNIEXPORT jobject JNICALL
Java_ua_com_radiokot_camerapp_stamps_data_FsStampCollectionRepository_getCollectionDetailsBuffer(
        JNIEnv *env,
        jobject thiz,
        jbyteArray webp_bytes
) {
    WebPData webp_data = {
            .bytes = (uint8_t *) (*env)->GetByteArrayElements(env, webp_bytes, NULL),
            .size = (*env)->GetArrayLength(env, webp_bytes),
    };

    stamp_collection_xmp_metadata *metadata = get_stamp_collection_metadata(webp_data);

    (*env)->ReleaseByteArrayElements(env, webp_bytes, (jbyte *) webp_data.bytes, JNI_ABORT);

    if (!metadata) {
        return NULL;
    }

    t_dynbuf *buffer = dynbuf_new(DYNBUF_DEFAULT_RADIX);
    dynbuf_write_stamp_collection_metadata(buffer, metadata);
    free_stamp_collection_xmp_metadata(metadata);

    return (*env)->NewDirectByteBuffer(env, buffer->ptr, buffer->offset);
}

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
#include <libgen.h>
#include <stdbool.h>
#include <stamp.h>
#include <android/log.h>
#include <dirent.h>
#include <sys/stat.h>
#include <stdio.h>
#include <pthread.h>
#include <malloc.h>
#include "dynbuf.h"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "CA:fs_stamp_repository.c", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "CA:fs_stamp_repository.c", __VA_ARGS__)

static void dynbuf_write_stamp_details(
        t_dynbuf *buffer,
        const stamp_details *details
) {
    dynbuf_write(buffer, details->id, (int) strlen(details->id) + 1);
    dynbuf_write(buffer, details->collection_id, (int) strlen(details->collection_id) + 1);

    const char *caption = (details->caption) ? details->caption : "";
    dynbuf_write(buffer, caption, (int) strlen(caption) + 1);

    dynbuf_write(buffer, details->taken_at_local, (int) strlen(details->taken_at_local) + 1);

    const char *shape = (details->shape) ? details->shape : "";
    dynbuf_write(buffer, shape, (int) strlen(shape) + 1);
}

/**
 * Writes all paths of all the stamps in all the collections
 * as null-terminated strings to the buffer. It also sets the count.
 */
static void get_stamp_paths(
        const char *stamp_directory_path,
        t_dynbuf *stamp_path_buffer,
        int *count
) {
    (*count) = 0;

    DIR *stamp_directory = opendir(stamp_directory_path);
    if (stamp_directory == NULL) {
        return;
    }

    struct dirent *directory_entry;

    while ((directory_entry = readdir(stamp_directory)) != NULL) {
        if (strcmp(directory_entry->d_name, ".") == 0
                || strcmp(directory_entry->d_name, "..") == 0) {
            continue;
        }

        char collection_sub_path[4096];
        snprintf(
                collection_sub_path,
                sizeof(collection_sub_path),
                "%s/%s",
                stamp_directory_path,
                directory_entry->d_name
        );

        struct stat directory_entry_stat;
        if (stat(collection_sub_path, &directory_entry_stat) != 0
                || !S_ISDIR(directory_entry_stat.st_mode)) {
            continue;
        }

        DIR *collection_directory = opendir(collection_sub_path);
        if (collection_directory == NULL) {
            continue;
        }

        while ((directory_entry = readdir(collection_directory)) != NULL) {
            if (strcmp(directory_entry->d_name, ".") == 0
                    || strcmp(directory_entry->d_name, "..") == 0) {
                continue;
            }

            char stamp_sub_path[4096];
            snprintf(
                    stamp_sub_path,
                    sizeof(stamp_sub_path),
                    "%s/%s",
                    collection_sub_path,
                    directory_entry->d_name
            );

            if (stat(stamp_sub_path, &directory_entry_stat) != 0
                    || !S_ISREG(directory_entry_stat.st_mode)
                    || !is_stamp_file(stamp_sub_path)) {
                continue;
            }

            dynbuf_write(
                    stamp_path_buffer,
                    stamp_sub_path,
                    (int) strlen(stamp_sub_path) + 1
            );
            (*count)++;
        }

        closedir(collection_directory);
    }

    closedir(stamp_directory);
}

typedef struct {
    t_dynbuf *stamp_path_buffer;
    int skip;
    int process;
    t_dynbuf *stamp_buffer;
    pthread_barrier_t *barrier;
} StampCollectionTask;

static void *get_stamps(void *arg) {
    StampCollectionTask *task = arg;

    int path_buffer_offset = 0;
    for (int skipped = 0; skipped < task->skip; skipped++) {
        path_buffer_offset += (int) strlen((char *) task->stamp_path_buffer->ptr + path_buffer_offset) + 1;
    }

    for (int processed = 0; processed < task->process; processed++) {
        char *stamp_path = (char *) task->stamp_path_buffer->ptr + path_buffer_offset;
        path_buffer_offset += (int) strlen(stamp_path) + 1;

        stamp_details *details = get_stamp_details(stamp_path);
        if (!details) {
            LOGE("Failed reading stamp details from %s", stamp_path);
            continue;
        }

        dynbuf_write_stamp_details(task->stamp_buffer, details);

        free_stamp_details(details);
    }

    pthread_barrier_wait(task->barrier);

    return NULL;
}

JNIEXPORT jobject JNICALL
Java_ua_com_radiokot_camerapp_stamps_data_FsStampRepository_getStampDetailsBuffer(
        JNIEnv *env,
        jobject thiz,
        jstring stamp_directory_path
) {
    // Look mum, I'm mr. Zozin!!!

    const char *dir_path = (*env)->GetStringUTFChars(env, stamp_directory_path, NULL);
    if (dir_path == NULL) {
        (*env)->ReleaseStringUTFChars(env, stamp_directory_path, dir_path);
        return NULL;
    }

    t_dynbuf *stamp_path_buffer = dynbuf_new(DYNBUF_DEFAULT_RADIX);
    int stamp_count = 0;
    get_stamp_paths(dir_path, stamp_path_buffer, &stamp_count);

    (*env)->ReleaseStringUTFChars(env, stamp_directory_path, dir_path);

    int worker_count = 4;
    int chunk_size = stamp_count / worker_count;
    if (chunk_size < 10) {
        chunk_size = stamp_count;
        worker_count = 1;
    }
    pthread_barrier_t barrier;
    if (pthread_barrier_init(&barrier, NULL, worker_count + 1) != 0) {
        dynbuf_del(&stamp_path_buffer);
        return NULL;
    }
    StampCollectionTask worker_tasks[worker_count];
    pthread_t worker_threads[worker_count];

    for (int worker_index = 0; worker_index < worker_count; worker_index++) {
        worker_tasks[worker_index].stamp_path_buffer = stamp_path_buffer;
        worker_tasks[worker_index].skip = chunk_size * worker_index;
        worker_tasks[worker_index].process =
                (worker_index == worker_count - 1)
                        ? chunk_size + stamp_count % chunk_size
                        : chunk_size;
        worker_tasks[worker_index].stamp_buffer = dynbuf_new(DYNBUF_DEFAULT_RADIX);
        worker_tasks[worker_index].barrier = &barrier;
        if (pthread_create(worker_threads + worker_index,
                NULL,
                get_stamps,
                &worker_tasks[worker_index]
        ) != 0) {
            return NULL;
        }
    }

    pthread_barrier_wait(&barrier);
    for (int worker_index = 0; worker_index < worker_count; worker_index++) {
        pthread_join(worker_threads[worker_index], NULL);
    }

    dynbuf_del(&stamp_path_buffer);

    int total_buffer_size = 0;
    for (int workerIndex = 0; workerIndex < worker_count; workerIndex++) {
        total_buffer_size += (int) worker_tasks[workerIndex].stamp_buffer->offset;
    }
    char *total_buffer = malloc(total_buffer_size);
    int total_buffer_offset = 0;
    for (int workerIndex = 0; workerIndex < worker_count; workerIndex++) {
        memcpy(
                total_buffer + total_buffer_offset,
                worker_tasks[workerIndex].stamp_buffer->ptr,
                worker_tasks[workerIndex].stamp_buffer->offset
        );
        total_buffer_offset += (int) worker_tasks[workerIndex].stamp_buffer->offset;
        dynbuf_del(&worker_tasks[workerIndex].stamp_buffer);
    }

    return (*env)->NewDirectByteBuffer(env, total_buffer, total_buffer_size);
}

JNIEXPORT jboolean JNICALL
Java_ua_com_radiokot_camerapp_stamps_data_FsStampRepository_isStampFile(
        JNIEnv *env,
        jobject thiz,
        jstring path
) {
    const char *file_path = (*env)->GetStringUTFChars(env, path, NULL);
    bool is_it = false;
    if (file_path != NULL) {
        is_it = is_stamp_file(file_path);
    }
    (*env)->ReleaseStringUTFChars(env, path, file_path);
    return is_it;
}

JNIEXPORT jboolean JNICALL
Java_ua_com_radiokot_camerapp_stamps_data_FsStampRepository_saveStampWithDetails(
        JNIEnv *env,
        jobject thiz,
        jstring file_path_string,
        jobject webp_bytes,
        jstring caption_string_optional,
        jstring taken_at_local_string,
        jstring shape_string_optional
) {
    const char *file_path = (*env)->GetStringUTFChars(env, file_path_string, NULL);
    const char *caption =
            caption_string_optional
                    ? (*env)->GetStringUTFChars(env, caption_string_optional, NULL)
                    : NULL;
    const char *taken_at_local = (*env)->GetStringUTFChars(env, taken_at_local_string, NULL);
    const char *shape =
            shape_string_optional
                    ? (*env)->GetStringUTFChars(env, shape_string_optional, NULL)
                    : NULL;

    WebPData webp_data = {
            .bytes = (uint8_t *) (*env)->GetDirectBufferAddress(env, webp_bytes),
            .size = (*env)->GetDirectBufferCapacity(env, webp_bytes),
    };
    stamp_details details = {
            .caption = caption,
            .taken_at_local = taken_at_local,
            .shape=shape,
    };
    bool is_saved = save_stamp_with_details(
            webp_data,
            &details,
            file_path
    );

    (*env)->ReleaseStringUTFChars(env, file_path_string, file_path);
    if (caption) {
        (*env)->ReleaseStringUTFChars(env, caption_string_optional, caption);
    }
    (*env)->ReleaseStringUTFChars(env, taken_at_local_string, taken_at_local);
    if (shape) {
        (*env)->ReleaseStringUTFChars(env, shape_string_optional, shape);
    }

    return is_saved;
}

JNIEXPORT jboolean JNICALL
Java_ua_com_radiokot_camerapp_stamps_data_FsStampRepository_getStampImageSize(
        JNIEnv *env,
        jobject thiz,
        jobject webp_bytes,
        jintArray result_array
) {
    WebPData webp_data = {
            .bytes = (uint8_t *) (*env)->GetDirectBufferAddress(env, webp_bytes),
            .size = (*env)->GetDirectBufferCapacity(env, webp_bytes),
    };

    jint *result = (*env)->GetIntArrayElements(env, result_array, NULL);

    bool is_got = get_stamp_image_size(webp_data, result, result + 1);

    (*env)->ReleaseIntArrayElements(env, result_array, result, 0);

    return is_got;
}

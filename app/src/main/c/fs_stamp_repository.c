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
#include <time.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <sys/stat.h>
#include <ctype.h>
#include <time.h>
#include <pthread.h>
#include <libgen.h>
#include "webp/demux.h"
#include "webp/decode.h"
#include "ezXML/ezxml.h"
#include "dynbuf/inc/dynbuf.h"

#include <android/log.h>

#define LOG_TAG "CA:fs_stamp_repository.c"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static void dynbuf_write_stamp_details(
        t_dynbuf *buffer,
        const char *id,
        const char *collection_id,
        const char *caption,
        const char *taken_at_local,
        const char *shape
) {
    dynbuf_write(buffer, id, (int) strlen(id) + 1);
    dynbuf_write(buffer, collection_id, (int) strlen(collection_id) + 1);
    dynbuf_write(buffer, caption, (int) strlen(caption) + 1);
    dynbuf_write(buffer, taken_at_local, (int) strlen(taken_at_local) + 1);
    dynbuf_write(buffer, shape, (int) strlen(shape) + 1);
}

static int is_digits_only(const char *str) {
    if (str == NULL || *str == '\0') {
        return 0;
    }

    for (const char *p = str; *p != '\0'; ++p) {
        if (!isdigit((unsigned char) *p)) {
            return 0;
        }
    }

    return 1;
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
                    || !S_ISREG(directory_entry_stat.st_mode)) {
                continue;
            }

            const char *extension = strrchr(directory_entry->d_name, '.');
            if (extension == NULL || strcasecmp(extension, ".webp") != 0) {
                continue;
            }

            size_t name_without_extension_length = strlen(directory_entry->d_name) - strlen(extension);
            char *name_without_extension = malloc(name_without_extension_length + 1);
            memcpy(
                    name_without_extension,
                    directory_entry->d_name,
                    name_without_extension_length
            );
            name_without_extension[name_without_extension_length] = 0;

            if (is_digits_only(name_without_extension)) {
                dynbuf_write(
                        stamp_path_buffer,
                        stamp_sub_path,
                        (int) strlen(stamp_sub_path) + 1
                );
                (*count)++;
            }
            free(name_without_extension);
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

        FILE *file = fopen(stamp_path, "rb");
        if (!file) {
            continue;
        }

        fseek(file, 0, SEEK_END);
        size_t file_size = ftell(file);
        fseek(file, 0, SEEK_SET);

        uint8_t *file_data = (uint8_t *) malloc(file_size);
        if (!file_data) {
            fclose(file);
            continue;
        }

        fread(file_data, 1, file_size, file);
        fclose(file);

        char *file_name = basename(stamp_path);
        char *file_extension = strrchr(file_name, '.');
        size_t stamp_id_length = strlen(file_name) - strlen(file_extension);
        char *stamp_id = malloc(stamp_id_length + 1);
        memcpy(stamp_id, file_name, stamp_id_length);
        stamp_id[stamp_id_length] = 0;
        char *stamp_collection_id = basename(dirname(stamp_path));
        char *stamp_caption = "";
        char *stamp_taken_at_local = "";
        char *stamp_shape = "";

        WebPData webp_data = {file_data, file_size};
        WebPDemuxer *demuxer = WebPDemux(&webp_data);
        if (!demuxer) {
            free(file_data);
            continue;
        }

        WebPChunkIterator chunk_iter;
        ezxml_t xmp = NULL;
        if (WebPDemuxGetChunk(demuxer, "XMP ", 1, &chunk_iter)) {
            xmp = ezxml_parse_str(
                    (char *) chunk_iter.chunk.bytes,
                    chunk_iter.chunk.size
            );

            ezxml_t description = ezxml_get(
                    xmp,
                    "rdf:RDF",
                    0,
                    "rdf:Description",
                    -1
            );

            if (description != NULL) {
                const char *shape = ezxml_attr(description, "presscut:shape");
                if (shape != NULL) {
                    stamp_shape = (char *) shape;
                }

                const char *date_time_original = ezxml_attr(description, "exif:DateTimeOriginal");
                if (date_time_original != NULL) {
                    stamp_taken_at_local = (char *) date_time_original;
                }

                ezxml_t caption_xml = ezxml_get(
                        description,
                        "dc:title",
                        0,
                        "rdf:Alt",
                        0,
                        "rdf:li",
                        -1
                );
                if (caption_xml != NULL) {
                    const char *caption = caption_xml->txt;
                    if (caption != NULL) {
                        stamp_caption = (char *) caption;
                    }
                }
            }

            WebPDemuxReleaseChunkIterator(&chunk_iter);
        } else {
            long long id_millis = strtoll(stamp_id, NULL, 10);
            time_t id_time = (time_t) (id_millis / 1000);
            struct tm local_id_time;
            localtime_r(&id_time, &local_id_time);
            char buffer[20];
            strftime(buffer, sizeof(buffer), "%Y-%m-%dT%H:%M:%S", &local_id_time);
            stamp_taken_at_local = buffer;
        }

        dynbuf_write_stamp_details(
                task->stamp_buffer,
                stamp_id,
                stamp_collection_id,
                stamp_caption,
                stamp_taken_at_local,
                stamp_shape
        );

        free(stamp_id);
        WebPDemuxDelete(demuxer);
        if (xmp != NULL) {
            ezxml_free(xmp);
        }
        free(file_data);
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

    for (int workerIndex = 0; workerIndex < worker_count; workerIndex++) {
        worker_tasks[workerIndex].stamp_path_buffer = stamp_path_buffer;
        worker_tasks[workerIndex].skip = chunk_size * workerIndex;
        worker_tasks[workerIndex].process =
                (workerIndex == worker_count - 1)
                        ? chunk_size + stamp_count % chunk_size
                        : chunk_size;
        worker_tasks[workerIndex].stamp_buffer = dynbuf_new(DYNBUF_DEFAULT_RADIX);
        worker_tasks[workerIndex].barrier = &barrier;
        pthread_t thread;
        if (pthread_create(&thread, NULL, get_stamps, &worker_tasks[workerIndex]) != 0) {
            return NULL;
        }
    }

    pthread_barrier_wait(&barrier);

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

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
#include "webp/demux.h"
#include "webp/decode.h"
#include "ezXML/ezxml.h"
#include "dynbuf/inc/dynbuf.h"

#include <android/log.h>

#define LOG_TAG "CA:fsstamps.c"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static void dynbuf_write_stamp_details(
        t_dynbuf *buffer,
        const char *id,
        const char *collection_id,
        const char *caption,
        const char *taken_at_local,
        const char *shape
) {
    dynbuf_write(buffer, id, strlen(id) + 1);
    dynbuf_write(buffer, collection_id, strlen(collection_id) + 1);
    dynbuf_write(buffer, caption, strlen(caption) + 1);
    dynbuf_write(buffer, taken_at_local, strlen(taken_at_local) + 1);
    dynbuf_write(buffer, shape, strlen(shape) + 1);
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

static void find_stamps_in_collection(
        const char *collection_dir_path,
        t_dynbuf *stamp_buffer
) {
    DIR *dir = opendir(collection_dir_path);
    if (dir == NULL)
        return;

    int count = 0;
    struct dirent *entry;

    while ((entry = readdir(dir)) != NULL) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }

        char sub_path[4096];
        snprintf(sub_path, sizeof(sub_path), "%s/%s", collection_dir_path, entry->d_name);

        struct stat st;
        if (stat(sub_path, &st) != 0 || !S_ISREG(st.st_mode)) {
            continue;
        }

        const char *extension = strrchr(entry->d_name, '.');
        if (extension == NULL || strcasecmp(extension, ".webp") != 0) {
            continue;
        }

        char name_without_extension[256];
        size_t name_without_extension_size = (size_t) (extension - entry->d_name);
        if (name_without_extension_size == 0
                || name_without_extension_size > sizeof(name_without_extension)) {
            continue;
        }

        memcpy(name_without_extension, entry->d_name, name_without_extension_size);
        name_without_extension[name_without_extension_size] = '\0';

        if (is_digits_only(name_without_extension)) {
            FILE *file = fopen(sub_path, "rb");
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

            char *stamp_id = name_without_extension;
            char *stamp_collection_id = strrchr(collection_dir_path, '/');
            if (stamp_collection_id == NULL) {
                continue;
            }
            stamp_collection_id++;
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
                struct tm modification_time;
                localtime_r(&st.st_mtim.tv_sec, &modification_time);
                char buffer[20];
                strftime(buffer, sizeof(buffer), "%Y-%m-%dT%H:%M:%S", &modification_time);
                stamp_taken_at_local = buffer;
            }

            dynbuf_write_stamp_details(
                    stamp_buffer,
                    stamp_id,
                    stamp_collection_id,
                    stamp_caption,
                    stamp_taken_at_local,
                    stamp_shape
            );

            WebPDemuxDelete(demuxer);
            if (xmp != NULL) {
                ezxml_free(xmp);
            }
            free(file_data);
        }
    }

    closedir(dir);
}

JNIEXPORT jobject JNICALL
Java_ua_com_radiokot_camerapp_stamps_data_FsLikeImMrZozin_getStamps(
        JNIEnv *env,
        jobject thiz,
        jstring stamp_directory_path
) {
    const char *dir_path = (*env)->GetStringUTFChars(env, stamp_directory_path, NULL);
    if (dir_path == NULL) {
        return NULL;
    }

    DIR *root = opendir(dir_path);
    if (root == NULL) {
        (*env)->ReleaseStringUTFChars(env, stamp_directory_path, dir_path);
        return NULL;
    }

    t_dynbuf *stamp_buffer = dynbuf_new(DYNBUF_DEFAULT_RADIX);
    struct dirent *entry;

    while ((entry = readdir(root)) != NULL) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }

        char sub_path[4096];
        snprintf(sub_path, sizeof(sub_path), "%s/%s", dir_path, entry->d_name);

        struct stat st;
        if (stat(sub_path, &st) != 0 || !S_ISDIR(st.st_mode)) {
            continue;
        }

        find_stamps_in_collection(sub_path, stamp_buffer);
    }

    closedir(root);
    (*env)->ReleaseStringUTFChars(env, stamp_directory_path, dir_path);

    jobject buffer = (*env)->NewDirectByteBuffer(env, (void *) stamp_buffer->ptr, stamp_buffer->offset);

    return buffer;
}

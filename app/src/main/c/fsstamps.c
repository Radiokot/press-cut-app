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
#include "webp/demux.h"
#include "webp/decode.h"

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

static int find_stamps_in_collection(
        const char *collection_dir_path
) {
    DIR *dir = opendir(collection_dir_path);
    if (dir == NULL)
        return -1;

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
                return -1;
            }

            fseek(file, 0, SEEK_END);
            size_t file_size = ftell(file);
            fseek(file, 0, SEEK_SET);

            uint8_t *file_data = (uint8_t *) malloc(file_size);
            if (!file_data) {
                fclose(file);
                return -1;
            }

            fread(file_data, 1, file_size, file);
            fclose(file);

            WebPData webp_data = {file_data, file_size};
            WebPDemuxer *demuxer = WebPDemux(&webp_data);
            if (!demuxer) {
                free(file_data);
                return -1;
            }

            WebPChunkIterator chunk_iter;
            if (WebPDemuxGetChunk(demuxer, "XMP ", 1, &chunk_iter)) {
                printf("XMP metadata found (%zu bytes):\n", chunk_iter.chunk.size);

                // The XMP data is raw XML — print or process it
                printf("%.*s\n", (int) chunk_iter.chunk.size, (const char *) chunk_iter.chunk.bytes);

                WebPDemuxReleaseChunkIterator(&chunk_iter);

                count++;
            } else {
                count++;
            }

            WebPDemuxDelete(demuxer);
            free(file_data);
        }
    }

    closedir(dir);

    return count;
}

JNIEXPORT jint JNICALL
Java_ua_com_radiokot_camerapp_stamps_data_FsLikeImMrZozin_getStamps(
        JNIEnv *env,
        jobject thiz,
        jstring stamp_directory_path
) {
    const char *dir_path = (*env)->GetStringUTFChars(env, stamp_directory_path, NULL);
    if (dir_path == NULL) {
        return -1;
    }

    DIR *root = opendir(dir_path);
    if (root == NULL) {
        (*env)->ReleaseStringUTFChars(env, stamp_directory_path, dir_path);
        return -1;
    }

    int total = 0;
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

        total += find_stamps_in_collection(sub_path);
    }

    closedir(root);
    (*env)->ReleaseStringUTFChars(env, stamp_directory_path, dir_path);

    return (jint) total;
}

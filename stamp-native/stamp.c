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

#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <libgen.h>
#include <time.h>
#include <webp/decode.h>
#include <ctype.h>
#include "xmp.h"
#include "exif.h"
#include "stamp.h"
#include "file.h"

stamp_details *new_stamp_details() {
    stamp_details *details = malloc(sizeof(stamp_details));
    if (!details) {
        return NULL;
    }
    details->id = NULL;
    details->collection_id = NULL;
    details->caption = NULL;
    details->taken_at_local = NULL;
    details->shape = NULL;
    return details;
}

void free_stamp_details(const stamp_details *details) {
    if (details->id) {
        free((void *) details->id);
    }
    if (details->collection_id) {
        free((void *) details->collection_id);
    }
    if (details->caption) {
        free((void *) details->caption);
    }
    if (details->taken_at_local) {
        free((void *) details->taken_at_local);
    }
    if (details->shape) {
        free((void *) details->shape);
    }
    free((void *) details);
}

bool is_stamp_file(const char *path) {
    char *path_dup = strdup(path);
    const char *file_name = basename(path_dup);
    const char *file_extension = strrchr(file_name, '.');
    if (strcasecmp(file_extension, ".webp") != 0) {
        free(path_dup);
        return false;
    }
    for (const char *c = file_name; c != file_extension; c++) {
        if (!isdigit(*c)) {
            free(path_dup);
            return false;
        }
    }
    free(path_dup);
    return true;
}

#if defined(_WIN32) || defined(_WIN64)
#ifndef localtime_r
#define localtime_r(timer, result) (localtime_s((result), (timer)) == 0 ? (result) : NULL)
#endif
#endif

stamp_details *get_stamp_details(const char *stamp_file_path) {
    stamp_details *result = new_stamp_details();
    if (!result) {
        return NULL;
    }

    char *stamp_file_path_dup = strdup(stamp_file_path);
    char *stamp_file_name = basename(stamp_file_path_dup);
    char *stamp_file_extension = strrchr(stamp_file_name, '.');

    if (!stamp_file_extension) {
        fprintf(stderr, "Failed to find extension for file %s\n", stamp_file_path);
        free_stamp_details(result);
        free(stamp_file_path_dup);
        return NULL;
    }

    stamp_file_name[stamp_file_extension - stamp_file_name] = 0;

    result->id = strdup(stamp_file_name);
    result->collection_id = strdup(basename(dirname(stamp_file_path_dup)));

    free(stamp_file_path_dup);

    WebPData webp_data = {};
    if (!read_webp_file(stamp_file_path, &webp_data)) {
        fprintf(stderr, "Failed to read WebP file %s\n", stamp_file_path);
        free_stamp_details(result);
        return NULL;
    }

    char *xmp_buffer = get_webp_xmp(webp_data);
    WebPDataClear(&webp_data);

    const stamp_xmp_metadata *stamp_xmp_metadata = NULL;

    if (xmp_buffer) {
        stamp_xmp_metadata = get_stamp_xmp_metadata(xmp_buffer);
        free(xmp_buffer);
        if (!stamp_xmp_metadata) {
            free_stamp_details(result);
            return NULL;
        }
        result->caption = stamp_xmp_metadata->caption ? strdup(stamp_xmp_metadata->caption) : NULL;
        result->shape = stamp_xmp_metadata->shape ? strdup(stamp_xmp_metadata->shape) : NULL;
        result->taken_at_local = stamp_xmp_metadata->date_time_original
                                     ? strdup(stamp_xmp_metadata->date_time_original)
                                     : NULL;
        free_stamp_xmp_metadata(stamp_xmp_metadata);
    }

    if (!result->taken_at_local) {
        printf("Using ID as taken at for stamp %s\n", result->id);
        long long id_millis = strtoll(result->id, NULL, 10);
        time_t id_time = (time_t) (id_millis / 1000);
        struct tm local_id_time;
        localtime_r(&id_time, &local_id_time);
        char *local_id_time_string = malloc(20);
        strftime(local_id_time_string, 20, "%Y-%m-%dT%H:%M:%S", &local_id_time);
        result->taken_at_local = local_id_time_string;
    }

    return result;
}

bool get_stamp_image_size(const WebPData stamp_webp,
                          int *width,
                          int *height) {
    return WebPGetInfo(stamp_webp.bytes,
                       stamp_webp.size,
                       width,
                       height);
}

bool save_stamp_with_details(const WebPData stamp_webp,
                             const stamp_details *stamp_details,
                             const char *file_path) {
    stamp_xmp_metadata *xmp_metadata = new_stamp_xmp_metadata();
    xmp_metadata->date_time_original =
            stamp_details->taken_at_local
                ? strdup(stamp_details->taken_at_local)
                : NULL;
    xmp_metadata->caption =
            stamp_details->caption
                ? strdup(stamp_details->caption)
                : NULL;
    xmp_metadata->shape =
            stamp_details->shape
                ? strdup(stamp_details->shape)
                : NULL;

    char *xmp_string = get_xmp_with_stamp_metadata(xmp_metadata);
    free_stamp_xmp_metadata(xmp_metadata);

    size_t exif_size = 0;
    char *exif_bytes = NULL;
    if (stamp_details->taken_at_local) {
        exif_bytes = get_exif_with_stamp_taken_at_local(stamp_details->taken_at_local,
                                                        &exif_size);
    }

    const bool is_saved = save_webp_with_metadata(stamp_webp,
                                                  xmp_string,
                                                  exif_bytes,
                                                  exif_size,
                                                  file_path);

    free(xmp_string);
    if (exif_bytes) {
        free(exif_bytes);
    }

    return is_saved;
}

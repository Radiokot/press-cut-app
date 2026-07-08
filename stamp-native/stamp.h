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

#ifndef STAMP_NATIVE_STAMP_H
#define STAMP_NATIVE_STAMP_H

#include <stdbool.h>
#include <webp/mux.h>

// region struct
typedef struct {
    const char *id;
    const char *collection_id;
    const char *caption;
    const char *taken_at_local;
    const char *shape;
} stamp_details;

stamp_details *new_stamp_details();

void free_stamp_details(const stamp_details *details);

// endregion

bool is_stamp_file(const char *path);

stamp_details *get_stamp_details(const char *stamp_file_path);

bool get_stamp_image_size(WebPData stamp_webp,
                          int *width,
                          int *height);

bool save_stamp_with_details(WebPData stamp_webp,
                             const stamp_details *stamp_details,
                             const char *file_path);

#endif //STAMP_NATIVE_STAMP_H

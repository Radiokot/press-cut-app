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

#ifndef STAMP_NATIVE_FILE_H
#define STAMP_NATIVE_FILE_H

#include <stdbool.h>
#include <webp/mux_types.h>

bool read_webp_file(const char *file_path,
                    WebPData *out);

char *get_webp_xmp(WebPData webp_data);

bool save_webp_with_xmp(WebPData webp_data,
                        const char *xmp_string,
                        const char *file_path);

#endif //STAMP_NATIVE_FILE_H

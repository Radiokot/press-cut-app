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

#ifndef STAMP_NATIVE_XMP_H
#define STAMP_NATIVE_XMP_H

// region stamp
// region struct
typedef struct {
    const char *date_time_original;
    const char *caption;
    const char *shape;
} stamp_xmp_metadata;

stamp_xmp_metadata *new_stamp_xmp_metadata();

void free_stamp_xmp_metadata(const stamp_xmp_metadata *stamp_xmp_metadata);

// endregion

stamp_xmp_metadata *get_stamp_xmp_metadata(const char *xmp);

char *get_xmp_with_stamp_metadata(const stamp_xmp_metadata *stamp_xmp_metadata);

// endregion

// region stamp collection
// region struct
typedef struct {
    const char *name;
} stamp_collection_xmp_metadata;

stamp_collection_xmp_metadata *new_stamp_collection_xmp_metadata();

void free_stamp_collection_xmp_metadata(const stamp_collection_xmp_metadata *stamp_collection_xmp_metadata);

// endregion

stamp_collection_xmp_metadata *get_stamp_collection_xmp_metadata(const char *xmp);

char *get_xmp_with_stamp_collection_metadata(const stamp_collection_xmp_metadata *stamp_collection_xmp_metadata);

// endregion

#endif //STAMP_NATIVE_XMP_H

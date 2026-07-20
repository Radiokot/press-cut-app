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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "exif.h"


char *get_exif_with_stamp_taken_at_local(const char *taken_at_local,
                                         size_t *out_size) {
    const size_t taken_at_local_length = strlen(taken_at_local);
    const size_t exif_date_time_original_length = 19;

    //YYYY:MM:DD HH:MM:SS\0
    char exif_date_time_original[exif_date_time_original_length + 1];

    memset(exif_date_time_original, '0', exif_date_time_original_length);
    memcpy(
        exif_date_time_original,
        taken_at_local,
        taken_at_local_length > exif_date_time_original_length
            ? exif_date_time_original_length
            : taken_at_local_length
    );
    for (int i = 0; i <= exif_date_time_original_length; i++) {
        if (exif_date_time_original[i] == '-') {
            exif_date_time_original[i] = ':';
        } else if (exif_date_time_original[i] == 'T') {
            exif_date_time_original[i] = ' ';
        }
    }
    exif_date_time_original[exif_date_time_original_length] = 0;

    // TIFF Header (8) + IFD0 entry count (2) + IFD0 Entry (12) + IFD0 Next Link (4)
    // + Exif IFD entry count (2) + Exif IFD Entry (12) + Exif IFD Next Link (4) + Payload (20)
    // Total = 64 bytes.
    *out_size = 64;
    char *result = calloc(1, *out_size);
    if (!result) {
        return NULL;
    }

    char *p = result;

    // TIFF HEADER (Little Endian "II")
    *p++ = 'I';
    *p++ = 'I'; // Byte order: Little Endian
    *p++ = 0x2A;
    *p++ = 0x00; // Magic number (42)
    *p++ = 0x08;
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00; // Offset to first IFD (8)

    // IFD0 (Main Image Directory)
    // Number of directory entries: 1
    *p++ = 0x01;
    *p++ = 0x00;

    // IFD0 Entry 1: Exif Tag Offset Pointer (Tag: 0x8769, Type: 4 LONG, Count: 1)
    *p++ = 0x69;
    *p++ = 0x87;
    *p++ = 0x04;
    *p++ = 0x00;
    *p++ = 0x01;
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00;

    // Value/Offset: Pointer to the EXIF SubIFD.
    // TIFF Header(8) + IFD0 Count(2) + IFD0 Entry(12) + Next Link(4) = 26.
    *p++ = 26;
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00;

    // Offset to next IFD (0 = None)
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00;

    // EXIF SUB-IFD
    // Number of directory entries: 1 (DateTimeOriginal)
    *p++ = 0x01;
    *p++ = 0x00;

    // Exif SubIFD Entry 1: DateTimeOriginal (Tag: 0x9003, Type: 2 ASCII, Count: 20)
    *p++ = 0x03;
    *p++ = 0x90;
    *p++ = 0x02;
    *p++ = 0x00;
    *p++ = 20;
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00;

    // Value/Offset: Pointer to the actual string data.
    // Base 26 + SubIFD Count(2) + SubIFD Entry(12) + Next Link(4) = 44.
    *p++ = 44;
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00;

    // Offset to next Exif SubIFD (0 = None)
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00;
    *p++ = 0x00;

    // The date
    memcpy(p, exif_date_time_original, 20);

    return result;
}

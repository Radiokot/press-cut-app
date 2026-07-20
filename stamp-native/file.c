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

#include "file.h"

#include <stdio.h>
#include <stdlib.h>
#include <webp/demux.h>
#include <webp/mux.h>

bool read_webp_file(const char *file_path,
                    WebPData *out) {
    FILE *file = fopen(file_path, "rb");
    if (!file) {
        fprintf(stderr, "Failed to open file %s\n", file_path);
        return false;
    }

    fseek(file, 0, SEEK_END);
    const size_t file_size = ftell(file);
    fseek(file, 0, SEEK_SET);

    out->bytes = malloc(file_size);
    if (!out->bytes) {
        fclose(file);
        return false;
    }

    fread((void *) out->bytes, 1, file_size, file);
    out->size = file_size;

    fclose(file);

    return true;
}

char *get_webp_xmp(const WebPData webp_data) {
    WebPDemuxer *webp_demuxer = WebPDemux(&webp_data);
    if (!webp_demuxer) {
        fprintf(stderr, "Failed to init WebP demuxer\n");
        return NULL;
    }

    char *xmp_buffer = NULL;
    WebPChunkIterator webp_chunk_iterator;
    if (WebPDemuxGetChunk(webp_demuxer, "XMP ", 1, &webp_chunk_iterator)) {
        xmp_buffer = calloc(webp_chunk_iterator.chunk.size, 1);
        memcpy(xmp_buffer, webp_chunk_iterator.chunk.bytes, webp_chunk_iterator.chunk.size);
        WebPDemuxReleaseChunkIterator(&webp_chunk_iterator);
    }

    WebPDemuxDelete(webp_demuxer);

    return xmp_buffer;
}

bool save_webp_with_metadata(const WebPData webp_data,
                             const char *xmp_string,
                             const char *exif_bytes,
                             const size_t exif_size,
                             const char *file_path) {
    const WebPData xmp_chunk_data = {
        .bytes = (uint8_t *) xmp_string,
        .size = strlen(xmp_string),
    };

    WebPMux *webp_mux = WebPMuxCreate(&webp_data, 0);
    if (!webp_mux) {
        fprintf(stderr, "Failed to init WebP muxer\n");
        return false;
    }

    WebPMuxError set_chunk_error = WebPMuxSetChunk(webp_mux,
                                                   "XMP ",
                                                   &xmp_chunk_data,
                                                   0);
    if (set_chunk_error != WEBP_MUX_OK) {
        fprintf(stderr, "Failed to set WebP XMP chunk (%d)\n", set_chunk_error);
        WebPMuxDelete(webp_mux);
        return false;
    }

    if (exif_bytes && exif_size != 0) {
        const WebPData exif_chunk_data = {
            .bytes = (uint8_t *) exif_bytes,
            .size = exif_size,
        };
        set_chunk_error = WebPMuxSetChunk(webp_mux,
                                          "EXIF",
                                          &exif_chunk_data,
                                          0);
        if (set_chunk_error != WEBP_MUX_OK) {
            fprintf(stderr, "Failed to set WebP EXIF chunk (%d)\n", set_chunk_error);
            WebPMuxDelete(webp_mux);
            return false;
        }
    }

    WebPData assembled_webp_data;
    const WebPMuxError assembly_error = WebPMuxAssemble(webp_mux, &assembled_webp_data);
    if (assembly_error != WEBP_MUX_OK) {
        fprintf(stderr, "Failed to assemble WebP (%d)\n", assembly_error);
        WebPMuxDelete(webp_mux);
        WebPDataClear(&assembled_webp_data);
        return false;
    }

    FILE *output_file = fopen(file_path, "wb");
    if (!output_file) {
        fprintf(stderr, "Failed to open file for writing: %s\n", file_path);
        WebPMuxDelete(webp_mux);
        WebPDataClear(&assembled_webp_data);
        return false;
    }

    fwrite(assembled_webp_data.bytes, 1, assembled_webp_data.size, output_file);
    fclose(output_file);

    WebPMuxDelete(webp_mux);
    WebPDataClear(&assembled_webp_data);

    return true;
}

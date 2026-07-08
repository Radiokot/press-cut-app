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
#include <mxml.h>
#include "xmp.h"

static char *surround_xmp_with_xpacket(const char *xmp_meta_string) {
    const size_t xmp_meta_string_size = strlen(xmp_meta_string);

    const char xpacket_begin[] = "<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>";
    const size_t xpacket_begin_size = sizeof(xpacket_begin) - 1;
    const char xpacket_end[] = "<?xpacket end=\"w\"?>";
    const size_t xpacket_end_size = sizeof(xpacket_end) - 1;

    const size_t result_buffer_size = xmp_meta_string_size + xpacket_begin_size + xpacket_end_size + 1;
    char *result_buffer = malloc(result_buffer_size);

    memcpy(result_buffer, xpacket_begin, xpacket_begin_size);
    memcpy(result_buffer + xpacket_begin_size, xmp_meta_string, xmp_meta_string_size);
    memcpy(result_buffer + xpacket_begin_size + xmp_meta_string_size, xpacket_end, xpacket_end_size);
    result_buffer[result_buffer_size - 1] = 0;

    return result_buffer;
}

stamp_xmp_metadata *new_stamp_xmp_metadata() {
    stamp_xmp_metadata *metadata = malloc(sizeof(stamp_xmp_metadata));
    if (!metadata) {
        return NULL;
    }
    metadata->date_time_original = NULL;
    metadata->caption = NULL;
    metadata->shape = NULL;
    return metadata;
}

void free_stamp_xmp_metadata(const stamp_xmp_metadata *stamp_xmp_metadata) {
    if (stamp_xmp_metadata->caption) {
        free((void *) stamp_xmp_metadata->caption);
    }
    if (stamp_xmp_metadata->date_time_original) {
        free((void *) stamp_xmp_metadata->date_time_original);
    }
    if (stamp_xmp_metadata->shape) {
        free((void *) stamp_xmp_metadata->shape);
    }
    free((void *) stamp_xmp_metadata);
}

stamp_xmp_metadata *get_stamp_xmp_metadata(const char *xmp) {
    mxml_options_t *opts = mxmlOptionsNew();
    mxmlOptionsSetTypeValue(opts, MXML_TYPE_OPAQUE);

    mxml_node_t *tree = mxmlLoadString(NULL, opts, xmp);
    mxmlOptionsDelete(opts);

    if (!tree) {
        fprintf(stderr, "Failed to parse XMP\n");
        return NULL;
    }

    mxml_node_t *description = mxmlFindElement(tree,
                                               tree,
                                               "rdf:Description",
                                               NULL,
                                               NULL,
                                               MXML_DESCEND_ALL);
    if (!description) {
        fprintf(stderr, "Failed to find rdf:Description\n");
        mxmlRelease(tree);
        return NULL;
    }

    const char *caption = NULL;
    const char *date_time_original = mxmlElementGetAttr(description, "exif:DateTimeOriginal");
    const char *shape = mxmlElementGetAttr(description, "presscut:shape");

    mxml_node_t *title_node = mxmlFindElement(description,
                                              tree,
                                              "dc:title",
                                              NULL,
                                              NULL,
                                              MXML_DESCEND_ALL);
    if (title_node) {
        mxml_node_t *caption_node = mxmlFindElement(title_node,
                                                    tree,
                                                    "rdf:li",
                                                    "xml:lang",
                                                    "x-default",
                                                    MXML_DESCEND_ALL);
        if (caption_node) {
            caption = mxmlGetOpaque(caption_node);
        }
    }

    stamp_xmp_metadata *result = new_stamp_xmp_metadata();
    if (!result) {
        return NULL;
    }

    if (date_time_original) {
        const size_t size = strlen(date_time_original) + 1;
        char *copy = malloc(size);
        memcpy(copy, date_time_original, size);
        result->date_time_original = copy;
    }
    if (caption) {
        const size_t size = strlen(caption) + 1;
        char *copy = malloc(size);
        memcpy(copy, caption, size);
        result->caption = copy;
    }
    if (shape) {
        const size_t size = strlen(shape) + 1;
        char *copy = malloc(size);
        memcpy(copy, shape, size);
        result->shape = copy;
    }

    mxmlRelease(tree);

    return result;
}

char *get_xmp_with_stamp_metadata(const stamp_xmp_metadata *stamp_xmp_metadata) {
    mxml_node_t *xmp_meta = mxmlNewElement(NULL, "x:xmpmeta");
    mxmlElementSetAttr(xmp_meta, "xmlns:x", "adobe:ns:meta/");

    mxml_node_t *rdf = mxmlNewElement(xmp_meta, "rdf:RDF");
    mxmlElementSetAttr(rdf, "xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

    mxml_node_t *description = mxmlNewElement(rdf, "rdf:Description");

    if (stamp_xmp_metadata->date_time_original) {
        mxmlElementSetAttr(description, "xmlns:exif", "http://ns.adobe.com/exif/1.0/");
        mxmlElementSetAttr(description,
                           "exif:DateTimeOriginal",
                           stamp_xmp_metadata->date_time_original);
    }

    if (stamp_xmp_metadata->shape) {
        mxmlElementSetAttr(description,
                           "xmlns:presscut",
                           "https://github.com/Radiokot/press-cut-app");
        mxmlElementSetAttr(description,
                           "presscut:shape",
                           "onestamp_square");
    }

    if (stamp_xmp_metadata->caption) {
        mxmlElementSetAttr(description, "xmlns:dc", "http://purl.org/dc/elements/1.1/");
        mxml_node_t *title = mxmlNewElement(description, "dc:title");
        mxml_node_t *title_alt = mxmlNewElement(title, "rdf:Alt");
        mxml_node_t *title_alt_li = mxmlNewElement(title_alt, "rdf:li");
        mxmlElementSetAttr(title_alt_li, "xml:lang", "x-default");
        mxmlNewOpaque(title_alt_li, stamp_xmp_metadata->caption);
    }

    const char *xmp_meta_string = mxmlSaveAllocString(xmp_meta, NULL);
    char *result = surround_xmp_with_xpacket(xmp_meta_string);
    mxmlDelete(xmp_meta);

    return result;
}

stamp_collection_xmp_metadata *new_stamp_collection_xmp_metadata() {
    stamp_collection_xmp_metadata *metadata = malloc(sizeof(stamp_collection_xmp_metadata));
    if (!metadata) {
        return NULL;
    }
    metadata->name = NULL;
    return metadata;
}

void free_stamp_collection_xmp_metadata(const stamp_collection_xmp_metadata *stamp_collection_xmp_metadata) {
    if (stamp_collection_xmp_metadata->name) {
        free((void *) stamp_collection_xmp_metadata->name);
    }
    free((void *) stamp_collection_xmp_metadata);
}

stamp_collection_xmp_metadata *get_stamp_collection_xmp_metadata(const char *xmp) {
    mxml_options_t *opts = mxmlOptionsNew();
    mxmlOptionsSetTypeValue(opts, MXML_TYPE_OPAQUE);

    mxml_node_t *tree = mxmlLoadString(NULL, opts, xmp);
    mxmlOptionsDelete(opts);

    if (!tree) {
        fprintf(stderr, "Failed to parse XMP\n");
        return NULL;
    }

    mxml_node_t *description = mxmlFindElement(tree,
                                               tree,
                                               "rdf:Description",
                                               NULL,
                                               NULL,
                                               MXML_DESCEND_ALL);
    if (!description) {
        fprintf(stderr, "Failed to find rdf:Description\n");
        mxmlRelease(tree);
        return NULL;
    }

    const char *name = NULL;

    mxml_node_t *title_node = mxmlFindElement(description,
                                              tree,
                                              "dc:title",
                                              NULL,
                                              NULL,
                                              MXML_DESCEND_ALL);
    if (title_node) {
        mxml_node_t *name_node = mxmlFindElement(title_node,
                                                 tree,
                                                 "rdf:li",
                                                 "xml:lang",
                                                 "x-default",
                                                 MXML_DESCEND_ALL);
        if (name_node) {
            name = mxmlGetOpaque(name_node);
        }
    }

    stamp_collection_xmp_metadata *result = new_stamp_collection_xmp_metadata();
    if (!result) {
        return NULL;
    }

    if (!name) {
        fprintf(stderr, "Failed to find name in XMP\n");
        free(result);
        mxmlRelease(tree);
        return NULL;
    } else {
        const size_t size = strlen(name) + 1;
        char *copy = malloc(size);
        memcpy(copy, name, size);
        result->name = copy;
    }

    mxmlRelease(tree);

    return result;
}

char *get_xmp_with_stamp_collection_metadata(const stamp_collection_xmp_metadata *stamp_collection_xmp_metadata) {
    mxml_node_t *xmp_meta = mxmlNewElement(NULL, "x:xmpmeta");
    mxmlElementSetAttr(xmp_meta, "xmlns:x", "adobe:ns:meta/");

    mxml_node_t *rdf = mxmlNewElement(xmp_meta, "rdf:RDF");
    mxmlElementSetAttr(rdf, "xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

    mxml_node_t *description = mxmlNewElement(rdf, "rdf:Description");

    if (stamp_collection_xmp_metadata->name) {
        mxmlElementSetAttr(description, "xmlns:dc", "http://purl.org/dc/elements/1.1/");
        mxml_node_t *title = mxmlNewElement(description, "dc:title");
        mxml_node_t *title_alt = mxmlNewElement(title, "rdf:Alt");
        mxml_node_t *title_alt_li = mxmlNewElement(title_alt, "rdf:li");
        mxmlElementSetAttr(title_alt_li, "xml:lang", "x-default");
        mxmlNewOpaque(title_alt_li, stamp_collection_xmp_metadata->name);
    }

    const char *xmp_meta_string = mxmlSaveAllocString(xmp_meta, NULL);
    char *result = surround_xmp_with_xpacket(xmp_meta_string);
    mxmlDelete(xmp_meta);

    return result;
}

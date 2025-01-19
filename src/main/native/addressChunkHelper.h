//
// Created by Vijay Dharmaji on 17/01/25.
//

#ifndef CHUNK_HELPER_H
#define CHUNK_HELPER_H

#include <stdexcept>  // For exceptions
#include <flatbuffers/flatbuffers.h>
#include "addressChunk_generated.h"  // Include the generated FlatBuffers schema

using namespace e6::flatbuffers::addressChunk;  // Ensure correct namespace

// Structure for returning serialized data
extern "C" struct addressReturn {
    long size;
    uint8_t* address;
};


// Function declaration
addressReturn serializeChunk(flatbuffers::FlatBufferBuilder& builder,
                             std::vector<flatbuffers::Offset<void>>& vector_data,
                             std::vector<uint8_t>& types);
const Chunk* deserializeChunk(long address, long length, long position);
long* getLongArray(long address);
int* getIntArray(long address);
uint8_t* getBooleanArray(long address);

#endif // CHUNK_HELPER_H

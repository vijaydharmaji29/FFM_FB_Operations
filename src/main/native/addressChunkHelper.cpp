#include <iostream>
#include <vector>
#include <flatbuffers/flatbuffers.h>
#include "addressChunk_generated.h" // Generated header from your schema

using namespace e6::flatbuffers::addressChunk;

extern "C" struct addressReturn{
    long size;
    uint8_t* address;
};

// Function to create vectors and serialize the chunk
addressReturn serializeChunk(flatbuffers::FlatBufferBuilder& builder,
                             std::vector<flatbuffers::Offset<void>>& vector_data, std::vector<uint8_t>& types) {
    // Create the vectors table
    auto fb_vectors = builder.CreateVector(vector_data);
    auto fb_types = builder.CreateVector(types);

    // Create the chunk using ChunkBuilder
    ChunkBuilder chunk_builder(builder);
    chunk_builder.add_vectors(fb_vectors);
    chunk_builder.add_vectors_type(fb_types);
    auto chunk = chunk_builder.Finish();

    builder.Finish(chunk);

    // Get the serialized buffer
    static std::vector<uint8_t> buf;
    buf.assign(builder.GetBufferPointer(), builder.GetBufferPointer() + builder.GetSize());

    // Prepare the return struct
    addressReturn result;
    result.size = 1024;
    result.address = buf.data();

    return result;
}

const Chunk* deserializeChunk(long address, long length, long position){
    uint8_t* bufferPtr = reinterpret_cast<uint8_t*>(static_cast<uintptr_t>(address));

    // Ensure the pointer is valid
    if (!bufferPtr) {
        throw std::runtime_error("Error: Null pointer from address.\n");
    }

    // Adjust the buffer pointer to the provided position
    uint8_t* adjustedBufferPtr = bufferPtr + position;

    // Verify the buffer length
    if (length <= 0) {
        throw std::runtime_error("Error: Invalid buffer length.\n");
    }

    // Deserialize the Chunk table
    auto chunk = GetChunk(adjustedBufferPtr);

    // Ensure the deserialization was successful
    if (!chunk) {
        throw std::runtime_error("Error: Failed to deserialize Chunk.\n");
    }

    return chunk;
}

long* getLongArray(long address){
    long* longArrayPtr = reinterpret_cast<long*>(address);
    return longArrayPtr;
}

int* getIntArray(long address){
    int* intArrayPtr = reinterpret_cast<int*>(address);
    return intArrayPtr;
}

uint8_t* getBooleanArray(long address){
    uint8_t* boolArrayPtr = reinterpret_cast<uint8_t*>(address);
    return boolArrayPtr;
}
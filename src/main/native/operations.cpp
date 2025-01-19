//
// Created by Vijay Dharmaji on 19/01/25.
//
#include <iostream>
#include <vector>
#include <flatbuffers/flatbuffers.h>
#include "addressChunk_generated.h" // Generated header from your schema
#include "addressChunkHelper.h"

extern "C" addressReturn addition(long address, long length, long position){
    const Chunk* chunk = deserializeChunk(address, length, position);

    auto vector1 = chunk->vectors()->GetAs<Int64Vector>(0);
    auto vector2 = chunk->vectors()->GetAs<Int64Vector>(1);
    auto vectorSize = vector1->data_length();

    long* longArray1 = getLongArray(vector1->data());
    long* longArray2 = getLongArray(vector2->data());

    long* additionArrayData = new long[vectorSize];
    bool* additionArrayNullset = new bool[vectorSize];

    for(long i = 0; i < vectorSize; i++){
        additionArrayData[i] = longArray1[i] + longArray2[i];
        additionArrayNullset[i] = true;
    }

    // Creating and returning the addition array

    long size = 1024;
    flatbuffers::FlatBufferBuilder builder(1024);

    // Store addresses as int64_t
    int64_t int64_data_ptr = reinterpret_cast<int64_t>(additionArrayData);
    int64_t int64_nullset_ptr = reinterpret_cast<int64_t>(additionArrayNullset);

    // Create the vectors
    std::vector<uint8_t> types;
    std::vector<flatbuffers::Offset<void>> vector_data;

    // Add Int64Vector
    auto int64vec = CreateInt64Vector(builder, int64_data_ptr, int64_nullset_ptr, vectorSize);
    types.push_back(Vector_Int64Vector);
    vector_data.push_back(int64vec.Union());

    return serializeChunk(builder, vector_data, types);
}

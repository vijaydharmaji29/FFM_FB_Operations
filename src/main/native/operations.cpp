//
// Created by Vijay Dharmaji on 19/01/25.
//
#include <iostream>
#include <vector>
#include <arm_neon.h>
#include <flatbuffers/flatbuffers.h>
#include "addressChunk_generated.h" // Generated header from your schema
#include "addressChunkHelper.h"

extern "C" addressReturn matrixMultiplication(long address, long length, long position){
    const Chunk* chunk = deserializeChunk(address, length, position);

    auto vector1 = chunk->vectors()->GetAs<Int64Vector>(0);
    auto vector2 = chunk->vectors()->GetAs<Int64Vector>(1);
    auto vectorSize = vector1->data_length();

    long* longArray1 = getLongArray(vector1->data());
    long* longArray2 = getLongArray(vector2->data());

    int n = longArray1[0];
    int m = longArray1[1];
    int p = longArray2[1];

    long* longArray3 = new long[n * p + 2];
    bool* longArray3Nullset = new bool[n * p + 2];
    longArray3Nullset[0] = true;
    longArray3Nullset[1] = true;
    longArray3[0] = n;
    longArray3[1] = p;


    for (int i = 0; i < n; i++) {
        for (int j = 0; j < p; j++) {
            long sum = 0;
            for (int k = 0; k < m; k++) {
                sum += longArray1[i * m + k + 2] * longArray2[k * p + j + 2];
            }
            longArray3[i * p + j + 2] = sum;
        }
    }

    long size = 1024;
    flatbuffers::FlatBufferBuilder builder(1024);

    // Store addresses as int64_t
    int64_t int64_data_ptr = reinterpret_cast<int64_t>(longArray3);
    int64_t int64_nullset_ptr = reinterpret_cast<int64_t>(longArray3Nullset);

    // Create the vectors
    std::vector<uint8_t> types;
    std::vector<flatbuffers::Offset<void>> vector_data;

    // Add Int64Vector
    auto int64vec = CreateInt64Vector(builder, int64_data_ptr, int64_nullset_ptr, vectorSize);
    types.push_back(Vector_Int64Vector);
    vector_data.push_back(int64vec.Union());

    return serializeChunk(builder, vector_data, types);
}

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
//        additionArrayNullset[i] = true; // Do not populate the nullset
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

extern "C" addressReturn additionSIMD(long address, long length, long position) {
    const Chunk* chunk = deserializeChunk(address, length, position);

    auto vector1 = chunk->vectors()->GetAs<Int64Vector>(0);
    auto vector2 = chunk->vectors()->GetAs<Int64Vector>(1);
    auto vectorSize = vector1->data_length();

    int64_t* longArray1 = reinterpret_cast<int64_t*>(getLongArray(vector1->data()));
    int64_t* longArray2 = reinterpret_cast<int64_t*>(getLongArray(vector2->data()));

    int64_t* additionArrayData = new int64_t[vectorSize];
    bool* additionArrayNullset = new bool[vectorSize];

    // SIMD 2 elements at a time (as it's a long)
    long i;
    for (i = 0; i <= vectorSize - 2; i += 2) {
        int64x2_t va = vld1q_s64(reinterpret_cast<const int64_t*>(&longArray1[i]));  // Load 2 int64 values
        int64x2_t vb = vld1q_s64(reinterpret_cast<const int64_t*>(&longArray2[i]));  // Load 2 int64 values
        int64x2_t vc = vaddq_s64(va, vb);                                            // SIMD addition
        vst1q_s64(reinterpret_cast<int64_t*>(&additionArrayData[i]), vc);            // Store result

        additionArrayNullset[i] = true;
        additionArrayNullset[i + 1] = true;
    }

    // For remaining elements
    for (; i < vectorSize; i++) {
        additionArrayData[i] = longArray1[i] + longArray2[i];
        additionArrayNullset[i] = true;
    }

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

extern "C" bool cppCall(){
    return true;
}
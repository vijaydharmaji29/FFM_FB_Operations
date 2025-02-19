package e6.flatbuffers.addressChunk;

import com.google.flatbuffers.FlatBufferBuilder;
import sun.nio.ch.DirectBuffer;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;

public class Helper {
    public static long createLongArray(long[] array){
        // Allocate off-heap memory for the array
        MemorySegment segment = Arena.ofAuto().allocate(array.length * Long.BYTES);

        // Store each long value in the memory segment
        for (int i = 0; i < array.length; i++) {
            segment.set(ValueLayout.JAVA_LONG, i * Long.BYTES, array[i]); // Store at correct byte offset
        }

        // Return the memory address
        return segment.address();
    }

    public static long createBooleanArray(boolean[] array){
        // Allocate memory for boolean values
        MemorySegment segment = Arena.ofAuto().allocate(array.length); // Boolean values are stored as bytes

        // Store each boolean value as a byte (1 = true, 0 = false)
        for (int i = 0; i < array.length; i++) {
            segment.set(ValueLayout.JAVA_BYTE, i, (byte) (array[i] ? 1 : 0));
        }

        // Return the memory address of the allocated segment
        return segment.address();
    }

    public static int createInt64Vector(FlatBufferBuilder builder, long dataAddress, long nullsetAddress, long dataLength){
        Int64Vector.startInt64Vector(builder);
        Int64Vector.addData(builder, dataAddress);
        Int64Vector.addDataLength(builder, dataLength);
        Int64Vector.addNullset(builder, nullsetAddress);
        return Int64Vector.endInt64Vector(builder);
    }

    public static int createInt64Vector(FlatBufferBuilder builder, long[] dataVector, boolean[] nullsetVector){
        long dataAddress = createLongArray(dataVector);
        long nullsetAddress = createBooleanArray(nullsetVector);

        return createInt64Vector(builder, dataAddress, nullsetAddress, (long) dataVector.length);
    }

    public static int createInt64Vector(FlatBufferBuilder builder, long[] dataVector){
        long dataAddress = createLongArray(dataVector);

        // Create a boolean vector
        boolean[] boolVector = new boolean[dataVector.length];
        java.util.Arrays.fill(boolVector, true); // All true

        long nullsetAddress = createBooleanArray(boolVector);

        return createInt64Vector(builder, dataAddress, nullsetAddress, (long) dataVector.length);
    }

    public static long[] createChunk(FlatBufferBuilder builder, int[] vectors, byte[] types){
        int vectorsOffset = Chunk.createVectorsVector(builder, vectors);
        int typesOffset = Chunk.createVectorsTypeVector(builder, types);

        // Create the Chunk
        Chunk.startChunk(builder);
        Chunk.addVectors(builder, vectorsOffset);
        Chunk.addVectorsType(builder, typesOffset);
        int chunk = Chunk.endChunk(builder);

        // Finish the buffer
        builder.finish(chunk);

        // Create new byte buffer that persists
        ByteBuffer deserializationBuffer = builder.dataBuffer().duplicate();

        // Retrieve the address of the direct buffer
        long address = ((DirectBuffer) deserializationBuffer).address();

        // Get the direct buffer length
        long length = deserializationBuffer.limit();
        int position = deserializationBuffer.position();

        builder.clear();

        long[] ret = {address, length, position};

        return ret;
    }

    public static Chunk deserialzeChunk(long address, long size){
        // Convert the buffer to a MemorySegment
        MemorySegment bufferSegment = MemorySegment.ofAddress(address).reinterpret(size);
        // Convert to ByteBuffer
        ByteBuffer byteBuffer = bufferSegment.asByteBuffer();

        // Deserialize the root `Chunk` object
        Chunk chunk = Chunk.getRootAsChunk(byteBuffer);

        byteBuffer.clear();

        return chunk;
    }

    public static Chunk deserialzeChunk(long address, long size, long posititon){
        // Convert the buffer to a MemorySegment
        MemorySegment bufferSegment = MemorySegment.ofAddress(address).reinterpret(size);

        // Convert to ByteBuffer
        ByteBuffer byteBuffer = bufferSegment.asByteBuffer();
        byteBuffer.position((int) posititon);

        // Deserialize the root `Chunk` object
        Chunk chunk = Chunk.getRootAsChunk(byteBuffer);

        byteBuffer.clear();

        return chunk;
    }

    // Need to probably fix this
    public static Chunk deserialzeChunk(MethodHandle method) {
        // Call the function, retrieve the memory address
        long size;
        long address;

        // Use an Arena for allocation
        Arena arena = Arena.ofAuto();
        // Call function and pass allocator
        MemorySegment structSegment = null;
        try {
            structSegment = (MemorySegment) method.invoke(arena);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // Read `size` from struct (offset 0)
        size = structSegment.get(ValueLayout.JAVA_LONG, 0);

        // Read `address` (pointer to buffer at offset 8)
        MemorySegment addressSegment = structSegment.get(ValueLayout.ADDRESS, 8);
        address = addressSegment.address();

        return deserialzeChunk(address, size);
    }

    public static Chunk deserializeChunk(String path, String function) {
        var arena = Arena.ofAuto();

        // Path to the native library
        String libraryPath = path; // path to root file

        // Load the native library
        Linker linker = Linker.nativeLinker();
        SymbolLookup libraryLookup = SymbolLookup.libraryLookup(libraryPath, arena);

        // Define struct layout manually
        MemoryLayout structLayout = MemoryLayout.structLayout(
                ValueLayout.JAVA_LONG.withName("size"),
                ValueLayout.ADDRESS.withName("address")
        );

        // Get function pointer
        MethodHandle getFunctionPointer = linker.downcallHandle(
                libraryLookup.find(function).orElseThrow(),
                FunctionDescriptor.of(structLayout)
        );

        return deserialzeChunk(getFunctionPointer);
    }

    public static Chunk deserializeChunk(String path, String function, long address, long length, long position, MethodHandle getFunctionPointer) {
        long retAdress, retSize;

        // Use an Arena for allocation
        Arena arenaReturn = Arena.ofAuto();
        // Call function and pass allocator
        MemorySegment structSegment = null;
        try {
            structSegment = (MemorySegment) getFunctionPointer.invoke(arenaReturn, address, length, position);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // Read `size` from struct (offset 0)
        retSize = structSegment.get(ValueLayout.JAVA_LONG, 0);


        // Read `address` (pointer to buffer at offset 8)
        MemorySegment addressSegment = structSegment.get(ValueLayout.ADDRESS, 8);
        retAdress = addressSegment.address();

        return deserialzeChunk(retAdress, retSize);

    }

    // Helper method to read an Int64Vector
    public static long[] readInt64Vector(Int64Vector int64Vector) {
        long dataAddr = int64Vector.data();
        long nullsetAddr = int64Vector.nullset();
        long length = int64Vector.dataLength();

        long[] ret = {dataAddr, nullsetAddr, length};
        return ret;
    }

    // Helper method to read an address
    public static MemorySegment readLongAddress(long address, long length){
        MemorySegment sharedMemory = MemorySegment.ofAddress(address).reinterpret(length*Long.BYTES);
        return sharedMemory;
    }

    public static MemorySegment readBooleanAddress(long address, long length){
        MemorySegment sharedMemory = MemorySegment.ofAddress(address).reinterpret(length);
        return sharedMemory;
    }
    public static MemorySegment readIntAddress(long address, long length){
        MemorySegment sharedMemory = MemorySegment.ofAddress(address).reinterpret(length*Integer.BYTES);
        return sharedMemory;
    }

    // Helper method to read an Int32Vector
    public static long[] readInt32Vector(Int32Vector int32Vector) {
        long dataAddr = int32Vector.data();
        long nullsetAddr = int32Vector.nullset();
        long length = int32Vector.dataLength();

        long[] ret = {dataAddr, nullsetAddr, length};

        return ret;
    }


}

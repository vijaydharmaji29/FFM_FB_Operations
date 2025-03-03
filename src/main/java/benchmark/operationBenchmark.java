package benchmark;

import com.google.flatbuffers.FlatBufferBuilder;
import e6.flatbuffers.addressChunk.Chunk;
import e6.flatbuffers.addressChunk.Helper;
import e6.flatbuffers.addressChunk.Int64Vector;
import e6.flatbuffers.addressChunk.Vector;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class operationBenchmark {
    public static long javaInternalAddition(long[] array1, long[] array2){
        long[] array3 = new long[array1.length];

        for(int i = 0; i < array1.length; i++){
            array3[i] = array1[i] + array2[i];
        }

        return array3[0];
    }

    public static void javaAddition(long address, long length, long position){
        Chunk chunk = Helper.deserialzeChunk(address, length, position);

        long[] arrayProperties1 = Helper.readInt64Vector((Int64Vector) chunk.vectors(new Int64Vector(), 0));
        MemorySegment memorySegmentArray1 = Helper.readLongAddress(arrayProperties1[0], arrayProperties1[2]);

        long[] arrayProperties2 = Helper.readInt64Vector((Int64Vector) chunk.vectors(new Int64Vector(), 1));
        MemorySegment memorySegmentArray2 = Helper.readLongAddress(arrayProperties2[0], arrayProperties2[2]);

        long[] arrayData3 = new long[(int) arrayProperties2[2]];

        for(long i = 0; i < arrayProperties2[2]; i++){
            arrayData3[(int) i] = memorySegmentArray1.get(ValueLayout.JAVA_LONG, i*Long.BYTES) + memorySegmentArray2.get(ValueLayout.JAVA_LONG, i*Long.BYTES);
        }

        // Create the FlatBuffers object for it
        MemorySegment segment = Arena.ofAuto().allocate(1024);
        ByteBuffer directBuffer = segment.asByteBuffer();
        FlatBufferBuilder builder = new FlatBufferBuilder(directBuffer);

        int[] vectors = {Helper.createInt64Vector(builder, arrayData3)};
        byte[] vectorTypes = {Vector.Int64Vector};

        long[] ret = Helper.createChunk(builder, vectors, vectorTypes);

        // Deserialize the FlatBuffers memory segment and print it again
        Chunk chunkOutput = Helper.deserialzeChunk(ret[0], ret[1], ret[2]);
        long[] arrayOutputProperties = Helper.readInt64Vector((Int64Vector) chunkOutput.vectors(new Int64Vector(), 0));
        MemorySegment memorySegment = Helper.readLongAddress(arrayOutputProperties[0], arrayOutputProperties[2]);


    }
    public static void cppAddition(long address, long length, long position, MethodHandle functionPointer){
        String libraryPath = "src/main/native/liboperations.dylib";
        Chunk chunk = Helper.deserializeChunk(libraryPath, "addition", address, length, position, functionPointer);

        long[] arrayProperties = Helper.readInt64Vector((Int64Vector) chunk.vectors(new Int64Vector(), 0));
        MemorySegment memorySegment = Helper.readLongAddress(arrayProperties[0], arrayProperties[2]);

    }

    public static void cppAdditionSIMD(long address, long length, long position, MethodHandle functionPointer){
        String libraryPath = "src/main/native/liboperations.dylib";
        Chunk chunk = Helper.deserializeChunk(libraryPath, "additionSIMD", address, length, position, functionPointer);

        long[] arrayProperties = Helper.readInt64Vector((Int64Vector) chunk.vectors(new Int64Vector(), 0));
        MemorySegment memorySegment = Helper.readLongAddress(arrayProperties[0], arrayProperties[2]);

    }

    public static boolean cppCall(MethodHandle functionPointer){

        // Call function and pass allocator
        boolean result;
        try {
            result = (boolean) functionPointer.invokeExact();

            return result;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public static long[] cppDirectAddition(long[] a, long[] b, MethodHandle functionPointer) {

        int size = a.length;

        // Allocate memory segments for the arrays
        Arena arena = Arena.ofAuto();
        MemorySegment aSegment = arena.allocateArray(ValueLayout.JAVA_LONG, a);
        MemorySegment bSegment = arena.allocateArray(ValueLayout.JAVA_LONG, b);
        MemorySegment resultSegment = arena.allocateArray(ValueLayout.JAVA_LONG, size);

        // Call the C++ function
        try {
            functionPointer.invokeExact(aSegment, bSegment, resultSegment, (long) size);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // Retrieve the results
        long[] result = resultSegment.toArray(ValueLayout.JAVA_LONG);

        return result;
    }

    public static void main(String args[]){
        //create 2 vectors
        int size = 10;
        long[] exampleArray1 = new long[size];
        long[] exampleArray2 = new long[size];

        for(int i = 0; i < size; i++){
            exampleArray1[i] = i * 10L;
            exampleArray2[i] = i * 10L;
        }

    }
}

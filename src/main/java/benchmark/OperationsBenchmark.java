package benchmark;

import com.google.flatbuffers.FlatBufferBuilder;
import e6.flatbuffers.addressChunk.Helper;
import e6.flatbuffers.addressChunk.Vector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 10)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 5)
public class OperationsBenchmark {

    private long[] exampleArray1;
    private long[] exampleArray2;
    private long address;
    private long length;
    private long position;
    Arena arenaCppCall;
    MethodHandle functionPointer;
    MethodHandle functionPointerCPPAddition;
    MethodHandle functionPointerCPPAdditionSIMD;

    MethodHandle functionPointerDirectAddition;

    @Setup (Level.Trial)
    public void setup() {
        // Initialize arrays
        int size = 10000;
        exampleArray1 = new long[size];
        exampleArray2 = new long[size];

        for(int i = 0; i < size; i++) {
            exampleArray1[i] = i * 10L;
            exampleArray2[i] = i * 10L;
        }

        // Create FlatBuffer data
        MemorySegment segment = Arena.ofConfined().allocate(1024);
        ByteBuffer directBuffer = segment.asByteBuffer();
        FlatBufferBuilder builder = new FlatBufferBuilder(directBuffer);

        int[] vectors = {
                Helper.createInt64Vector(builder, exampleArray1),
                Helper.createInt64Vector(builder, exampleArray2)
        };
        byte[] vectorTypes = {Vector.Int64Vector, Vector.Int64Vector};

        long[] ret = Helper.createChunk(builder, vectors, vectorTypes);
        address = ret[0];
        length = ret[1];
        position = ret[2];


        String libraryPath = "src/main/native/liboperations.dylib";
        String function = "cppCall";

        var arenaCppCall = Arena.ofAuto();

        // Load the native library
        Linker linker = Linker.nativeLinker();
        SymbolLookup libraryLookup = SymbolLookup.libraryLookup(libraryPath, arenaCppCall);

        // Get function pointer
        functionPointer = linker.downcallHandle(
                libraryLookup.find(function).orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN)
        );

        // Load the native library
        // Define struct layout manually
        MemoryLayout structLayout = MemoryLayout.structLayout(
                ValueLayout.JAVA_LONG.withName("size"),
                ValueLayout.ADDRESS.withName("address")
        );
        function = "addition";
        // Get function pointer
        functionPointerCPPAddition = linker.downcallHandle(
                libraryLookup.find(function).orElseThrow(),
                FunctionDescriptor.of(structLayout, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        );

        // Get function pointer for CPP Addition SIMD
        function = "additionSIMD";
        functionPointerCPPAdditionSIMD = linker.downcallHandle(
                libraryLookup.find(function).orElseThrow(),
                FunctionDescriptor.of(structLayout, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        );


        function = "add_long_arrays";
        functionPointerDirectAddition = linker.downcallHandle(
                libraryLookup.find(function).orElseThrow(),
                FunctionDescriptor.ofVoid( // Function returns void
                        ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_LONG), // Pointer to long[] A
                        ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_LONG), // Pointer to long[] B
                        ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_LONG), // Pointer to result long[]
                        ValueLayout.JAVA_LONG // Size of the arrays
                )
        );


    }

//    @Benchmark
//    public long benchmarkJavaInternalAddition() {
//        return operationBenchmark.javaInternalAddition(exampleArray1, exampleArray2);
//    }

    @Benchmark
    public long[] benchmarkCppAdditionDirect(){
        return operationBenchmark.cppDirectAddition(exampleArray1, exampleArray2, functionPointerDirectAddition);
    }

//    @Benchmark
//    public void benchmarkJavaAddition() {
//        operationBenchmark.javaAddition(address, length, position);
//    }

//    @Benchmark
//    public void benchmarkCppAddition() {
//        operationBenchmark.cppAddition(address, length, position, functionPointerCPPAddition);
//    }

//    @Benchmark
//    public void benchmarkCppAdditionSIMD() {
//        operationBenchmark.cppAdditionSIMD(address, length, position, functionPointerCPPAdditionSIMD);
//    }

//    @Benchmark
//    public void benchmarkCppCall(){
//        operationBenchmark.cppCall(functionPointer);
//    }



    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(OperationsBenchmark.class.getSimpleName())
                .jvmArgs(
                "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
                "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                "--enable-preview",
                "-XX:MaxDirectMemorySize=16G", // Increase direct memory
                "-Xmx4G" // Increase heap size
                )
                .forks(1)
                .threads(1)
                .build();
        new Runner(opt).run();
    }
}
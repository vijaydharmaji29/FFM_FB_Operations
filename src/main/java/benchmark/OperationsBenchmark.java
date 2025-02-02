package benchmark;

import com.google.flatbuffers.FlatBufferBuilder;
import e6.flatbuffers.addressChunk.Helper;
import e6.flatbuffers.addressChunk.Vector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class OperationsBenchmark {

    private long[] exampleArray1;
    private long[] exampleArray2;
    private long address;
    private long length;
    private long position;

    @Setup
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
    }

    @Benchmark
    public void benchmarkJavaInternalAddition() {
        operationBenchmark.javaInternalAddition(exampleArray1, exampleArray2);
    }

    @Benchmark
    public void benchmarkJavaAddition() {
        operationBenchmark.javaAddition(address, length, position);
    }

    @Benchmark
    public void benchmarkCppAddition() {
        operationBenchmark.cppAddition(address, length, position);
    }

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
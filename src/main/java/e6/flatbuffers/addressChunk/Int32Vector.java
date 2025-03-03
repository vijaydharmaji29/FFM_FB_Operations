// automatically generated by the FlatBuffers compiler, do not modify

package e6.flatbuffers.addressChunk;

import com.google.flatbuffers.BaseVector;
import com.google.flatbuffers.BooleanVector;
import com.google.flatbuffers.ByteVector;
import com.google.flatbuffers.Constants;
import com.google.flatbuffers.DoubleVector;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FloatVector;
import com.google.flatbuffers.IntVector;
import com.google.flatbuffers.LongVector;
import com.google.flatbuffers.ShortVector;
import com.google.flatbuffers.StringVector;
import com.google.flatbuffers.Struct;
import com.google.flatbuffers.Table;
import com.google.flatbuffers.UnionVector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("unused")
public final class Int32Vector extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_24_3_25(); }
  public static Int32Vector getRootAsInt32Vector(ByteBuffer _bb) { return getRootAsInt32Vector(_bb, new Int32Vector()); }
  public static Int32Vector getRootAsInt32Vector(ByteBuffer _bb, Int32Vector obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public Int32Vector __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public long data() { int o = __offset(4); return o != 0 ? bb.getLong(o + bb_pos) : 0L; }
  public long nullset() { int o = __offset(6); return o != 0 ? bb.getLong(o + bb_pos) : 0L; }
  public long dataLength() { int o = __offset(8); return o != 0 ? bb.getLong(o + bb_pos) : 0L; }

  public static int createInt32Vector(FlatBufferBuilder builder,
      long data,
      long nullset,
      long dataLength) {
    builder.startTable(3);
    Int32Vector.addDataLength(builder, dataLength);
    Int32Vector.addNullset(builder, nullset);
    Int32Vector.addData(builder, data);
    return Int32Vector.endInt32Vector(builder);
  }

  public static void startInt32Vector(FlatBufferBuilder builder) { builder.startTable(3); }
  public static void addData(FlatBufferBuilder builder, long data) { builder.addLong(0, data, 0L); }
  public static void addNullset(FlatBufferBuilder builder, long nullset) { builder.addLong(1, nullset, 0L); }
  public static void addDataLength(FlatBufferBuilder builder, long dataLength) { builder.addLong(2, dataLength, 0L); }
  public static int endInt32Vector(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public Int32Vector get(int j) { return get(new Int32Vector(), j); }
    public Int32Vector get(Int32Vector obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}


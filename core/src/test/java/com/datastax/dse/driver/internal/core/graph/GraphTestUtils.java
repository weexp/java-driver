/*
 * Copyright DataStax, Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.driver.internal.core.graph;

import com.datastax.dse.driver.api.core.DseProtocolVersion;
import com.datastax.dse.driver.internal.core.context.DseDriverContext;
import com.datastax.dse.driver.internal.core.graph.binary.GraphBinaryModule;
import com.datastax.dse.protocol.internal.response.result.DseRowsMetadata;
import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableList;
import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableMap;
import com.datastax.oss.protocol.internal.Frame;
import com.datastax.oss.protocol.internal.Message;
import com.datastax.oss.protocol.internal.ProtocolConstants;
import com.datastax.oss.protocol.internal.response.result.ColumnSpec;
import com.datastax.oss.protocol.internal.response.result.DefaultRows;
import com.datastax.oss.protocol.internal.response.result.RawType;
import com.datastax.oss.protocol.internal.response.result.Rows;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import org.apache.tinkerpop.gremlin.process.remote.traversal.DefaultRemoteTraverser;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.Buffer;
import org.apache.tinkerpop.gremlin.structure.io.binary.GraphBinaryReader;
import org.apache.tinkerpop.gremlin.structure.io.binary.GraphBinaryWriter;
import org.apache.tinkerpop.gremlin.structure.io.binary.TypeSerializerRegistry;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;

public class GraphTestUtils {
  public static ByteBuffer serialize(
      Object value, GraphProtocol graphProtocol, GraphBinaryModule graphBinaryModule)
      throws IOException {

    Buffer tinkerBuf = graphBinaryModule.serialize(value);
    ByteBuffer nioBuffer = tinkerBuf.nioBuffer();
    tinkerBuf.release();
    return graphProtocol.isGraphBinary()
        ? nioBuffer
        : GraphSONUtils.serializeToByteBuffer(value, graphProtocol);
  }

  public static Frame defaultDseFrameOf(Message responseMessage) {
    return Frame.forResponse(
        DseProtocolVersion.DSE_V2.getCode(),
        0,
        null,
        Frame.NO_PAYLOAD,
        Collections.emptyList(),
        responseMessage);
  }

  public static Message singleGraphRow(
      GraphProtocol graphProtocol, Object value, GraphBinaryModule module) throws IOException {
    return singleGraphRow(graphProtocol, value, module, 1, true);
  }

  // Returns a single row, with a single "message" column containing the value
  // given in parameter serialized according to the protocol
  public static Message singleGraphRow(
      GraphProtocol graphProtocol, Object value, GraphBinaryModule module, int page, boolean isLast)
      throws IOException {
    DseRowsMetadata metadata =
        new DseRowsMetadata(
            ImmutableList.of(
                new ColumnSpec(
                    "ks",
                    "table",
                    "gremlin",
                    0,
                    graphProtocol.isGraphBinary()
                        ? RawType.PRIMITIVES.get(ProtocolConstants.DataType.BLOB)
                        : RawType.PRIMITIVES.get(ProtocolConstants.DataType.VARCHAR))),
            null,
            new int[] {},
            null,
            page,
            isLast);
    Queue<List<ByteBuffer>> data = new ArrayDeque<>();

    data.add(
        ImmutableList.of(
            serialize(
                graphProtocol.isGraphBinary()
                    // GraphBinary returns results directly inside a Traverser
                    ? new DefaultRemoteTraverser<>(value, 1)
                    : ImmutableMap.of("result", value),
                graphProtocol,
                module)));
    return new DefaultRows(metadata, data);
  }

  // Returns 10 rows, each with a vertex
  public static Rows tenGraphRows(
      GraphProtocol graphProtocol, GraphBinaryModule module, int page, boolean last)
      throws IOException {
    DseRowsMetadata metadata =
        new DseRowsMetadata(
            ImmutableList.of(
                new ColumnSpec(
                    "ks",
                    "table",
                    "gremlin",
                    0,
                    graphProtocol.isGraphBinary()
                        ? RawType.PRIMITIVES.get(ProtocolConstants.DataType.BLOB)
                        : RawType.PRIMITIVES.get(ProtocolConstants.DataType.VARCHAR))),
            null,
            new int[] {},
            null,
            page,
            last);
    Queue<List<ByteBuffer>> data = new ArrayDeque<>();
    int start = (page - 1) * 10;
    for (int i = start; i < start + 10; i++) {
      Vertex v =
          DetachedVertex.build()
              .setId("vertex" + i)
              .setLabel("person")
              .addProperty(
                  DetachedVertexProperty.build()
                      .setId("property" + i)
                      .setLabel("name")
                      .setValue("user" + i)
                      .create())
              .create();
      data.add(
          ImmutableList.of(
              serialize(
                  graphProtocol.isGraphBinary()
                      // GraphBinary returns results directly inside a Traverser
                      ? new DefaultRemoteTraverser<>(v, 1)
                      : ImmutableMap.of("result", v),
                  graphProtocol,
                  module)));
    }
    return new DefaultRows(metadata, data);
  }

  public static GraphBinaryModule createGraphBinaryModule(DseDriverContext context) {
    TypeSerializerRegistry registry = GraphBinaryModule.createDseTypeSerializerRegistry(context);
    return new GraphBinaryModule(new GraphBinaryReader(registry), new GraphBinaryWriter(registry));
  }
}
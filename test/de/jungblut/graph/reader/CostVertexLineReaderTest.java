package de.jungblut.graph.reader;

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

import com.google.common.base.Optional;

import de.jungblut.graph.Graph;
import de.jungblut.graph.model.Vertex;

public class CostVertexLineReaderTest extends TestCase {

  @Test
  public void testReader() throws IOException {
    CostVertexLineReader reader = new CostVertexLineReader(
        "res/weighted_graph/edges.txt", ' ', 1, true);
    Optional<Graph<Integer, Integer, Integer>> absent = Optional.absent();
    Graph<Integer, Integer, Integer> graph = reader.readGraph(absent);

    Vertex<Integer, Integer> vertex = graph.getVertex(1);
    assertNotNull(vertex);

    assertEquals(500, graph.getNumVertices());

    assertEquals(2184 * 2, graph.getNumEdges());

  }
}

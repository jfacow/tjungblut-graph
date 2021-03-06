package de.jungblut.graph.bsp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.HamaConfiguration;
import org.apache.hama.bsp.HashPartitioner;
import org.apache.hama.bsp.TextInputFormat;
import org.apache.hama.bsp.TextOutputFormat;
import org.apache.hama.graph.GraphJob;
import org.junit.Test;

import de.jungblut.graph.Graph;
import de.jungblut.graph.TestGraphProvider;
import de.jungblut.graph.bsp.SSSP.IntIntPairWritable;
import de.jungblut.graph.bsp.SSSP.SSSPTextReader;
import de.jungblut.graph.bsp.SSSP.ShortestPathVertex;
import de.jungblut.graph.model.Edge;
import de.jungblut.graph.model.Vertex;

public final class SSSPTest extends TestCase {

  @Test
  public void testSSSP() throws Exception {

    // Graph job configuration
    HamaConfiguration conf = new HamaConfiguration();
    conf.set("bsp.local.tasks.maximum", "2");
    GraphJob ssspJob = new GraphJob(conf, SSSP.class);
    // Set the job name
    ssspJob.setJobName("Single Source Shortest Path");
    FileSystem fs = FileSystem.get(conf);
    Path in = new Path("/tmp/sssp/input.txt");
    createInput(fs, in);
    Path out = new Path("/tmp/sssp/out/");
    if (fs.exists(out)) {
      fs.delete(out, true);
    }
    conf.set(SSSP.START_VERTEX, "0");
    ssspJob.setNumBspTask(2);
    ssspJob.setInputPath(in);
    ssspJob.setOutputPath(out);

    ssspJob.setVertexClass(ShortestPathVertex.class);
    ssspJob.setInputFormat(TextInputFormat.class);
    ssspJob.setInputKeyClass(LongWritable.class);
    ssspJob.setInputValueClass(Text.class);

    ssspJob.setPartitioner(HashPartitioner.class);
    ssspJob.setOutputFormat(TextOutputFormat.class);
    ssspJob.setVertexInputReaderClass(SSSPTextReader.class);
    ssspJob.setOutputKeyClass(IntWritable.class);
    ssspJob.setOutputValueClass(IntIntPairWritable.class);
    // Iterate until all the nodes have been reached.
    ssspJob.setMaxIteration(Integer.MAX_VALUE);

    ssspJob.setVertexIDClass(IntWritable.class);
    ssspJob.setVertexValueClass(IntIntPairWritable.class);
    ssspJob.setEdgeValueClass(IntWritable.class);

    long startTime = System.currentTimeMillis();
    if (ssspJob.waitForCompletion(true)) {
      System.out.println("Job Finished in "
          + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
      verifyOutput(fs, out);
    }
  }

  private void createInput(FileSystem fs, Path in) throws IOException {
    if (fs.exists(in)) {
      fs.delete(in, true);
    }

    @SuppressWarnings("resource")
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
        fs.create(in)));

    Graph<Integer, String, Integer> wikipediaExampleGraph = TestGraphProvider
        .getWikipediaExampleGraph();
    for (Vertex<Integer, String> v : wikipediaExampleGraph.getVertexSet()) {
      Set<Edge<Integer, Integer>> adjacentVertices = wikipediaExampleGraph
          .getEdges(v.getVertexId());
      writer.write(v.getVertexId() + "\t" + toString(adjacentVertices));
      writer.write('\n');
    }
    writer.close();
  }

  private String toString(Set<Edge<Integer, Integer>> adjacentVertices) {
    StringBuilder sb = new StringBuilder();
    for (Edge<Integer, Integer> v : adjacentVertices) {
      sb.append(v.getDestinationVertexID());
      sb.append(':');
      sb.append(v.getValue());
      sb.append('\t');
    }
    return sb.toString();
  }

  private void verifyOutput(FileSystem fs, Path out) throws IOException {
    int[] costResult = new int[10];
    int[] ancestorResult = new int[10];
    int[] costs = new int[] { 0, 85, 217, 503, 173, 165, 403, 320, 415, 487 };
    int[] ancestors = new int[] { 0, 0, 0, 7, 0, 1, 2, 2, 5, 7 };
    FileStatus[] status = fs.listStatus(out);
    for (FileStatus fss : status) {
      @SuppressWarnings("resource")
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          fs.open(fss.getPath())));

      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
        String[] split = line.split("\t");
        costResult[Integer.parseInt(split[0])] = Integer.parseInt(split[2]);
        ancestorResult[Integer.parseInt(split[0])] = Integer.parseInt(split[1]);
      }
      reader.close();
    }

    for (int i = 0; i < ancestorResult.length; i++) {
      assertEquals(costs[i], costResult[i]);
      assertEquals(ancestors[i], ancestorResult[i]);
    }

  }
}

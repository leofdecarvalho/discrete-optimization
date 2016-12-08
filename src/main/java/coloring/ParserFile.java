package coloring;

import input.HandleFile;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.util.List;

/**
 * Created by Leo on 08/12/2016.
 */
@Data
class ParserFile {
    private String[] args;
    private int nodesSize;
    private int edgesSize;
    private int[][] edges;
    private boolean[][] matrixEdges;

    public ParserFile(String... args) {
        this.args = args;
    }

    public boolean[][] getMatrixEdges() {
        return matrixEdges;
    }

    public ParserFile invoke() throws IOException {
        List<String> lines = HandleFile.getLines(args);

        String[] firstLine = lines.get(0).split("\\s+");
        nodesSize = Integer.parseInt(firstLine[0]);
        edgesSize = Integer.parseInt(firstLine[1]);

        edges = new int[edgesSize][2];
        matrixEdges = new boolean[nodesSize][nodesSize];

        for (int i = 1; i <= edgesSize; i++) {
            String line = lines.get(i);
            String[] parts = line.split("\\s+");

            int e0 = Integer.parseInt(parts[0]);
            int e1 = Integer.parseInt(parts[1]);
            edges[i-1][0] = e0;
            edges[i-1][1] = e1;
            matrixEdges[e0][e1] = true;
            matrixEdges[e1][e0] = true;
        }
        return this;
    }
}

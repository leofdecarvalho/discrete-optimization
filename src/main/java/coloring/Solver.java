package coloring;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


import input.HandleFile;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;


public class Solver {

    private static int timeout = 4 * 60;

    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void solve(String[] args) throws IOException {

        ParserFile parserFile = new ParserFile(args).invoke();

        int nodesSize = parserFile.getNodesSize();
        boolean[][] matrixEdges = parserFile.getMatrixEdges();
        int edgesSize = parserFile.getEdgesSize();
        int[][] edges = parserFile.getEdges();

        new SolverColoring(nodesSize, edgesSize, edges, matrixEdges).solve();
    }


}

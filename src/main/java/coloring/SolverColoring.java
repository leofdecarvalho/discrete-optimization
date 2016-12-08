package coloring;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Leo on 08/12/2016.
 */
@Data
@AllArgsConstructor
public class SolverColoring {

    private int nodesSize;
    private int edgesSize;
    private int[][] edges;
    private boolean[][] matrixEdges;

    private static int TIME_LIMITE_SECONDS = 200;

    public void solve() {
        //Create model
        Model model = new Model();

        // Add objective function
        Objective objective = new Objective(model).addObjective();
        IntVar[] nodesColor = objective.getNodesColor();
        Integer[] orderedNodesByDegrees = objective.getOrderedNodesByDregrees();

        // Add constraints
        new Constraints(orderedNodesByDegrees, model, nodesColor).addConstraints();

        //Configure solver
        org.chocosolver.solver.Solver solver = model.getSolver();
        solver.limitTime(TIME_LIMITE_SECONDS * 1000);
        solver.setSearch(Search.minDomLBSearch(nodesColor));
        final Solution solutionVar = new Solution(model, nodesColor);
        final Solution solutionObjective = new Solution(model, objective.getObjectiveExpression());
        solver.plugMonitor((IMonitorSolution) () -> {
            solutionObjective.record();
            solutionVar.record();
        });

        //Optimize
        solver.findAllOptimalSolutions(objective.getObjectiveExpression(), Model.MINIMIZE);

        //Print
        printSolution(nodesColor, objective.getObjectiveExpression(), solutionVar, solutionObjective);
    }

    private void printSolution(IntVar[] nodesColor, IntVar objective, Solution solutionVar, Solution solutionObjective) {
        try {
            System.out.println((solutionObjective.getIntVal(objective) + 1) + " 1");
            for (int i = 0; i < nodesSize; i++) {
                System.out.print(solutionVar.getIntVal(nodesColor[i]) + " ");
            }
            System.out.println();
        }  catch (SolverException e) {
            System.out.println("Solution not founded.");
        }
    }



    @Data
    private class Objective {

        private Integer[] orderedNodesByDregrees;
        private int node_with_highest_degree;
        private Model model;
        private IntVar[] nodesColor;
        private IntVar objectiveExpression;

        public Objective(Model model) {
            this.model = model;
        }

        public Objective addObjective() {

            orderedNodesByDregrees = getNodeOrderedByDegree(nodesSize, matrixEdges);
            node_with_highest_degree = orderedNodesByDregrees[0];

            nodesColor = new IntVar[nodesSize];
            for (int i = 0; i < nodesSize; i++) {
                if (i == node_with_highest_degree) { //Select the with highest degree
                    nodesColor[i] = model.intVar("node:0",0, 0);
                } else if (matrixEdges[node_with_highest_degree][i]) { //Select the adjacent to the highest degree
                    nodesColor[i] = model.intVar("node:" + i, 1, nodesSize - 1);
                } else { //Select other nodes
                    nodesColor[i] = model.intVar("node:" + i, 0, nodesSize - 1);
                }
            }

            //Minimize the largest node color
            objectiveExpression = model.intVar("largest_color", 0, nodesSize - 1);
            objectiveExpression.eq(nodesColor[0].max(nodesColor)).post();
            model.setObjective(Model.MINIMIZE, objectiveExpression);

            return this;
        }

        private Integer[] getNodeOrderedByDegree(int nodesSize, boolean[][] matrixEdges) {
            Integer[] nodes = new Integer[nodesSize];
            final int[] degree = new int[nodesSize];
            for (int i = 0; i < nodesSize; i++) {
                nodes[i] = i;
                for (int j = 0; j < nodesSize; j++)
                    if (matrixEdges[i][j])
                        degree[i]++;
            }
            Comparator<Integer> c = (a, b) -> (degree[b] - degree[a]);
            Arrays.sort(nodes, c);
            return nodes;
        }
    }

    @Data
    @AllArgsConstructor
    private class Constraints {
        private Integer[] orderedNodesByDregree;
        private Model model;
        private IntVar[] nodesColor;


        public void addConstraints() {
            // Colors of adjacent nodes must be different
            for (int i = 0; i < edgesSize; i++) {
                model.arithm(nodesColor[edges[i][0]], "!=",nodesColor[edges[i][1]]).post();
            }

            // Handle symmetry
            for (int i = 1; i < nodesSize; i++) {
                final IntVar[] vector = new IntVar[i];
                for (int j = 0; j < i; j++) {
                    vector[j] = nodesColor[orderedNodesByDregree[j]];
                }
                nodesColor[orderedNodesByDregree[i]].le(vector[0].max(vector).add(1)).post();
            }
        }
    }
}

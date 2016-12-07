package facility;

import gurobi.*;
import input.HandleFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by Leo on 04/12/2016.
 */
public class Solver {


    public static Double getDistance(Point i, Point j) {
        double x1 = i.getX();
        double y1 = i.getY();
        double x2 = j.getX();
        double y2 = j.getY();
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static void main(String[] args) {

        ParserFile parserFile = new ParserFile(args).invoke();
        int nFacilities = parserFile.getnFacilities();
        List<Facility> facilities = parserFile.getFacilities();
        int nCostumers = parserFile.getnCostumers();
        List<Costumer> costumers = parserFile.getCostumers();

        try {

            // Model
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "facility");

            // Plant open decision variables: open[p] == 1 if plant p is open.
            GRBVar[] x = new GRBVar[nFacilities];
            for (int p = 0; p < nFacilities; ++p) {
                x[p] = model.addVar(0, 1, facilities.get(p).getSetupCost(), GRB.BINARY, "x[" + p + "]");
            }


            // Transportation decision variables: y[w][p] = if product is transported from plant p to costumer w
            GRBVar[][] y = new GRBVar[nCostumers][nFacilities];
            for (int w = 0; w < nCostumers; ++w) {
                for (int p = 0; p < nFacilities; ++p) {
                    y[w][p] =
                            model.addVar(0,1, getDistance(costumers.get(w).getPosition(), facilities.get(p).getPosition()), GRB.BINARY,
                                    "y[" + w + "][" + p + "]");
                }

            }

            // Update model to integrate new variables
            model.update();

            // Demand constraints
            for (int w = 0; w < nCostumers; ++w) {
                for (int p = 0; p < nFacilities; ++p) {
                    model.addConstr(y[w][p], GRB.LESS_EQUAL, x[p], "y(" + w + "," + p + ") <= " + "x(" + p + ")");
                }
            }

            // Capacity vs Demand  constraints
            for (int p = 0; p < nFacilities; ++p) {
                GRBLinExpr ptot = new GRBLinExpr();
                for (int w = 0; w < nCostumers; ++w) {
                    ptot.addTerm(costumers.get(w).getDemand(), y[w][p]);
                }
                GRBLinExpr limit = new GRBLinExpr();
                limit.addTerm(facilities.get(p).getCapacity(), x[p]);
                model.addConstr(ptot, GRB.LESS_EQUAL, limit, "S(demand, " +p + ") <= "+ "Capacity[" + p + "]");
            }

            // Y constraints
            for (int w = 0; w < nCostumers; ++w) {
                GRBLinExpr dtot = new GRBLinExpr();
                for (int p = 0; p < nFacilities; ++p) {
                    dtot.addTerm(1, y[w][p]);
                }
                model.addConstr(dtot, GRB.EQUAL, 1, "S(y, " + w + ") = 1");
            }

            // Guess at the starting point: close the plant with the highest
            // fixed costs; open all others

            // First, open all plants
            for (int p = 0; p < nFacilities; ++p) {
                x[p].set(GRB.DoubleAttr.Start, 1.0);
            }

            // Now close the plant with the highest fixed cost
            //System.out.println("Initial guess:");
            double maxFixed = -GRB.INFINITY;
            for (int p = 0; p < nFacilities; ++p) {
                if (facilities.get(p).getSetupCost() > maxFixed) {
                    maxFixed = facilities.get(p).getSetupCost();
                }
            }
            for (int p = 0; p < nFacilities; ++p) {
                if (facilities.get(p).getSetupCost() == maxFixed) {
                    x[p].set(GRB.DoubleAttr.Start, 0.0);
                    break;
                }
            }

            // The objective is to minimize the total fixed and variable costs
            model.set(GRB.IntAttr.ModelSense, 1);

            // Use barrier to solve root relaxation
            model.getEnv().set(GRB.IntParam.Method, GRB.METHOD_BARRIER);

            // Solve
            model.getEnv().set(GRB.IntParam.OutputFlag, 0);

            //model.write("Facility.lp");

            model.getEnv().set(GRB.DoubleParam.TimeLimit, 200);
            model.optimize();

           // model.computeIIS();
            //model.write("Facility.ilp");

            // Print solution
            System.out.println(model.get(GRB.DoubleAttr.ObjVal) + " " + 1);
            int k = 1;
            for (int w = 0; w < nCostumers; ++w) {
                for (int p = 0; p < nFacilities; ++p) {
                    if (x[p].get(GRB.DoubleAttr.X) == 1.0 && y[w][p].get(GRB.DoubleAttr.X) > 0.0001) {
                        System.out.print(p + " ");
                    }
                }
            }


        /*    System.out.println();
            for (int p = 0; p < nFacilities; ++p) {
                if (x[p].get(GRB.DoubleAttr.X) == 1.0) {
                    System.out.println("Plant " + p + " open:");
                    for (int w = 0; w < nCostumers; ++w) {
                        if(y[w][p].get(GRB.DoubleAttr.X) > 0.0001) {
                            System.out.println("  Transport " +
                                    y[w][p].get(GRB.DoubleAttr.X)*facilities.get(p).getCapacity() +
                                    " units to warehouse " + w + " d:"  +  getDistance(costumers.get(w).getPosition(), facilities.get(p).getPosition()) + " y: " + y[w][p].get(GRB.DoubleAttr.X));
                        }
                    }
                }
            }*/

            // Dispose of model and environment
            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
    }

    private static class ParserFile {
        private String[] args;
        private int nFacilities;
        private int nCostumers;
        private List<Facility> facilities;
        private List<Costumer> costumers;

        public ParserFile(String... args) {
            this.args = args;
        }

        public int getnFacilities() {
            return nFacilities;
        }

        public int getnCostumers() {
            return nCostumers;
        }

        public List<Facility> getFacilities() {
            return facilities;
        }

        public List<Costumer> getCostumers() {
            return costumers;
        }

        public ParserFile invoke() {
            List<String> lines = new ArrayList<>();

            try {
                lines = HandleFile.getLines(args);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] firstLine = lines.get(0).split("\\s+");
            nFacilities = Integer.parseInt(firstLine[0]);
            nCostumers = Integer.parseInt(firstLine[1]);
            int i= 1;
            facilities = new ArrayList<>();
            for (; i <= nFacilities; i++) {
                String[] line = lines.get(i).split("\\s+");
                facilities.add(new Facility(i,
                        Double.parseDouble(line[0]),
                        Double.parseDouble(line[1]),
                        new Point(Double.parseDouble(line[2]), Double.parseDouble(line[3]))
                                ));
            }
            costumers = new ArrayList<>();
            for (; i <= nFacilities + nCostumers; i++) {
                String[] line = lines.get(i).split("\\s+");
                costumers.add(new Costumer(i,
                        Double.parseDouble(line[0]),
                        new Point(Double.parseDouble(line[1]), Double.parseDouble(line[2]))
                ));
            }
            return this;
        }
    }
}

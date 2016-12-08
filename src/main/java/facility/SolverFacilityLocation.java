package facility;

import gurobi.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Created by Leo on 08/12/2016.
 */
@Data
@AllArgsConstructor
public class SolverFacilityLocation {

    private int nFacilities;
    private int nCostumers;
    private List<Facility> facilities;
    private List<Costumer> costumers;

    private static int TIME_LIMIT_SECONDS = 200;

    public Double getDistance(Point i, Point j) {
        double x1 = i.getX();
        double y1 = i.getY();
        double x2 = j.getX();
        double y2 = j.getY();
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public void solve() {
        try {

            // Create Model
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "facility");

            //Add objectiveExpression
            Objective objective = new Objective(model).addObjective();
            GRBVar[][] y = objective.getY();
            GRBVar[] x = objective.getX();
            //Add constraints
            new Constraints(model, y, x).addConstraints();
            //Add starting point to optimization
            new StartingPoint(x).defineGuess();


            //Configure solver
            model.getEnv().set(GRB.IntParam.Method, GRB.METHOD_BARRIER);  // Use barrier to solve root relaxation
            model.getEnv().set(GRB.IntParam.OutputFlag, 0); // Disable gurobi logs
            //model.write("Facility.lp"); //Used to print model in a file
            model.getEnv().set(GRB.DoubleParam.TimeLimit, TIME_LIMIT_SECONDS);  //Define time limit optimization

            model.optimize();

            // model.computeIIS(); //Used to debug solution infeasible
            //model.write("Facility.ilp"); //Used to IIS in a file

            // Print solution
            printSolution(model, y, x);

            //debugSolution(model, y, x);

            // Dispose of model and environment
            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
    }

    private void debugSolution(GRBModel model, GRBVar[][] y, GRBVar[] x) throws GRBException {
        System.out.println(model.get(GRB.DoubleAttr.ObjVal) + " " + 1);
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
        }
    }

    private void printSolution(GRBModel model, GRBVar[][] y, GRBVar[] x) throws GRBException {
        System.out.println(model.get(GRB.DoubleAttr.ObjVal) + " " + 1);
        int k = 1;
        for (int w = 0; w < nCostumers; ++w) {
            for (int p = 0; p < nFacilities; ++p) {
                if (x[p].get(GRB.DoubleAttr.X) == 1.0 && y[w][p].get(GRB.DoubleAttr.X) > 0.0001) {
                    System.out.print(p + " ");
                }
            }
        }
    }

    @Data
    private class Objective {
        private GRBModel model;
        private GRBVar[] x;
        private GRBVar[][] y;

        public Objective(GRBModel model) {
            this.model = model;
        }

        public Objective addObjective() throws GRBException {

            // Plant open decision variables: open[p] == 1 if plant p is open.
            x = new GRBVar[nFacilities];
            for (int p = 0; p < nFacilities; ++p) {
                x[p] = model.addVar(0, 1, facilities.get(p).getSetupCost(), GRB.BINARY, "x[" + p + "]");
            }


            // Transportation decision variables: y[w][p] = if product is transported from plant p to costumer w
            y = new GRBVar[nCostumers][nFacilities];
            for (int w = 0; w < nCostumers; ++w) {
                for (int p = 0; p < nFacilities; ++p) {
                    y[w][p] =
                            model.addVar(0,1, getDistance(costumers.get(w).getPosition(), facilities.get(p).getPosition()), GRB.BINARY,
                                    "y[" + w + "][" + p + "]");
                }

            }

            model.update();

            // The objectiveExpression is to minimize the total fixed and variable costs
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

            return this;
        }
    }

    @Data
    @AllArgsConstructor
    private class Constraints {

        private GRBModel model;
        private GRBVar[][] y;
        private GRBVar[] x;


        public void addConstraints() throws GRBException {
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
        }
    }

    private class StartingPoint {
        private GRBVar[] x;

        public StartingPoint(GRBVar... x) {
            this.x = x;
        }

        public void defineGuess() throws GRBException {
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
        }
    }
}

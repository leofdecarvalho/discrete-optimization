package vrp.mip;

import gurobi.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Leo on 08/12/2016.
 */
@Data
@AllArgsConstructor
public class SolverVRP {

    @NonNull private List<Truck> trucks;
    @NonNull private Facility facility;
    @NonNull private List<Customer> customers;
    @NonNull private List<Local> locals;

    private static int TIME_LIMIT_SECONDS = 200;

    public Double getDistance(Local i, Local j) {
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
            model.set(GRB.StringAttr.ModelName, "vrp");

            //Add objectiveExpression
            Objective objective = new Objective(model).addObjective();
            GRBVar[][] x = objective.getX();
            GRBVar[][][] t = objective.getT();
            GRBVar[] u = objective.getU();
            //Add constraints
            new Constraints(model, t, x, u).addConstraints();


            //Configure solver
            model.getEnv().set(GRB.IntParam.Method, GRB.METHOD_BARRIER);  // Use barrier to solve root relaxation
            model.getEnv().set(GRB.IntParam.OutputFlag, 1); // Disable gurobi logs

            model.getEnv().set(GRB.DoubleParam.TimeLimit, TIME_LIMIT_SECONDS);  //Define time limit optimization

           // model.setCallback(new VRPCallBack(x, u));
            model.optimize();
            model.write("VRP.lp"); //Used to print model in a file
            // Print solution

       // model.computeIIS(); //Used to debug solution infeasible
        //model.write("VRP.ilp"); //Used to IIS in a file

            printSolution(model, t, x, u);
            double[][][] vars = model.get(GRB.DoubleAttr.X, t);
            for (int ti = 0; ti < trucks.size(); ti++) {
                System.out.println("\n\n" + ti);
                int[] findsubtour = VRPCallBack.findsubtour(vars[ti]);
                for (int i = 0; i < findsubtour.length; i++) {
                    System.out.print(findsubtour[i] + " ");
                }
            }

            //debugSolution(model, amountDelivered, amountDelivered);

            // Dispose of model and environment
            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
    }

    private void printSolution(GRBModel model, GRBVar[][][] t, GRBVar[][] x, GRBVar[] u) throws GRBException {
        Tour tour = new Tour();
        for (int i = 0; i < locals.size(); i++) {
            for (int j = 0; j < locals.size(); j++) {
                    for (int ti = 0; ti < trucks.size(); ti++) {
                        if (t[ti][i][j].get(GRB.DoubleAttr.X) > 0.5) {
                            if (!tour.getTourByTruck().containsKey(ti)) {
                                tour.getTourByTruck().put(ti, new ArrayList<>());
                            }
                            tour.getTourByTruck().get(ti).add(i+"->"+j);
                        }
                }
            }
        }
        System.out.println("Trucks");
        for (int ti = 0; ti < trucks.size(); ti++) {
            System.out.print("\n" + ti);
            for (int i = 0; i < locals.size(); i++) {
                System.out.println();
                for (int j = 0; j < locals.size(); j++) {
                    Integer v = (int) t[ti][i][j].get(GRB.DoubleAttr.X);
                    System.out.print(v + " ");
                }
            }
        }

        System.out.println("\nRoutes");
        for (int i = 0; i < locals.size(); i++) {
            System.out.println();
            for (int j = 0; j < locals.size(); j++) {
                Integer v = (int) x[i][j].get(GRB.DoubleAttr.X);
                System.out.print(v + " ");
            }
        }
        System.out.println();
        for (Map.Entry<Integer, List<String>> tourByTrc: tour.getTourByTruck().entrySet()) {
            System.out.print("\n"+tourByTrc.getKey() + ": ");
            for (String j : tourByTrc.getValue()) {
                System.out.print(j + ", ");
            }

        }


    }



    @Data
    private class Objective {
        private GRBModel model;
        private GRBVar[][][] t; // route i->j is used by truck tw
        private GRBVar[][] x; // route i->j is used
        private GRBVar[] u; //(shipment for client i)
        private GRBVar[] v; //(shipment for client i)

        public Objective(GRBModel model) {
            this.model = model;
        }

        public Objective addObjective() throws GRBException {

            t = new GRBVar[trucks.size()][locals.size()][locals.size()];
            x = new GRBVar[locals.size()][locals.size()];
            for (int i = 0; i < locals.size(); ++i) {
                for (int j = 0; j < locals.size(); j++) {
                    x[i][j] = model.addVar(0, 1, 1, GRB.BINARY, "x_"+i+"_"+j);
                    for (int ti = 0; ti < trucks.size(); ti++) {
                       t[ti][i][j] = model.addVar(0, 1, getDistance(locals.get(i), locals.get(j)), GRB.BINARY, "t_"+i+"_"+j+"_"+ti);
                    }
                }
            }

            u = new GRBVar[customers.size()];
            for (int i = 0; i < customers.size(); i++) {
                u[i] = model.addVar(customers.get(i).getDemand(), trucks.get(0).getCapacity(), 1, GRB.CONTINUOUS, "u_"+i);
            }

            u = new GRBVar[customers.size()];
            for (int i = 0; i < customers.size(); i++) {
                u[i] = model.addVar(customers.get(i).getDemand(), trucks.get(0).getCapacity(), 1, GRB.CONTINUOUS, "u_"+i);
            }

            model.update();

            // Forbid edge from node back to itself
            for (int ti = 0; ti < trucks.size(); ti++)
                for (int i = 0; i < locals.size(); i++)
                    t[ti][i][i].set(GRB.DoubleAttr.UB, 0.0);

            for (int i = 0; i < locals.size(); i++)
                x[i][i].set(GRB.DoubleAttr.UB, 0.0);

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
        private GRBVar[][][] t; // truck tw is used on route (i,j)
        private GRBVar[][] x; // route (i->j) is used
        private GRBVar[] u; //(shipment for client i)
        private GRBVar[] V; //(shipment for client i)

        public void addConstraints() throws GRBException {

            // The loading capacity of each vehicle cannot be exceeded
            for (int ti = 0; ti < trucks.size(); ti++) {
                GRBLinExpr tot = new GRBLinExpr();
                for (int i = 1; i <= customers.size(); i++) {
                    for (int j = 0; j < locals.size(); j++) {
                        tot.addTerm(customers.get(i-1).getDemand(), t[ti][i][j]);
                    }
                }
                model.addConstr(tot, GRB.LESS_EQUAL, trucks.get(ti).getCapacity(), "C1_"+ti);
            }


            //The route i->j can be traveled by at most one vehicle
            for (int i = 0; i < locals.size(); i++) {
                for (int j = 0; j < locals.size(); j++) {
                    GRBLinExpr totVehicleOnRoute = new GRBLinExpr();
                    for (int ti = 0; ti < trucks.size(); ti++) {
                        totVehicleOnRoute.addTerm(1, t[ti][i][j]);
                    }
                    model.addConstr(totVehicleOnRoute, GRB.EQUAL, x[i][j], "C2_" + i + "_" + j);
                }
            }

            // The customer must be visited excactly once
            for (int i=1; i <= customers.size(); i++) {
                GRBLinExpr totVisitOncustomer = new GRBLinExpr();
                for (int j = 0; j < locals.size(); j++) {
                    totVisitOncustomer.addTerm(1, x[i][j]);
                }
                model.addConstr(totVisitOncustomer, GRB.EQUAL, 1, "C3_"+i);
            }

            // The customer must be visited excactly once
            for (int j= 1; j <= customers.size(); j++) {
                GRBLinExpr totVisitOncustomer = new GRBLinExpr();
                for (int i = 0; i < locals.size(); i++) {
                    totVisitOncustomer.addTerm(1, x[i][j]);
                }
                model.addConstr(totVisitOncustomer, GRB.EQUAL, 1, "C4_"+j);
            }


            //A vehicle must start at facility
            GRBLinExpr totStartVehicle = new GRBLinExpr();
            for (int j = 1; j <= customers.size(); j++) {
                totStartVehicle.addTerm(1, x[0][j]);
            }
            model.addConstr(totStartVehicle, GRB.LESS_EQUAL, trucks.size(), "C5");

            //A vehicle must end at facility
            GRBLinExpr totEndVehicle = new GRBLinExpr();
            for (int i = 1; i <= customers.size(); i++) {
                totEndVehicle.addTerm(1, x[i][0]);
            }
            model.addConstr(totEndVehicle, GRB.LESS_EQUAL, trucks.size(), "C6");


            // A vehicle that reaches a customer must leave the same customer
            for (int i = 1; i < customers.size(); i++) {
                for (int ti = 0; ti < trucks.size(); ti++) {
                    GRBLinExpr totIn = new GRBLinExpr();
                    for (int j = 0; j < locals.size(); j++) {
                        totIn.addTerm(1, t[ti][i][j]);
                        totIn.addTerm(-1, t[ti][j][i]);
                    }
                    model.addConstr(totIn, GRB.EQUAL, 0, "C8_"+i+"_"+ti);
                }
            }

            for (int ti = 0; ti < trucks.size(); ti++) {
                for (int i = 1; i <= customers.size(); i++) {
                    GRBLinExpr exp1 = new GRBLinExpr();
                    exp1.addTerm(customers.get(i - 1).getDemand() - trucks.get(0).getCapacity(), t[ti][0][i]);
                    exp1.addConstant(trucks.get(0).getCapacity());
                    model.addConstr(u[i - 1], GRB.LESS_EQUAL, exp1, "C9_" + ti+"_"+i);
                }
            }

            for (int ti = 0; ti < trucks.size(); ti++) {
                for (int i = 1; i <= customers.size(); i++) {
                    for (int j = 1; j <= customers.size(); j++) {
                        if (i != j) {
                            GRBLinExpr exp1 = new GRBLinExpr();
                            exp1.addTerm(1, u[i - 1]);
                            exp1.addTerm(-1, u[j - 1]);
                            exp1.addTerm(trucks.get(0).getCapacity(), t[ti][i][j]);
                            model.addConstr(exp1, GRB.LESS_EQUAL, trucks.get(0).getCapacity() - customers.get(j - 1).getDemand(), "C10_" +ti+"_"+ i + "_" + j);
                        }
                    }
                }
            }
        }
    }

}

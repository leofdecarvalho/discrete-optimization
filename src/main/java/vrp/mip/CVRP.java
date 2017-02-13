package vrp.mip;/* Copyright 2013, Gurobi Optimization, Inc. */
/* adapté de l'exemple TSP.java de Gurobi
 *  2013, C.Dürr , INF580
 

résoud le problème de Capacitated Vehicle Rounting.
Le modèle comporte des variables binaires Xijk, 
qui indiquent que le tour k empreinte l'arc (i,j).

Cette implémentation utilise des CallBack pour 
générer des contraintes de coupes.
*/

import java.io.*;
import java.util.*;
import gurobi.*;

class CVRP extends GRBCallback {
    private GRBVar[][][] vars;             // environnement pour le callback
    double q[];
    int C;

    public CVRP(GRBVar[][][] _vars, double _q[], int _C) {
        vars  = _vars;
        q     = _q;
        C     = _C;
    }

    // Gurobi propose une telle fonction pour des tableau à 1 ou 2 dimensions
    // mais pour 3 nous devons en écrire une
    double [][][] getSolution(GRBVar[][][] v) throws GRBException {
        int n = v.length;
        double r[][][] = new double[n][][];
        for (int i=0; i<n; i++)
            r[i] = getSolution(v[i]);
        return r;
    }

    // Subtour elimination callback.  Whenever a feasible solution is found,
    // find the subtour that contains node 0, and add a subtour elimination
    // constraint if the tour doesn't visit every node.

    protected void callback() {
        try {
            if (where == GRB.CB_MIPSOL) {
                // Found an integer feasible solution - does it visit every node?
                int n = vars.length;
                int[][] tours = findtours(getSolution(vars));
                int m = tours.length;
                boolean visited[] = new boolean[n];
                for (int t[]: tours)
                    for (int i: t)
                        visited[i] = true;
                int orphelin = 0; // demande qui n'est pas satisfaite par les tours
// String S = "";  // -- for debugging purposes	
                for (int i=0; i<n; i++)
                    if (!visited[i]) {
                        orphelin += q[i];
                        // S += ", "+i;
                    }

                if (orphelin > 0) {
                    // Add cut constraint
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int i = 0; i < n; i++)
                        if (visited[i])
                            for (int j = 0; j < n; j++)
                                if (!visited[j])
                                    for (int k=0; k<m; k++)
                                        expr.addTerm(1.0, vars[i][j][k]);
                    addLazy(expr, GRB.GREATER_EQUAL, orphelin/(double)C); // no integer division please
                }
//				System.out.println("*** cut "+orphelin+S);
            }
        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
            e.printStackTrace();
        }
    }

    // retourne un tableau de taille m qui pour chaque tour
    // comporte la liste des sommets visités, avec dépot en premier et dernier
    protected static int[][] findtours(double[][][] sol)
    {
        int j;
        int n = sol.length;
        int m = sol[0][0].length;
        // r[k] va contenir les sommets du tour du camion k en partant du depot
        int r[][] = new int[m][];

//		System.out.println(Arrays.deepToString(sol));

        for (int k=0; k<m; k++) {
            int tour[] = new int[n+1]; // n+1 = borne sup sur taille tour
            int p = 1; //                            -- position dans le tour (indice 0 =depot)
            int i=0;   //                            -- debuter le tour au depot 0
            while (p==1 || i>0) {
                // cherche l'arc i->j d'étiquette k
                for (j=0; j<n; j++)
                    if (sol[i][j][k]>0.5)
                        break;
                tour[p++]=j;
                i=j; // continuer la recherche sur des arcs sortant de j
            }
            //                          recopier le tour trouvé
            r[k] = new int[p];
            for (int a=0; a<p; a++)
                r[k][a] = tour[a];
        }

//		System.out.println(Arrays.deepToString(r));
        return r;
    }


    // Euclidean distance between points 'i' and 'j'

    protected static double distance(double[] x,
                                     double[] y,
                                     int      i,
                                     int      j) {
        return Math.hypot(x[i]-x[j], y[i]-y[j]);
    }

    public static void main(String[] args) throws Exception {



        // the instance
        int n=0,m=0;
        double x[]=null, y[]=null;
        double q[]=null;
        int C=0;


        ParserFile parserFile = new ParserFile(args).invoke();

        List<Truck> trucks = parserFile.getTrucks();
        Facility facility = parserFile.getFacility();
        List<Customer> costumers = parserFile.getCostumers();
        List<Local> locals = parserFile.getLocals();

        // two manners to generate instances
        m= trucks.size();
            n = locals.size();
            x = new double[n];    // coordonnees des noeuds
            y = new double[n];
            q = new double[n];       // demande en chaque noeud

            Random des = new Random(1024); // pour pouvoir reproduire les instances
            for (int i = 0; i < n; i++) {
                x[i] = locals.get(i).getX();
                y[i] = locals.get(i).getY();
            }
            for (int i = 1; i <= costumers.size(); i++) {
                q[i] = costumers.get(i-1).getDemand();
            }
            q[0] = 0; // depot has no demand
            C = trucks.get(0).getCapacity(); // capacity of a vehicle

        try {
            GRBEnv   env   = new GRBEnv();
            GRBModel model = new GRBModel(env);

            // Must disable dual reductions when using lazy constraints

            model.getEnv().set(GRB.IntParam.DualReductions, 0);


            // Create variables

            GRBVar[][][] vars = new GRBVar[n][n][m];

            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    for (int k = 0; k < m; k++)
                        vars[i][j][k] = model.addVar(0.0, 1.0, distance(x, y, i, j),
                                GRB.BINARY,
                                "x"+i+"_"+j+"_"+k);

            // Integrate variables

            model.update();

            // Forbid edge from node back to itself
            for (int k = 0; k < m; k++)
                for (int i = 0; i < n; i++)
                    vars[i][i][k].set(GRB.DoubleAttr.UB, 0.0);

            model.update();
            // A-Symmetric CVRP

            // cannot change vehicle outside of depot
            for (int j = 1; j < n; j++)
                for (int k = 0; k < m; k++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int i = 0; i < n; i++) {
                        expr.addTerm(+1.0, vars[i][j][k]);
                        expr.addTerm(-1.0, vars[j][i][k]);
                    }
                    model.addConstr(expr, GRB.EQUAL, 0.0, "conserveVehicle_"+j+"_"+k);
                }

            // out-degree 1 constraints for non-depot
            for (int i = 1; i < n; i++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int j = 0; j < n; j++)
                    for (int k = 0; k < m; k++)
                        expr.addTerm(1.0, vars[i][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1.0, "deg1_"+i);
            }

            // out-degree 1 constraint for depot
            for (int k = 0; k < m; k++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int j = 1; j < n; j++)
                    expr.addTerm(1.0, vars[0][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1.0, "depot1_"+k);
            }

            // vehicle capacity
            for (int k = 0; k < m; k++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int j = 1; j < n; j++)
                    for (int i = 0; i < n; i++)
                        expr.addTerm(q[j], vars[i][j][k]);
                model.addConstr(expr, GRB.LESS_EQUAL, C, "capacity_"+k);
            }

            model.setCallback(new CVRP(vars, q, C));
            model.optimize();
            model.write("tmp.lp"); // for debugging purposes, but LP will not contain lazy generated constraints
//           model.computeIIS();
            //model.write("tmp2.ilp");
            // the text file contains the coordinates of every tour separated by space
            // can be plotted using the import function of
            // http://itools.subhashbose.com/grapher/index.php?import=Import+txt+or+csv+file
            // or with gnuplot by the command
            // plot "solution.txt" with linespoints
            FileWriter solution = new FileWriter("solution.txt");
            if (model.get(GRB.IntAttr.SolCount) > 0) {
                int[][] tours = findtours(model.get(GRB.DoubleAttr.X, vars));
                for (int t[]: tours) {
                    for (int i: t)
                        solution.write(x[i]+" "+y[i]+"\n");
                    solution.write("\n");
                }
                for (int ti = 0; ti < m; ti++) {
                    solution.write("\n" + ti + "\n");
                    for (int i = 0; i < n; i++) {
                        solution.write("\n");
                        for (int j = 0; j < n; j++) {
                            solution.write(" " + (int) vars[i][j][ti].get(GRB.DoubleAttr.X));
                        }
                    }
                }
            }
            solution.close();

            // Dispose of model and environment
            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
            e.printStackTrace();
        }
    }
}
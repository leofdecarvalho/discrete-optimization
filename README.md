# ReduxSimpleStarter

Interested in learning [Discrete Optimization](https://www.coursera.org/learn/discrete-optimization)?

###Getting Started###

To getting started with this repo:

Checkout this repo, and put your java code with a main method in src/main/java/:

Example ...

public class Solver {
    
    /**
     * The main class
     */
    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Read the instance, solve it, and print the solution in the standard output
     */
    public static void solve(String[] args) throws IOException {

		//Read the input file
        List<String> lines = HandleFile.getLines(args);
       
		...	   
		//Call the solver
        KnapsackSolver solver = new BranchAndBound(items, capacity);
        KnapsackSolution solution = solver.solve();

	   ...
	   //Print the solution
        for (Item item: items) {
            if (solution.getItems().contains(item)){
                System.out.print(1 +" ");
            } else {
                System.out.print(0+" ");
            }
        }
    }

}

To run the assignament:

1. run mvn clean install in root dir of the project.This will generate the target\discrete-optimization-1.0.jar.

2. run submit.py in \assignament\problem\ where problem is the assignament (kanapsack, colorign, or others). 

The file solver.py was modified to call the jar discrete-optimization-1.0.jar generated by maven and the main class. Please, read the file \assignament\kanapsack\data\submit.py

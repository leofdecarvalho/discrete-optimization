package vrp.mip;

import java.util.List;

/**
 * Created by Leo on 04/12/2016.
 */
public class Solver {

    public static void main(String[] args) {

        ParserFile parserFile = new ParserFile(args).invoke();

        List<Truck> trucks = parserFile.getTrucks();
        Facility facility = parserFile.getFacility();
        List<Customer> costumers = parserFile.getCostumers();
        List<Local> locals = parserFile.getLocals();

        new SolverVRP(trucks, facility, costumers, locals).solve();
    }


}

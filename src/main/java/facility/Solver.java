package facility;

import java.util.List;

/**
 * Created by Leo on 04/12/2016.
 */
public class Solver {

    public static void main(String[] args) {

        ParserFile parserFile = new ParserFile(args).invoke();
        int nFacilities = parserFile.getNFacilities();
        List<Facility> facilities = parserFile.getFacilities();
        int nCostumers = parserFile.getNCostumers();
        List<Customer> costumers = parserFile.getCostumers();

        new SolverFacilityLocation(nFacilities, nCostumers, facilities, costumers).solve();
    }


}

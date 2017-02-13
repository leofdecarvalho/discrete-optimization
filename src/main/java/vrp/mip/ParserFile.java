package vrp.mip;

import input.HandleFile;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leo on 08/12/2016.
 */
@Data
class ParserFile {
    private String[] args;
    private List<Truck> trucks;
    private Facility facility;
    private List<Customer> costumers;
    private List<Local> locals;

    public ParserFile(String... args) {
        this.args = args;
    }

    public ParserFile invoke() {
        List<String> lines = new ArrayList<>();

        try {
            lines = HandleFile.getLines(args);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] firstLine = lines.get(0).split("\\s+");
        int trucksQuantity = Integer.parseInt(firstLine[1]);
        int trucksCapacity = Integer.parseInt(firstLine[2]);
        trucks = new ArrayList<>();
        for (Integer i = 0; i < trucksQuantity; i++) {
            trucks.add(new Truck(i.toString(), trucksCapacity));
        }
        int nCostumers = Integer.parseInt(firstLine[0]);

        String[] secondLine = lines.get(1).split("\\s+");
        costumers = new ArrayList<>();
        facility = new Facility(0, Double.parseDouble(secondLine[1]), Double.parseDouble(secondLine[2]));
        locals = new ArrayList<>();
        locals.add(facility);
        for (Integer i = 2; i <= nCostumers; i++) {
            String[] line = lines.get(i).split("\\s+");
            Integer costumerName = i-1;
            double positionX = Double.parseDouble(line[1]);
            double positionY = Double.parseDouble(line[2]);
            double demand = Double.parseDouble(line[0]);
            Customer costumer = new Customer(costumerName, positionX, positionY, demand);
            costumers.add(costumer);
            locals.add(costumer);
        }
        return this;
    }
}

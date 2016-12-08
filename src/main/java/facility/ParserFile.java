package facility;

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
    private int nFacilities;
    private int nCostumers;
    private List<Facility> facilities;
    private List<Costumer> costumers;

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

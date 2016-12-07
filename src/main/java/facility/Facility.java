package facility;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Facility {

    private Integer label;
    private Double setupCost;
    private Double capacity;
    private Point position;
}

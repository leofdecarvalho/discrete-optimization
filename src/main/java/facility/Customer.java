package facility;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Leo on 04/12/2016.
 */
@Data
@AllArgsConstructor
public class Customer {

    private Integer label;
    private Double demand;
    private Point position;
}

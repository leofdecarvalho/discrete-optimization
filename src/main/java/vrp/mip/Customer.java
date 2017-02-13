package vrp.mip;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Leo on 04/12/2016.
 */
@Data
public class Customer extends Local{

    private Double demand;

    public Customer(Integer name, Double x, Double y, Double demand) {
        super(name, x, y);
        this.demand = demand;
    }
}

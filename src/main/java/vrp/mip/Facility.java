package vrp.mip;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Facility extends  Local {
    public Facility(Integer name, Double x, Double y) {
        super(name, x, y);
    }
}

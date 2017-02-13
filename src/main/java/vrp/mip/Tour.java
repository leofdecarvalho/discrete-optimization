package vrp.mip;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Leo on 18/12/2016.
 */
@Data
@NoArgsConstructor
public class Tour {

    Map<Integer, List<String>> tourByTruck = new HashMap<>();
}

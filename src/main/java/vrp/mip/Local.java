package vrp.mip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of={"name"})
public class Local {
	private Integer name;
	private Double x;
	private Double y;
}

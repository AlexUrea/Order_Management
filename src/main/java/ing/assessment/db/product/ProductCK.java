package ing.assessment.db.product;

import ing.assessment.model.Location;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ProductCK {
    private Integer id;
    private Location location;
}
package ing.assessment.db.dto;

import ing.assessment.model.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Integer id;          // from ProductCK.id
    private String name;
    private Double price;
    private Integer quantity;
    private Location location;   // from ProductCK.location
}


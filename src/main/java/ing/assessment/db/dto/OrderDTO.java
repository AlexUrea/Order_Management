package ing.assessment.db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Integer id;
    private Date timestamp;
    private List<OrderProductDTO> orderProducts;
    private Double orderCost;
    private Integer deliveryCost;
    private Integer deliveryTime;
}


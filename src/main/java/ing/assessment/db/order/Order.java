package ing.assessment.db.order;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

import static ing.assessment.utils.GlobalConstants.Costs.DEFAULT_COST_OF_DELIVERY;
import static ing.assessment.utils.GlobalConstants.Costs.DEFAULT_DELIVERY_TIME;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Date timestamp;
    @ElementCollection
    private List<OrderProduct> orderProducts;
    private Double orderCost;
    private Integer deliveryCost = DEFAULT_COST_OF_DELIVERY;
    private Integer deliveryTime = DEFAULT_DELIVERY_TIME;
}
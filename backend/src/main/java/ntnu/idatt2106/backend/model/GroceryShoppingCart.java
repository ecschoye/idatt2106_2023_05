package ntnu.idatt2106.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "GroceryShoppingCart")
@Schema(description = "Connection table between groceries and the shopping cart")
@Entity
public class GroceryShoppingCart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "The id to the connection between shopping cart and grocery, automatically generated")
    private long id;

    @Column(name = "quantity")
    @Schema(description = "The number of groceries to buy")
    private int quantity;

    @ManyToOne
    @JoinColumn(name = "groceryId")
    @Schema(description = "Grocery object in the list")
    private Grocery grocery;

    @ManyToOne
    @JoinColumn(name = "shoppingCartId")
    @Schema(description = "The shopping cart connected to the grocery")
    private ShoppingCart shoppingCart;
}

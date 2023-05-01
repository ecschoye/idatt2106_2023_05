package ntnu.idatt2106.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RefrigeratorGrocery")
@Schema(description = "Connection between the groceries and refrigerators")
@Entity
public class RefrigeratorGrocery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "The id for the connection between refrigerator and grocery, automatically generated")
    private long id;

    @NotNull
    @Column(name = "physicalExpireDate")
    @Schema(description = "Expire date for the grocery")
    private Date physicalExpireDate;

    @NotNull
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "refrigeratorId")
    @Schema(description = "The refrigerator the grocery is in")
    private Refrigerator refrigerator;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "groceryId")
    @Schema(description = "Grocery in the refrigerator")
    private Grocery grocery;

    @OneToMany(mappedBy = "groceryEntity", cascade = CascadeType.REMOVE)
    private Set<GroceryNotification> groceryNotifications = new HashSet<>();
}

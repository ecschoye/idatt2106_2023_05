package ntnu.idatt2106.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ntnu.idatt2106.backend.model.enums.Role;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RefrigeratorUser")
@Schema(description = "Connection between the refrigerators and users")
@Entity
public class RefrigeratorUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "The id for the connection between refrigerator and user, automatically generated")
    private long id;

    @Column(name = "role")
    @Schema(description = "Role the member has in the refrigerator")
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "refrigeratorId")
    @Schema(description = "The refrigerator the user is member of")
    private Refrigerator refrigerator;

    @ManyToOne
    @JoinColumn(name = "userId")
    @Schema(description = "Member in the refrigerator")
    private User user;
}

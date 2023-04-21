package ntnu.idatt2106.backend.repository;

import ntnu.idatt2106.backend.model.RefrigeratorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefrigeratorUserRepository extends JpaRepository<RefrigeratorUser, Long> {
    Optional<RefrigeratorUser> findByUser_IdAndRefrigerator_Id(String userId, long refrigeratorId);
}
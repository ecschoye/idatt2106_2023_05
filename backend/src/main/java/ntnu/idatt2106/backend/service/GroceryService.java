package ntnu.idatt2106.backend.service;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ntnu.idatt2106.backend.exceptions.RefrigeratorNotFoundException;
import ntnu.idatt2106.backend.exceptions.UnauthorizedException;
import ntnu.idatt2106.backend.exceptions.UserNotFoundException;
import ntnu.idatt2106.backend.model.Refrigerator;
import ntnu.idatt2106.backend.model.RefrigeratorGrocery;
import ntnu.idatt2106.backend.model.dto.RefrigeratorGroceryDTO;
import ntnu.idatt2106.backend.model.enums.Role;
import ntnu.idatt2106.backend.repository.RefrigeratorGroceryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling operations regarding groceries inside a refrigerator
 */
@Service
@RequiredArgsConstructor
public class GroceryService {

    private final Logger logger = LoggerFactory.getLogger(GroceryService.class);

    private final CookieService cookieService;
    private final JwtService jwtService;

    private final RefrigeratorGroceryRepository refGroceryRepository;
    private final RefrigeratorService refrigeratorService;

    /**
     * Extracts username from cookie
     * @param httpRequest Request we are extracting from
     * @return username
     */
    public String extractEmail(HttpServletRequest httpRequest) {
        String token = cookieService.extractTokenFromCookie(httpRequest);
        return jwtService.extractClaim(token, Claims::getSubject);
    }

    /**
     * Gets a list of DTO's representing groceries
     * in refrigerator.
     *
     * @param refrigeratorId Refrigerator id
     * @param request Http request
     * @return List of RefrigeratorGrocery DTO's
     * @throws RefrigeratorNotFoundException If refrigerator not found
     * @throws UserNotFoundException if user not found
     * @throws UnauthorizedException if user not member
     */
    public List<RefrigeratorGroceryDTO> getGroceriesByRefrigerator(long refrigeratorId, HttpServletRequest request) throws RefrigeratorNotFoundException, UserNotFoundException, UnauthorizedException {
        //Throws if refrigerator does not exist
        Refrigerator refrigerator = refrigeratorService.getRefrigerator(refrigeratorId);
        getRole(refrigerator,request);

        List<RefrigeratorGrocery> groceries = refGroceryRepository.findByRefrigerator(refrigerator);
        List<RefrigeratorGroceryDTO> refGroceryDTOS = new ArrayList<>();
        for (RefrigeratorGrocery grocery: groceries) {
            refGroceryDTOS.add(new RefrigeratorGroceryDTO(grocery));
        }
        return refGroceryDTOS;
    }

    /**
     * Gets the role of user that requested action
     *
     * @param refrigerator refrigerator role is in
     * @param request request to api
     * @return Role of user
     * @throws UserNotFoundException if user not found
     * @throws UnauthorizedException if user not member
     */
    public Role getRole(Refrigerator refrigerator, HttpServletRequest request) throws UserNotFoundException, UnauthorizedException {
        logger.info("Checking if user is member");
        String email = extractEmail(request);
        //Throws if user is not member
        return refrigeratorService.getUserRole(refrigerator, email);
    }

    /**
     * Removes a refrigeratorGrocery by id
     *
     * @param refrigeratorGroceryId id
     * @param request http request by user
     * @throws UserNotFoundException If refrigeratorGrocery not found
     * @throws UnauthorizedException If user does not have permission
     */
    @Transactional(propagation =  Propagation.REQUIRED, rollbackFor = Exception.class)
    public void removeRefrigeratorGrocery(long refrigeratorGroceryId, HttpServletRequest request) throws UserNotFoundException, UnauthorizedException, EntityNotFoundException {
        RefrigeratorGrocery refrigeratorGrocery = getRefrigeratorGroceryById(refrigeratorGroceryId);
        if(getRole(refrigeratorGrocery.getRefrigerator(), request) != Role.SUPERUSER) {
            throw new UnauthorizedException("User does not have permission to remove this grocery");
        }
        refGroceryRepository.deleteById(refrigeratorGroceryId);
    }

    /**
     * Gets refigeratorGrocery by id
     *
     * @param refrigeratorGroceryId refigeratorGrocery id
     * @return the refrigeratorGrocery
     */
    public RefrigeratorGrocery getRefrigeratorGroceryById(long refrigeratorGroceryId) throws EntityNotFoundException{
        return refGroceryRepository.findById(refrigeratorGroceryId)
                .orElseThrow(() -> new EntityNotFoundException("refrigeratorGrocery not found"));
    }

}

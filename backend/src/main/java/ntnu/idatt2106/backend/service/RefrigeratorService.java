package ntnu.idatt2106.backend.service;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ntnu.idatt2106.backend.exceptions.*;
import ntnu.idatt2106.backend.model.Refrigerator;
import ntnu.idatt2106.backend.model.RefrigeratorGrocery;
import ntnu.idatt2106.backend.model.RefrigeratorUser;
import ntnu.idatt2106.backend.model.User;
import ntnu.idatt2106.backend.model.dto.RefrigeratorDTO;
import ntnu.idatt2106.backend.model.enums.FridgeRole;
import ntnu.idatt2106.backend.model.requests.MemberRequest;
import ntnu.idatt2106.backend.model.dto.MemberDTO;
import ntnu.idatt2106.backend.model.requests.RemoveMemberRequest;
import ntnu.idatt2106.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The RefrigeratorService class provides access to user data stored in the RefrigeratorRepository.
 * It provides methods for finding and manipulating user data.
 *
 * TODO Generalize member data valiation
 * TODO Superuser should not be able to change to user if only superuser
 */
@Service
@RequiredArgsConstructor
public class RefrigeratorService {

    private final FridgeRole DEFAULT_USER_Fridge_ROLE = FridgeRole.USER;
    private final FridgeRole EDIT_PRIVILEGE = FridgeRole.SUPERUSER;

    private final CookieService cookieService;
    private final JwtService jwtService;
    private final RefrigeratorRepository refrigeratorRepository;
    private final RefrigeratorUserRepository refrigeratorUserRepository;
    private final RefrigeratorGroceryRepository refrigeratorGroceryRepository;
    private final UserRepository userRepository;

    private final Logger logger = LoggerFactory.getLogger(RefrigeratorService.class);

    /**
     * Adds a member to a refrigerator. Takes a member request and performs
     * necessary checks to validate that user exists, super has privileges in
     * refrigerator.
     *
     * @param request MemberRequest input
     * @param httpRequest
     * @return MemberReponse containing information about the affected user
     * @throws UserNotFoundException if super or user does not exist
     */
    public MemberDTO addMember(MemberRequest request, HttpServletRequest httpRequest) throws UserNotFoundException, RefrigeratorNotFoundException, UnauthorizedException {
        logger.debug("Getting new member user");
        User user = getUser(request.getUserName());

        //Get refrigerator
        Refrigerator refrigerator = getRefrigerator(request.getRefrigeratorId());

        //Check privileges
        FridgeRole privileges = getFridgeRole(refrigerator, extractEmail(httpRequest));
        if(privileges != FridgeRole.SUPERUSER){
            logger.warn("Member could not be added: User does not have super-privileges");
            return null;
        }

        Optional<RefrigeratorUser> existing = refrigeratorUserRepository.findByUserAndRefrigerator(user, refrigerator);
        if(existing.isPresent()) {
            return new MemberDTO(existing.get());
        }

        RefrigeratorUser ru = new RefrigeratorUser();
        ru.setFridgeRole(DEFAULT_USER_Fridge_ROLE);
        ru.setRefrigerator(refrigerator);
        ru.setUser(user);

        try {
            logger.info("Checks validated, saving refrigeratorUser");
            RefrigeratorUser result = refrigeratorUserRepository.save(ru);
            return new MemberDTO(result);
        } catch (Exception e) {
            logger.warn("Member could not be added: Failed to save refrigeratoruser");
            return null;
        }
    }

    /**
     * Method to edit a refrigerator. Takes a refrigeratorDTO and
     * changes the refrigerator with its values.
     *
     * @param refrigeratorDTO Updated refrigerator data
     * @param request http request
     * @throws UnauthorizedException If user not superuser
     * @throws RefrigeratorNotFoundException If refrigerator by id returns empty
     * @throws UserNotFoundException if user not found
     * @throws SaveException if refrigerator could not be updated
     */
    @Transactional(propagation =  Propagation.REQUIRED, rollbackFor = Exception.class)
    public void editRefrigerator(RefrigeratorDTO refrigeratorDTO, HttpServletRequest request) throws UnauthorizedException, UserNotFoundException, SaveException, RefrigeratorNotFoundException {
        User user = getUser(extractEmail(request));
        Refrigerator refrigerator = getRefrigerator(refrigeratorDTO.getId());
        RefrigeratorUser refrigeratorUser = refrigeratorUserRepository.findByUserAndRefrigerator(user, refrigerator)
                .orElseThrow(() -> new UnauthorizedException("User not a member"));
        FridgeRole role = getFridgeRole(refrigerator, user.getEmail());

        if(role != EDIT_PRIVILEGE) throw new UnauthorizedException("User not authorized to edit refrigerator");
        logger.info("Updating refrigerator");

        refrigerator.setName(refrigeratorDTO.getName());
        if(refrigeratorDTO.getAddress() != null){
            refrigerator.setAddress(refrigeratorDTO.getAddress());
        }
        try{
            refrigerator = refrigeratorRepository.save(refrigerator);
            refrigeratorUserRepository.save(refrigeratorUser);
        }
        catch(Exception e){
            throw new SaveException("Refrigerator could not be updated");
        }
        refrigeratorUser.setRefrigerator(refrigerator);
    }
    /**
     * Retrieves all refrigerators where a given user is a member
     *
     * @return A list containing all refrigerators.
     */
    public List<Refrigerator> getAllByUser(HttpServletRequest request) throws UserNotFoundException{
        User user = getUser(extractEmail(request));
        List<Refrigerator> refrigerators = new ArrayList<>();
        refrigeratorUserRepository.findByUser(user).forEach(ru -> {refrigerators.add(ru.getRefrigerator());});
        return refrigerators;
    }

    public Refrigerator convertToEntity(RefrigeratorDTO refrigeratorDTO){
        if (refrigeratorDTO == null) return null;


        Refrigerator refrigerator = Refrigerator.builder()
                .id(refrigeratorDTO.getId())
                .name(refrigeratorDTO.getName())
                .address(refrigeratorDTO.getAddress())
                .build();

        return refrigerator;
    }

    /**
     * Gets a refrigerator by id and all data we
     * want the user to know about the refrigerator.
     *
     * @param id Id of refrigerator to get
     * @return RefrigeratorResponse
     */
    public RefrigeratorDTO getRefrigeratorDTOById(long id, HttpServletRequest request) throws EntityNotFoundException, RefrigeratorNotFoundException, UserNotFoundException {
        User user = getUser(extractEmail(request));
        Refrigerator refrigerator = getRefrigerator(id);
        List<MemberDTO> users = new ArrayList<>();
        refrigeratorUserRepository.findByRefrigeratorId(id)
                .forEach((ru -> users.add(new MemberDTO(ru))));
        return new RefrigeratorDTO(refrigerator, users);
    }

    /**
     * Removes user from refrigerator. Does check to always
     * keep at least one superuser in the refrigerator.
     *
     * @param request request object containing necessary data.
     * @param httpRequest
     * @throws AccessDeniedException If user tries to remove other users
     * @throws LastSuperuserException If superuser tries to remove himself as last superuser
     * @throws EntityNotFoundException If request data is invalid.
     */
    @Transactional(propagation =  Propagation.REQUIRED, rollbackFor = Exception.class)
    public void removeUserFromRefrigerator(RemoveMemberRequest request, HttpServletRequest httpRequest) throws Exception {
        if(request.isForceDelete()) {
            forceDeleteRefrigerator(request.getRefrigeratorId(), httpRequest);
        }

        Refrigerator refrigerator = getRefrigerator(request.getRefrigeratorId());
        User user = getUser(request.getUserName());
        RefrigeratorUser userRole = refrigeratorUserRepository.findByUser_IdAndRefrigerator_Id(user.getId(), refrigerator.getId())
                    .orElseThrow(() -> new EntityNotFoundException("User not a member"));
        User superUser = getUser(extractEmail(httpRequest));
        RefrigeratorUser superuserRole = refrigeratorUserRepository.findByUser_IdAndRefrigerator_Id(superUser.getId(), refrigerator.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not a member"));

        logger.info("Performing member removal");
        if(userRole.getFridgeRole() == FridgeRole.USER && user.getId().equals(superUser.getId())){
            logger.debug("User deleting himself");
            refrigeratorUserRepository.delete(userRole);
        }
        else if(userRole.getFridgeRole() == FridgeRole.SUPERUSER){
            List<RefrigeratorUser> superUsers = refrigeratorUserRepository.findByRefrigeratorIdAndFridgeRole(refrigerator.getId(), FridgeRole.SUPERUSER);
            if(superUsers.size() <= 1) {
                throw new LastSuperuserException("Last superuser in refrigerator");
            }
            else {
                refrigeratorUserRepository.delete(userRole);
            }
        }
        else {
            if(superuserRole.getFridgeRole() != FridgeRole.SUPERUSER) {
                logger.warn("User attempted to remove another user(!)");
                throw new AccessDeniedException("User cannot remove other users");
            }
            refrigeratorUserRepository.delete(userRole);
        }
        refrigeratorRepository.save(refrigerator);
    }



    /**
     * Saves a new refrigerator and connects the creator as a
     * superuser of the refrigerator. Performs checks on data and resets state
     * should an error occur underway.
     *
     * @param refrigeratorDTO request containing refrigerator to save.
     * @return The saved refrigerator.
     */
    @Transactional(propagation =  Propagation.REQUIRED, rollbackFor = Exception.class)
    public Refrigerator save(RefrigeratorDTO refrigeratorDTO, HttpServletRequest httpRequest) throws Exception {
        logger.info("Converting to refrigeretor object");
        Refrigerator refrigerator = convertToEntity(refrigeratorDTO);

        //Check user exists
        logger.info("Checking user");
        User user = getUser(extractEmail(httpRequest));
        Refrigerator refrigeratorResult;
        try {
            logger.info("Saving refrigerator");
            refrigeratorResult = refrigeratorRepository.save(refrigerator);
        } catch (Exception e) {
            logger.warn("Refrigerator could not be added: refrigerator could not be added");
            throw new Exception("Refrigerator could not be added: refrigerator could not be added");
        }

        //Connect user to
        logger.info("Connecting user as member");
        RefrigeratorUser refrigeratorUser = new RefrigeratorUser();
        refrigeratorUser.setRefrigerator(refrigeratorResult);
        refrigeratorUser.setUser(user);
        refrigeratorUser.setFridgeRole(FridgeRole.SUPERUSER);
        try {
            refrigeratorUserRepository.save(refrigeratorUser);
        } catch (Exception e) {
            logger.warn("Refrigerator could not be added: User could not be connected to refrigerator");
            throw new Exception("User could not be connected to refrigerator");
        }
        return refrigerator;
    }

    /**
     * Sets role of a refrigerator member
     *
     * @param request Request containing data about the super, user, role
     * @param httpRequest
     * @return Response containing user affected, and given role.
     */
    public MemberDTO setFridgeRole(MemberRequest request, HttpServletRequest httpRequest) throws UserNotFoundException, UnauthorizedException, RefrigeratorNotFoundException {
        //Get User
        logger.debug("Getting new member user");
        User user = getUser(request.getUserName());

        //Get refrigerator
        Refrigerator refrigerator = getRefrigerator(request.getRefrigeratorId());

        //Check privileges
        FridgeRole privileges = getFridgeRole(refrigerator, extractEmail(httpRequest));
        if(privileges != FridgeRole.SUPERUSER){
            logger.warn("Member could not be added: User does not have super-privileges");
            throw new UnauthorizedException("User does not have super-privileges");
        }

        RefrigeratorUser ru = new RefrigeratorUser();
        ru.setFridgeRole(FridgeRole.USER);
        ru.setRefrigerator(refrigerator);
        ru.setUser(user);

        //Check if we have an instance from before
        Optional<RefrigeratorUser> existingRu = refrigeratorUserRepository.findByUserAndRefrigerator(user,refrigerator);
        if(existingRu.isPresent()){
            existingRu.get().setFridgeRole(request.getFridgeRole());
            try {
                logger.info("Checks validated, updating refrigeratorUser");
                RefrigeratorUser result = refrigeratorUserRepository.save(existingRu.get());
                return new MemberDTO(result);
            } catch (Exception e) {
                logger.warn("Member could not be updated: Failed to update refrigeratoruser");
                return null;
            }
        }
        else logger.warn("Updating role failed: user is not a member");
        return null;
    }

    /**
     * Forcefully deletes a refrigerator and all its members. Gets
     * the user that requested the delete and checks permission. Then
     * deletes any members and the refrigerator itself.
     *
     * TODO: DELETE INVENTORY ++
     * @param refrigeratorId The ID of the refrigerator to delete.
     */
    @Transactional(propagation =  Propagation.REQUIRED, rollbackFor = Exception.class)
    public void forceDeleteRefrigerator(long refrigeratorId, HttpServletRequest httpRequest) throws Exception {
        logger.info("Force delete was requested");
        User superUser = getUser(extractEmail(httpRequest));
        RefrigeratorUser superuserRole = refrigeratorUserRepository.findByUser_IdAndRefrigerator_Id(superUser.getId(), refrigeratorId)
                .orElseThrow(() -> new EntityNotFoundException("User not a member"));
        if(superuserRole.getFridgeRole() != FridgeRole.SUPERUSER){
            logger.error("Failed to delete refrigerator: User not superuser");
            throw new AccessDeniedException("Failed to delete refrigerator: User not superuser");
        }
        if (refrigeratorRepository.existsById(refrigeratorId)) {
            long members = refrigeratorUserRepository.findByRefrigeratorId(refrigeratorId).size();
            if(members > 0){
                try {
                    refrigeratorUserRepository.removeByRefrigeratorId(refrigeratorId);
                } catch (Exception e) {
                    logger.error("Failed to delete refrigerator: failed to remove refrigerator members, {}",e.getMessage());
                    throw e;
                }
            }
            long groceries = refrigeratorGroceryRepository.findAllByRefrigeratorId(refrigeratorId).size();
            if(groceries > 0){
                try {
                    refrigeratorGroceryRepository.removeByRefrigeratorId(refrigeratorId);
                } catch (Exception e) {
                    logger.error("Failed to delete refrigerator: failed to remove refrigerator groceries, {}",e.getMessage());
                    throw e;
                }
            }
            try {
                refrigeratorRepository.deleteById(refrigeratorId);
                return;
            } catch (EmptyResultDataAccessException e) {
                logger.error("Failed to delete refrigerator: Id {}: {},", refrigeratorId, e.getMessage());
            }
        }
        else{
            logger.error("Failed to delete refrigerator: Refrigerator with id {} does not exist", refrigeratorId);
            throw new EntityNotFoundException("Failed to delete refrigerator: Refrigerator does not exist");
        }
        throw new Exception("Failed to delete refrigerator");
    }


    /**
     * Gets refrigerator
     *
     * @param refrigeratorId id of refrigerator
     * @return Refrigerator
     * @throws RefrigeratorNotFoundException if not found
     */
    public Refrigerator getRefrigerator(long refrigeratorId) throws RefrigeratorNotFoundException{
        return refrigeratorRepository.findById(refrigeratorId)
                .orElseThrow(() -> new RefrigeratorNotFoundException("Refrigerator not found"));
    }

    /**
     * Gets a users role in the refrigerator
     *
     * @param refrigerator refrigerator
     * @param email username
     * @return Role in refrigerator
     * @throws UserNotFoundException if user not found
     * @throws UnauthorizedException if user not member of refrigerator
     */
    public FridgeRole getFridgeRole(Refrigerator refrigerator, String email) throws UserNotFoundException, UnauthorizedException {
        User user = getUser(email);
        RefrigeratorUser ru = refrigeratorUserRepository.findByUserAndRefrigerator(user, refrigerator)
                .orElseThrow(() -> new UnauthorizedException("User not member of refrigerator"));
        return ru.getFridgeRole();
    }

    /**
     * Gets a users role in the refrigerator
     *
     * @param refrigeratorId id of refrigerator
     * @param userId id of user
     * @return Role in refrigerator
     * @throws UnauthorizedException if user not member of refrigerator
     */
    public FridgeRole getFridgeRoleById(long refrigeratorId, String userId) throws UnauthorizedException {
        RefrigeratorUser ru = refrigeratorUserRepository.findByUser_IdAndRefrigerator_Id(userId, refrigeratorId)
                .orElseThrow(() -> new UnauthorizedException("User not member of refrigerator"));
        return ru.getFridgeRole();
    }

    /**
     * Gets user
     *
     * @param email email usernamae
     * @return User
     * @throws UserNotFoundException if not found
     */
    public User getUser(String email) throws UserNotFoundException{
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email "+email+" not found"));
    }

    /**
     * Extracts username from cookie
     * @param httpRequest Request we are extracting from
     * @return username
     */
    public String extractEmail(HttpServletRequest httpRequest) {
        String token = cookieService.extractTokenFromCookie(httpRequest);
        return jwtService.extractClaim(token, Claims::getSubject);
    }
}

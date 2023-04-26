package ntnu.idatt2106.backend.service;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import ntnu.idatt2106.backend.exceptions.RefrigeratorNotFoundException;
import ntnu.idatt2106.backend.exceptions.UnauthorizedException;
import ntnu.idatt2106.backend.exceptions.UserNotFoundException;
import ntnu.idatt2106.backend.model.*;
import ntnu.idatt2106.backend.model.dto.RefrigeratorGroceryDTO;
import ntnu.idatt2106.backend.model.enums.Role;
import ntnu.idatt2106.backend.repository.RefrigeratorGroceryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GroceryServiceTest {

    @Mock
    private RefrigeratorGroceryRepository refrigeratorGroceryRepository;

    @Mock
    private RefrigeratorService refrigeratorService;

    @Mock
    private CookieService cookieService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private GroceryService groceryService;

    //Testdata
    private List<RefrigeratorGrocery> groceryList;
    private List<RefrigeratorGroceryDTO> groceryDTOList;
    private RefrigeratorGrocery refrigeratorGrocery;
    private Refrigerator refrigerator;
    private RefrigeratorUser refrigeratorUser;
    private User user;
    private HttpServletRequest request;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId("testUserId");
        user.setEmail("testuser@test.com");

        refrigerator = new Refrigerator();
        refrigerator.setId(1L);
        refrigerator.setName("Test Refrigerator");

        refrigeratorUser = new RefrigeratorUser();
        refrigeratorUser.setUser(user);
        refrigeratorUser.setRefrigerator(refrigerator);
        refrigeratorUser.setRole(Role.USER);

        groceryList = new ArrayList<>();
        groceryDTOList = new ArrayList<>();

        SubCategory subCategory = new SubCategory();
        subCategory.setCategory(new Category());
        Grocery grocery = new Grocery();
        grocery.setId(1L);
        grocery.setName("Name");
        grocery.setDescription("Desc");
        grocery.setGroceryExpiryDays(2);
        grocery.setSubCategory(subCategory);
        refrigeratorGrocery = new RefrigeratorGrocery();
        refrigeratorGrocery.setGrocery(grocery);

        groceryList.add(refrigeratorGrocery);
        groceryDTOList.add(new RefrigeratorGroceryDTO(refrigeratorGrocery));

        request = mock(HttpServletRequest.class);
    }

    @Test
    @DisplayName("Test getGroceriesByRefrigerator succeeds")
    public void testGetGroceriesByRefrigeratorSucceeds() throws RefrigeratorNotFoundException, UserNotFoundException, UnauthorizedException {
        // Setup
        long refrigeratorId = 1L;
        HttpServletRequest request = mock(HttpServletRequest.class);
        String expectedEmail = "testuser@test.com";
        String token = "valid_token";
        List<RefrigeratorGrocery> groceries = new ArrayList<>();
        groceries.add(refrigeratorGrocery);
        Mockito.when(cookieService.extractTokenFromCookie(request)).thenReturn(token);
        Mockito.when(jwtService.extractClaim(token, Claims::getSubject)).thenReturn(expectedEmail);
        Mockito.when(refrigeratorService.getRefrigerator(refrigeratorId)).thenReturn(refrigerator);
        Mockito.when(refrigeratorService.getUserRole(any(), any())).thenReturn(Role.USER);
        Mockito.when(refrigeratorGroceryRepository.findByRefrigerator(refrigerator)).thenReturn(groceries);

        // Execute
        List<RefrigeratorGroceryDTO> result = groceryService.getGroceriesByRefrigerator(refrigeratorId, request);

        // Assert
        assertNotNull(result);
        assertEquals(groceries.size(), result.size());
    }

    @Test
    @DisplayName("Test getGroceriesByRefrigerator fails when refrigerator not found")
    public void testGetGroceriesByRefrigeratorFailsWhenRefrigeratorNotFound() throws RefrigeratorNotFoundException {
        // Setup
        long refrigeratorId = 1L;
        HttpServletRequest request = mock(HttpServletRequest.class);
        String expectedEmail = "testuser@test.com";
        String token = "valid_token";
        Mockito.when(cookieService.extractTokenFromCookie(request)).thenReturn(token);
        Mockito.when(jwtService.extractClaim(token, Claims::getSubject)).thenReturn(expectedEmail);
        Mockito.when(refrigeratorService.getRefrigerator(refrigeratorId)).thenThrow(new RefrigeratorNotFoundException("Refrigerator not found"));

        // Execute and Assert
        assertThrows(RefrigeratorNotFoundException.class, () -> groceryService.getGroceriesByRefrigerator(refrigeratorId, request));
    }

    @Test
    @DisplayName("Test getGroceriesByRefrigerator fails when user not a member of the refrigerator")
    public void testGetGroceriesByRefrigeratorFailsWhenUserNotMember() throws RefrigeratorNotFoundException, UserNotFoundException, UnauthorizedException {
        // Setup
        long refrigeratorId = 1L;
        HttpServletRequest request = mock(HttpServletRequest.class);
        String token = "valid_token";
        Mockito.when(cookieService.extractTokenFromCookie(request)).thenReturn(token);
        Mockito.when(jwtService.extractClaim(token, Claims::getSubject)).thenReturn(user.getUsername());
        Mockito.when(refrigeratorService.getRefrigerator(refrigeratorId)).thenReturn(refrigerator);
        Mockito.when(refrigeratorService.getUserRole(any(), any())).thenThrow(new UnauthorizedException("User not member"));

        // Execute and Assert
        assertThrows(UnauthorizedException.class, () -> groceryService.getGroceriesByRefrigerator(refrigeratorId, request));
    }

    @Test
    @DisplayName("Test getGroceriesByRefrigerator fails when user not found")
    public void testGetGroceriesByRefrigeratorFailsWhenUserNotFound() throws RefrigeratorNotFoundException, UserNotFoundException, UnauthorizedException {
        // Setup
        long refrigeratorId = 1L;
        HttpServletRequest request = mock(HttpServletRequest.class);
        String expectedEmail = "testuser@test.com";
        String token = "valid_token";
        Mockito.when(cookieService.extractTokenFromCookie(request)).thenReturn(token);
        Mockito.when(jwtService.extractClaim(token, Claims::getSubject)).thenReturn(expectedEmail);
        Mockito.when(refrigeratorService.getRefrigerator(refrigeratorId)).thenReturn(refrigerator);
        Mockito.when(refrigeratorService.getUserRole(any(),any())).thenThrow(new UserNotFoundException("User not found"));

        // Execute and Assert
        assertThrows(UserNotFoundException.class, () -> groceryService.getGroceriesByRefrigerator(refrigeratorId, request));
    }

    @Test
    public void removeRefrigeratorGrocery_SuccessfulRemoval() throws UserNotFoundException, UnauthorizedException, EntityNotFoundException {
        String email = "user@example.com";

        when(cookieService.extractTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.extractClaim(eq("token"), any())).thenReturn(email);
        when(refrigeratorGroceryRepository.findById(any())).thenReturn(Optional.of(refrigeratorGrocery));
        when(refrigeratorService.getUserRole(any(), any())).thenReturn(Role.SUPERUSER);

        groceryService.removeRefrigeratorGrocery(1L, request);

        verify(refrigeratorGroceryRepository, times(1)).deleteById(1L);
    }

    @Test
    public void removeRefrigeratorGrocery_EntityNotFound() {
        when(refrigeratorGroceryRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> groceryService.removeRefrigeratorGrocery(1L, request));

        verify(refrigeratorGroceryRepository, never()).deleteById(1L);
    }

    @Test
    public void removeRefrigeratorGrocery_UnauthorizedUser() throws UserNotFoundException, EntityNotFoundException, UnauthorizedException {
        String email = "user@example.com";

        when(cookieService.extractTokenFromCookie(any(HttpServletRequest.class))).thenReturn("token");
        when(jwtService.extractClaim(eq("token"), any())).thenReturn(email);
        when(refrigeratorGroceryRepository.findById(any())).thenReturn(Optional.of(refrigeratorGrocery));
        when(refrigeratorService.getUserRole(any(Refrigerator.class), eq(email))).thenReturn(Role.USER);

        Assertions.assertThrows(UnauthorizedException.class, () -> groceryService.removeRefrigeratorGrocery(refrigeratorGrocery.getId(), request));
    }
}

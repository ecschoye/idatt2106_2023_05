package ntnu.idatt2106.backend.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ntnu.idatt2106.backend.exceptions.NotificationException;
import ntnu.idatt2106.backend.model.User;
import ntnu.idatt2106.backend.model.dto.GroceryNotificationDTO;
import ntnu.idatt2106.backend.model.dto.response.ErrorResponse;
import ntnu.idatt2106.backend.service.CookieService;
import ntnu.idatt2106.backend.service.JwtService;
import ntnu.idatt2106.backend.service.NotificationService;
import ntnu.idatt2106.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification controller", description = "Controller used to handle notifications sent to the user")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final JwtService jwtService;
    private final CookieService cookieService;

    Logger logger = Logger.getLogger(NotificationController.class.getName());


    @Operation(summary = "Retrieves all of the users notifications",
            description = "Retrieves a users notifications",
            responses = {
            @ApiResponse(responseCode = "200", description = "If notifications were retrieved successfully",
            content = @Content(mediaType = "application/json"))
            }
    )
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAll(HttpServletRequest request) throws NotificationException {
        User user = userService.findByEmail(jwtService.extractUsername(cookieService.extractTokenFromCookie(request)));
        logger.info("Received request for retrieving a users notifications");
        try{
            List<GroceryNotificationDTO> notifications = notificationService.getNotifications(user);
            return ResponseEntity.ok(notifications);
        }catch(Exception e){
            throw new NotificationException("Caught exception when retrieving all notifications");
        }
    }

    @PostMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> delete(HttpServletRequest request, @RequestBody long id) throws NotificationException {
        User user = userService.findByEmail(jwtService.extractUsername(cookieService.extractTokenFromCookie(request)));
        GroceryNotificationDTO deleted = notificationService.deleteNotification(user, id);
        if(deleted.getId() == id){
            return ResponseEntity.ok(deleted);
        }
        else{
            throw new NotificationException("Unexpected error occured");
        }
    }
}
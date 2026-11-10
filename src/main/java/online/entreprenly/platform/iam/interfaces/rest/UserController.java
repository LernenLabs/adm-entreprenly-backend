package online.entreprenly.platform.iam.interfaces.rest;

import online.entreprenly.platform.iam.application.commandservices.UserCommandService;
import online.entreprenly.platform.iam.interfaces.rest.resources.ChangePasswordResource;
import online.entreprenly.platform.iam.interfaces.rest.resources.UserResource;
import online.entreprenly.platform.iam.interfaces.rest.transform.ChangePasswordCommandFromResourceAssembler;
import online.entreprenly.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import online.entreprenly.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UserController
 * <p>
 *     Handles account-management operations for the authenticated user. The current user
 *     identity is resolved from the JWT (the token subject is the user's email), so these
 *     endpoints require a valid Bearer token.
 *     <ul>
 *         <li>PUT /api/v1/users/me/password</li>
 *     </ul>
 * </p>
 */
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Authenticated user account management endpoints")
public class UserController {
    private final UserCommandService userCommandService;
    private final online.entreprenly.platform.iam.infrastructure.tokens.jwt.BearerTokenService tokens;

    public UserController(UserCommandService userCommandService,
                          online.entreprenly.platform.iam.infrastructure.tokens.jwt.BearerTokenService tokens) {
        this.tokens = tokens;
        this.userCommandService = userCommandService;
    }

    /**
     * Changes the authenticated user's password.
     * @param resource the change-password request body with current and new password.
     * @param authentication the security context holding the current user's email.
     * @return the updated user resource.
     */
    @PutMapping("/me/password")
    @Operation(
        summary = "Change password",
        description = "Changes the authenticated user's password after verifying the current password."
    )
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordResource resource,
            Authentication authentication, jakarta.servlet.http.HttpServletRequest request) {
        var command = ChangePasswordCommandFromResourceAssembler.toCommandFromResource(
                authentication.getName(), resource, tokens.getBearerTokenFrom(request));
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                UserResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK
        );
    }

}

package online.entreprenly.platform.iam.application.internal.commandservices;

import online.entreprenly.platform.iam.application.commandservices.UserCommandService;
import online.entreprenly.platform.iam.application.internal.outboundservices.hashing.HashingService;
import online.entreprenly.platform.iam.application.internal.outboundservices.tokens.TokenService;
import online.entreprenly.platform.iam.domain.model.aggregates.User;
import online.entreprenly.platform.iam.domain.model.commands.ChangePasswordCommand;
import online.entreprenly.platform.iam.domain.model.commands.SignInCommand;
import online.entreprenly.platform.iam.domain.model.commands.SignUpCommand;
import online.entreprenly.platform.iam.domain.model.events.UserSignedUpEvent;
import online.entreprenly.platform.iam.domain.model.valueobjects.Roles;
import online.entreprenly.platform.iam.domain.repositories.RoleRepository;
import online.entreprenly.platform.iam.domain.repositories.UserRepository;
import online.entreprenly.platform.shared.application.result.ApplicationError;
import online.entreprenly.platform.shared.application.result.Result;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * User command service implementation.
 */
@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            HashingService hashingService,
            TokenService tokenService,
            RoleRepository roleRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<ImmutablePair<User, String>, ApplicationError> handle(SignInCommand command) {
        var user = userRepository.findByEmail(command.email());
        if (user.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.email()));
        }
        if (!hashingService.matches(command.password(), user.get().getPassword())) {
            return Result.failure(ApplicationError.validationError("credentials", "Invalid email or password"));
        }
        var token = tokenService.generateToken(user.get());
        return Result.success(ImmutablePair.of(user.get(), token));
    }

    @Override
    public Result<User, ApplicationError> handle(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            return Result.failure(ApplicationError.conflict("User", "Email already exists"));
        }

        var defaultRole = roleRepository.findByName(Roles.ROLE_USER);
        if (defaultRole.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Role", Roles.ROLE_USER.name()));
        }

        var user = new User(command.email(), hashingService.encode(command.password()), java.util.List.of(defaultRole.get()));
        userRepository.save(user);
        return userRepository.findByEmail(command.email())
                .<Result<User, ApplicationError>>map(savedUser -> {
                    eventPublisher.publishEvent(new UserSignedUpEvent(savedUser.getId(), savedUser.getEmail(),
                            command.firstName(), command.lastName(), command.phone(), command.timezone()));
                    return Result.success(savedUser);
                })
                .orElseGet(() -> Result.failure(ApplicationError.unexpected("sign-up", "Created user could not be reloaded")));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Result<User, ApplicationError> handle(ChangePasswordCommand command) {
        var user = userRepository.findByEmailForUpdate(command.email());
        if (user.isEmpty()) {
            return Result.failure(ApplicationError.notFound("User", command.email()));
        }
        if (!hashingService.matches(command.currentPassword(), user.get().getPassword())) {
            return Result.failure(ApplicationError.validationError("currentPassword", "Current password is incorrect"));
        }
        var session = tokenService.activeSessionId(command.token(), user.get());
        if (session.isEmpty()) {
            return Result.failure(ApplicationError.validationError("session", "Sign in again to change the password"));
        }
        if (command.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return Result.failure(ApplicationError.validationError("newPassword", "Password exceeds 72 bytes"));
        }
        user.get().setPassword(hashingService.encode(command.newPassword()));
        user.get().setSessionVersion(user.get().getSessionVersion() + 1);
        user.get().setRetainedSessionId(session.get());
        return Result.success(userRepository.save(user.get()));
    }

}

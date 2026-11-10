package online.entreprenly.platform.notification.application.internal.commandservices;

import online.entreprenly.platform.notification.application.commandservices.DeviceTokenCommandService;
import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;
import online.entreprenly.platform.notification.domain.model.commands.RegisterDeviceTokenCommand;
import online.entreprenly.platform.notification.domain.model.commands.UnregisterDeviceTokenCommand;
import online.entreprenly.platform.notification.domain.repositories.DeviceTokenRepository;
import online.entreprenly.platform.shared.application.result.ApplicationError;
import online.entreprenly.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Device token command service implementation. All operations are scoped to the owner account.
 */
@Service
public class DeviceTokenCommandServiceImpl implements DeviceTokenCommandService {

    private static final int MAX_TOKEN_LENGTH = 1024;

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenCommandServiceImpl(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public Result<DeviceToken, ApplicationError> handle(RegisterDeviceTokenCommand command) {
        if (command.ownerEmail() == null || command.ownerEmail().isBlank()) {
            return Result.failure(ApplicationError.validationError("owner", "An authenticated owner is required"));
        }
        String token = command.token() == null ? "" : command.token().trim();
        if (token.isEmpty()) {
            return Result.failure(ApplicationError.validationError("token", "A device token is required"));
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            return Result.failure(ApplicationError.validationError("token",
                    "The device token cannot exceed %d characters".formatted(MAX_TOKEN_LENGTH)));
        }
        String platform = command.platform() == null || command.platform().isBlank()
                ? DeviceToken.PLATFORM_ANDROID
                : command.platform().trim().toLowerCase(Locale.ROOT);
        if (!DeviceToken.PLATFORM_ANDROID.equals(platform)) {
            return Result.failure(ApplicationError.validationError("platform", "Only android devices are supported"));
        }
        var deviceToken = deviceTokenRepository.findByToken(token)
                .map(existing -> existing.reassignTo(command.ownerEmail(), platform))
                .orElseGet(() -> new DeviceToken(command.ownerEmail(), token, platform));
        return Result.success(deviceTokenRepository.save(deviceToken));
    }

    @Override
    public Result<Boolean, ApplicationError> handle(UnregisterDeviceTokenCommand command) {
        if (command.ownerEmail() == null || command.ownerEmail().isBlank()) {
            return Result.failure(ApplicationError.validationError("owner", "An authenticated owner is required"));
        }
        String token = command.token() == null ? "" : command.token().trim();
        if (token.isEmpty()) {
            return Result.failure(ApplicationError.validationError("token", "A device token is required"));
        }
        var existing = deviceTokenRepository.findByToken(token);
        if (existing.isPresent() && command.ownerEmail().equals(existing.get().getOwnerEmail())) {
            deviceTokenRepository.deleteByToken(token);
            return Result.success(true);
        }
        return Result.success(false);
    }
}

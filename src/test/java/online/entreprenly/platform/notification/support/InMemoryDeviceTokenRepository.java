package online.entreprenly.platform.notification.support;

import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;
import online.entreprenly.platform.notification.domain.repositories.DeviceTokenRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryDeviceTokenRepository implements DeviceTokenRepository {

    private final List<DeviceToken> tokens = new ArrayList<>();
    private long nextId = 1;

    @Override
    public List<DeviceToken> findAllByOwnerEmail(String ownerEmail) {
        return tokens.stream().filter(t -> t.getOwnerEmail().equals(ownerEmail)).toList();
    }

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return tokens.stream().filter(t -> t.getToken().equals(token)).findFirst();
    }

    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        if (deviceToken.getId() == null) {
            deviceToken.restoreState(nextId++, deviceToken.getOwnerEmail(), deviceToken.getToken(),
                    deviceToken.getPlatform());
            tokens.add(deviceToken);
        }
        return deviceToken;
    }

    @Override
    public void deleteByToken(String token) {
        tokens.removeIf(t -> t.getToken().equals(token));
    }

    public List<DeviceToken> all() {
        return List.copyOf(tokens);
    }
}

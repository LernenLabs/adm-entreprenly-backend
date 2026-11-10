package online.entreprenly.platform.notification.application;

import online.entreprenly.platform.notification.application.internal.commandservices.DeviceTokenCommandServiceImpl;
import online.entreprenly.platform.notification.domain.model.commands.RegisterDeviceTokenCommand;
import online.entreprenly.platform.notification.domain.model.commands.UnregisterDeviceTokenCommand;
import online.entreprenly.platform.notification.support.InMemoryDeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceTokenCommandServiceImplTest {

    private InMemoryDeviceTokenRepository devices;
    private DeviceTokenCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        devices = new InMemoryDeviceTokenRepository();
        service = new DeviceTokenCommandServiceImpl(devices);
    }

    @Test
    void registersATokenForTheOwner() {
        var result = service.handle(new RegisterDeviceTokenCommand("a@b.com", "tok", "android"));

        assertTrue(result.isSuccess());
        assertEquals(1, devices.findAllByOwnerEmail("a@b.com").size());
    }

    @Test
    void registeringTheSameTokenTwiceKeepsOneRecord() {
        service.handle(new RegisterDeviceTokenCommand("a@b.com", "tok", "android"));
        service.handle(new RegisterDeviceTokenCommand("a@b.com", "tok", "android"));

        assertEquals(1, devices.all().size());
    }

    @Test
    void reassignsATokenWhenAnotherAccountSignsInOnTheSameDevice() {
        service.handle(new RegisterDeviceTokenCommand("a@b.com", "tok", "android"));
        service.handle(new RegisterDeviceTokenCommand("c@d.com", "tok", "android"));

        assertTrue(devices.findAllByOwnerEmail("a@b.com").isEmpty());
        assertEquals(1, devices.findAllByOwnerEmail("c@d.com").size());
    }

    @Test
    void rejectsBlankTokenAndUnsupportedPlatform() {
        assertTrue(service.handle(new RegisterDeviceTokenCommand("a@b.com", " ", "android")).isFailure());
        assertTrue(service.handle(new RegisterDeviceTokenCommand("a@b.com", "tok", "ios")).isFailure());
    }

    @Test
    void unregisterIsIdempotentAndOnlyRemovesOwnTokens() {
        service.handle(new RegisterDeviceTokenCommand("a@b.com", "tok", "android"));

        assertTrue(service.handle(new UnregisterDeviceTokenCommand("c@d.com", "tok")).isSuccess());
        assertEquals(1, devices.all().size());

        assertTrue(service.handle(new UnregisterDeviceTokenCommand("a@b.com", "tok")).isSuccess());
        assertTrue(service.handle(new UnregisterDeviceTokenCommand("a@b.com", "tok")).isSuccess());
        assertTrue(devices.all().isEmpty());
    }
}

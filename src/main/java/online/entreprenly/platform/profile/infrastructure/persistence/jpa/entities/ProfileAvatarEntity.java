package online.entreprenly.platform.profile.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profile_avatars")
@Getter
public class ProfileAvatarEntity {
    @Id
    private Long profileId;
    @Column(nullable = false, unique = true)
    private UUID accessKey;
    @Column(nullable = false, length = 32)
    private String mimeType;
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] image;

    protected ProfileAvatarEntity() {}

    public ProfileAvatarEntity(Long profileId, UUID accessKey, String mimeType, byte[] image) {
        this.profileId = profileId;
        this.accessKey = accessKey;
        this.mimeType = mimeType;
        this.image = image;
    }
}

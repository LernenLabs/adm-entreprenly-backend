package online.entreprenly.platform.chatbot.domain.model.aggregates;

import online.entreprenly.platform.chatbot.domain.model.valueobjects.ConversationStatus;
import online.entreprenly.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Getter
public class Conversation extends AbstractDomainAggregateRoot<Conversation> {

    public static final int LAST_MESSAGE_MAX_LENGTH = 500;

    @Setter
    private Long id;
    private Long sellerId;
    private String clientPhone;
    private String clientName;
    private String lastMessage;
    private String lastMessageTime;
    private ConversationStatus status;
    private Instant createdAt;
    private Instant closedAt;

    public Conversation() {
    }

    public Conversation(Long sellerId, String clientPhone, String clientName) {
        this.sellerId = sellerId;
        this.clientPhone = clientPhone;
        this.clientName = clientName;
        this.status = ConversationStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.lastMessage = "";
        this.lastMessageTime = "";
    }

    
    public void registerLastMessage(String content, String time) {
        if (content != null && content.codePointCount(0, content.length()) > LAST_MESSAGE_MAX_LENGTH) {
            content = content.substring(0, content.offsetByCodePoints(0, LAST_MESSAGE_MAX_LENGTH - 1)) + "…";
        }
        this.lastMessage = content;
        this.lastMessageTime = time;
    }

    
    public void changeStatus(ConversationStatus status) {
        if (status == null) return;
        this.status = status;
        this.closedAt = (status == ConversationStatus.CLOSED || status == ConversationStatus.COMPLETED)
                ? Instant.now()
                : null;
    }

    
    public void restoreState(Long id, Long sellerId, String clientPhone, String clientName,
                             String lastMessage, String lastMessageTime, ConversationStatus status,
                             Instant createdAt, Instant closedAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.clientPhone = clientPhone;
        this.clientName = clientName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.status = status;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
    }
}

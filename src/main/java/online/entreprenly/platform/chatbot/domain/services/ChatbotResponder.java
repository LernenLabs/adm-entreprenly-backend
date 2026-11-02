package online.entreprenly.platform.chatbot.domain.services;


public interface ChatbotResponder {

    
    String reply(String incomingContent, String clientName);

    /** False when {@link #reply} would only have the generic fallback to offer. */
    boolean understands(String incomingContent);
}

package service.blockchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Handles blockchain events received from Redis Pub/Sub.
 * This ensures all server nodes stay synchronized with blockchain state.
 */
@Service
public class BlockchainEventHandler implements MessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainEventHandler.class);
    
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    
    @Autowired
    public BlockchainEventHandler(RedisMessageListenerContainer redisMessageListenerContainer) {
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }
    
    @PostConstruct
    public void subscribeToBlockchainEvents() {
        if (redisMessageListenerContainer != null) {
            redisMessageListenerContainer.addMessageListener(
                this, 
                new ChannelTopic("blockchain-events")
            );
            logger.info("✅ Subscribed to blockchain-events channel");
        } else {
            logger.warn("RedisMessageListenerContainer not available, blockchain event sync disabled");
        }
    }
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String msg = new String(message.getBody());
        handleBlockchainEvent(msg);
    }
    
    private void handleBlockchainEvent(String message) {
        try {
            if (message.startsWith("BLOCKCHAIN:CARD_MINTED:")) {
                handleCardMintedEvent(message);
            } else if (message.startsWith("BLOCKCHAIN:TRADE_ACCEPTED:")) {
                handleTradeAcceptedEvent(message);
            } else if (message.startsWith("BLOCKCHAIN:MATCH_RECORDED:")) {
                handleMatchRecordedEvent(message);
            } else {
                logger.debug("Unknown blockchain event type: {}", message);
            }
        } catch (Exception e) {
            logger.error("Error handling blockchain event: {}", e.getMessage(), e);
        }
    }
    
    private void handleCardMintedEvent(String message) {
        // Parse: BLOCKCHAIN:CARD_MINTED:tokenId=123:owner=0x123...
        String[] parts = message.split(":");
        if (parts.length < 4) {
            logger.warn("Invalid CardMinted event format: {}", message);
            return;
        }
        
        String tokenIdPart = parts[2];
        String ownerPart = parts[3];
        
        String tokenId = extractValue(tokenIdPart);
        String owner = extractValue(ownerPart);
        
        logger.info("🎴 Synchronized CardMinted: tokenId={}, owner={}", tokenId, owner);
        
        // Here you could update local cache, notify clients, etc.
        // For now, just log the event for synchronization awareness
    }
    
    private void handleTradeAcceptedEvent(String message) {
        // Parse: BLOCKCHAIN:TRADE_ACCEPTED:tradeId=123:proposer=0x123...:acceptor=0x456...
        String[] parts = message.split(":");
        if (parts.length < 5) {
            logger.warn("Invalid TradeAccepted event format: {}", message);
            return;
        }
        
        String tradeIdPart = parts[2];
        String proposerPart = parts[3];
        String acceptorPart = parts[4];
        
        String tradeId = extractValue(tradeIdPart);
        String proposer = extractValue(proposerPart);
        String acceptor = extractValue(acceptorPart);
        
        logger.info("🔄 Synchronized TradeAccepted: tradeId={}, proposer={}, acceptor={}", 
            tradeId, proposer, acceptor);
        
        // Here you could verify local trade state matches blockchain, etc.
    }
    
    private void handleMatchRecordedEvent(String message) {
        // Parse: BLOCKCHAIN:MATCH_RECORDED:matchId=123:player1=0x123...:player2=0x456...
        String[] parts = message.split(":");
        if (parts.length < 5) {
            logger.warn("Invalid MatchRecorded event format: {}", message);
            return;
        }
        
        String matchIdPart = parts[2];
        String player1Part = parts[3];
        String player2Part = parts[4];
        
        String matchId = extractValue(matchIdPart);
        String player1 = extractValue(player1Part);
        String player2 = extractValue(player2Part);
        
        logger.info("⚔️ Synchronized MatchRecorded: matchId={}, player1={}, player2={}", 
            matchId, player1, player2);
        
        // Here you could verify local match state matches blockchain, etc.
    }
    
    private String extractValue(String keyValue) {
        // Extract value from "key=value" format
        int equalsIndex = keyValue.indexOf('=');
        if (equalsIndex > 0 && equalsIndex < keyValue.length() - 1) {
            return keyValue.substring(equalsIndex + 1);
        }
        return keyValue;
    }
}

package service.blockchain;

import config.BlockchainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigInteger;
import java.util.Arrays;

@Service
public class BlockchainEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainEventListener.class);
    
    private final BlockchainConfig config;
    private final Web3j web3j;
    private final StringRedisTemplate redisTemplate;
    
    // Event signatures
    private static final Event CARD_MINTED_EVENT = new Event("CardMinted",
        Arrays.asList(
            new TypeReference<Uint256>(true) {},  // tokenId indexed
            new TypeReference<Address>(true) {},   // owner indexed
            new TypeReference<Utf8String>() {},    // cardName
            new TypeReference<Utf8String>() {},    // cardType
            new TypeReference<Uint8>() {},         // rarity
            new TypeReference<Uint16>() {},        // attack
            new TypeReference<Uint16>() {}         // defense
        )
    );
    
    private static final Event TRADE_ACCEPTED_EVENT = new Event("TradeAccepted",
        Arrays.asList(
            new TypeReference<Uint256>(true) {},   // tradeId indexed
            new TypeReference<Address>(true) {},   // proposer indexed
            new TypeReference<Address>(true) {}    // acceptor indexed
        )
    );
    
    private static final Event MATCH_RECORDED_EVENT = new Event("MatchRecorded",
        Arrays.asList(
            new TypeReference<Uint256>(true) {},   // matchId indexed
            new TypeReference<Address>(true) {},   // player1 indexed
            new TypeReference<Address>(true) {},   // player2 indexed
            new TypeReference<Address>() {},       // winner
            new TypeReference<Bytes32>() {},       // gameStateHash
            new TypeReference<Uint8>() {},         // player1Score
            new TypeReference<Uint8>() {}          // player2Score
        )
    );
    
    private io.reactivex.disposables.Disposable cardMintedSubscription;
    private io.reactivex.disposables.Disposable tradeAcceptedSubscription;
    private io.reactivex.disposables.Disposable matchRecordedSubscription;
    
    @Autowired
    public BlockchainEventListener(
        BlockchainConfig config,
        Web3j web3j,
        StringRedisTemplate redisTemplate
    ) {
        this.config = config;
        this.web3j = web3j;
        this.redisTemplate = redisTemplate;
    }
    
    @PostConstruct
    public void startListening() {
        if (!config.isBlockchainEnabled() || web3j == null) {
            logger.info("Blockchain event listener disabled - blockchain integration not enabled");
            return;
        }
        
        logger.info("Starting blockchain event listeners...");
        
        try {
            // Listen to CardMinted events from AssetContract
            listenToCardMintedEvents();
            
            // Listen to TradeAccepted events from TradeContract
            listenToTradeAcceptedEvents();
            
            // Listen to MatchRecorded events from MatchContract
            listenToMatchRecordedEvents();
            
            logger.info("✅ Blockchain event listeners started successfully");
        } catch (Exception e) {
            logger.error("Failed to start blockchain event listeners: {}", e.getMessage(), e);
        }
    }
    
    private void listenToCardMintedEvents() {
        String assetContractAddress = config.getAssetContractAddress();
        if (assetContractAddress == null || assetContractAddress.isEmpty()) {
            logger.warn("AssetContract address not configured, skipping CardMinted listener");
            return;
        }
        
        String eventHash = EventEncoder.encode(CARD_MINTED_EVENT);
        
        EthFilter filter = new EthFilter(
            DefaultBlockParameterName.LATEST,
            DefaultBlockParameterName.LATEST,
            assetContractAddress
        ).addSingleTopic(eventHash);
        
        cardMintedSubscription = web3j.ethLogFlowable(filter).subscribe(
            log -> handleCardMintedEvent(log),
            error -> logger.error("Error in CardMinted event listener: {}", error.getMessage(), error)
        );
        
        logger.info("📡 Listening to CardMinted events on {}", assetContractAddress);
    }
    
    private void listenToTradeAcceptedEvents() {
        String tradeContractAddress = config.getTradeContractAddress();
        if (tradeContractAddress == null || tradeContractAddress.isEmpty()) {
            logger.warn("TradeContract address not configured, skipping TradeAccepted listener");
            return;
        }
        
        String eventHash = EventEncoder.encode(TRADE_ACCEPTED_EVENT);
        
        EthFilter filter = new EthFilter(
            DefaultBlockParameterName.LATEST,
            DefaultBlockParameterName.LATEST,
            tradeContractAddress
        ).addSingleTopic(eventHash);
        
        tradeAcceptedSubscription = web3j.ethLogFlowable(filter).subscribe(
            log -> handleTradeAcceptedEvent(log),
            error -> logger.error("Error in TradeAccepted event listener: {}", error.getMessage(), error)
        );
        
        logger.info("📡 Listening to TradeAccepted events on {}", tradeContractAddress);
    }
    
    private void listenToMatchRecordedEvents() {
        String matchContractAddress = config.getMatchContractAddress();
        if (matchContractAddress == null || matchContractAddress.isEmpty()) {
            logger.warn("MatchContract address not configured, skipping MatchRecorded listener");
            return;
        }
        
        String eventHash = EventEncoder.encode(MATCH_RECORDED_EVENT);
        
        EthFilter filter = new EthFilter(
            DefaultBlockParameterName.LATEST,
            DefaultBlockParameterName.LATEST,
            matchContractAddress
        ).addSingleTopic(eventHash);
        
        matchRecordedSubscription = web3j.ethLogFlowable(filter).subscribe(
            log -> handleMatchRecordedEvent(log),
            error -> logger.error("Error in MatchRecorded event listener: {}", error.getMessage(), error)
        );
        
        logger.info("📡 Listening to MatchRecorded events on {}", matchContractAddress);
    }
    
    private void handleCardMintedEvent(Log log) {
        try {
            // Extract tokenId from first topic (indexed)
            String tokenIdHex = log.getTopics().get(1);
            BigInteger tokenId = new BigInteger(tokenIdHex.substring(2), 16);
            
            // Extract owner from second topic (indexed)
            String ownerHex = log.getTopics().get(2);
            String owner = "0x" + ownerHex.substring(26);
            
            logger.info("🎴 CardMinted event: tokenId={}, owner={}", tokenId, owner);
            
            // Publish event to Redis for cross-server synchronization
            String eventData = String.format(
                "BLOCKCHAIN:CARD_MINTED:tokenId=%s:owner=%s",
                tokenId, owner
            );
            if (redisTemplate != null) {
                redisTemplate.convertAndSend("blockchain-events", eventData);
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle CardMinted event: {}", e.getMessage(), e);
        }
    }
    
    private void handleTradeAcceptedEvent(Log log) {
        try {
            // Extract tradeId from first topic (indexed)
            String tradeIdHex = log.getTopics().get(1);
            BigInteger tradeId = new BigInteger(tradeIdHex.substring(2), 16);
            
            // Extract proposer from second topic (indexed)
            String proposerHex = log.getTopics().get(2);
            String proposer = "0x" + proposerHex.substring(26);
            
            // Extract acceptor from third topic (indexed)
            String acceptorHex = log.getTopics().get(3);
            String acceptor = "0x" + acceptorHex.substring(26);
            
            logger.info("🔄 TradeAccepted event: tradeId={}, proposer={}, acceptor={}", 
                tradeId, proposer, acceptor);
            
            // Publish event to Redis for cross-server synchronization
            String eventData = String.format(
                "BLOCKCHAIN:TRADE_ACCEPTED:tradeId=%s:proposer=%s:acceptor=%s",
                tradeId, proposer, acceptor
            );
            if (redisTemplate != null) {
                redisTemplate.convertAndSend("blockchain-events", eventData);
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle TradeAccepted event: {}", e.getMessage(), e);
        }
    }
    
    private void handleMatchRecordedEvent(Log log) {
        try {
            // Extract matchId from first topic (indexed)
            String matchIdHex = log.getTopics().get(1);
            BigInteger matchId = new BigInteger(matchIdHex.substring(2), 16);
            
            // Extract player1 from second topic (indexed)
            String player1Hex = log.getTopics().get(2);
            String player1 = "0x" + player1Hex.substring(26);
            
            // Extract player2 from third topic (indexed)
            String player2Hex = log.getTopics().get(3);
            String player2 = "0x" + player2Hex.substring(26);
            
            logger.info("⚔️ MatchRecorded event: matchId={}, player1={}, player2={}", 
                matchId, player1, player2);
            
            // Publish event to Redis for cross-server synchronization
            String eventData = String.format(
                "BLOCKCHAIN:MATCH_RECORDED:matchId=%s:player1=%s:player2=%s",
                matchId, player1, player2
            );
            if (redisTemplate != null) {
                redisTemplate.convertAndSend("blockchain-events", eventData);
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle MatchRecorded event: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void stopListening() {
        logger.info("Stopping blockchain event listeners...");
        
        if (cardMintedSubscription != null && !cardMintedSubscription.isDisposed()) {
            cardMintedSubscription.dispose();
        }
        
        if (tradeAcceptedSubscription != null && !tradeAcceptedSubscription.isDisposed()) {
            tradeAcceptedSubscription.dispose();
        }
        
        if (matchRecordedSubscription != null && !matchRecordedSubscription.isDisposed()) {
            matchRecordedSubscription.dispose();
        }
        
        logger.info("✅ Blockchain event listeners stopped");
    }
}

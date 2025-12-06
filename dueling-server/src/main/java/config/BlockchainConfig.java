package config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.ContractGasProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class BlockchainConfig {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainConfig.class);
    
    // Shared executor for ALL blockchain transactions to prevent nonce conflicts
    private static final ExecutorService BLOCKCHAIN_EXECUTOR = Executors.newSingleThreadExecutor();

    @Value("${blockchain.enabled:true}")
    private boolean blockchainEnabled;

    @Value("${blockchain.node.url:http://dueling-blockchain:8545}")
    private String nodeUrl;

    @Value("${blockchain.deployment.file:/shared/deployment-info.json}")
    private String deploymentFilePath;

    private String assetContractAddress;
    private String storeContractAddress;
    private String tradeContractAddress;
    private String matchContractAddress;
    private String integrityContractAddress;
    private String oracleRegistryAddress;

    @PostConstruct
    public void loadContractAddresses() {
        if (!blockchainEnabled) {
            logger.info("Blockchain is disabled");
            return;
        }

        try {
            Path deploymentFile = Paths.get(deploymentFilePath);
            
            if (!Files.exists(deploymentFile)) {
                logger.warn("Deployment file not found at: {}", deploymentFilePath);
                logger.warn("Blockchain features may not work correctly");
                return;
            }

            logger.info("Loading contract addresses from: {}", deploymentFilePath);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(deploymentFile.toFile());
            JsonNode contracts = root.get("contracts");
            
            if (contracts == null) {
                logger.error("No 'contracts' field found in deployment file");
                return;
            }
            
            this.assetContractAddress = contracts.get("AssetContract").asText();
            this.storeContractAddress = contracts.get("StoreContract").asText();
            this.tradeContractAddress = contracts.get("TradeContract").asText();
            this.matchContractAddress = contracts.get("MatchContract").asText();
            this.integrityContractAddress = contracts.get("IntegrityContract").asText();
            this.oracleRegistryAddress = contracts.get("OracleRegistry").asText();
            
            logger.info("✅ Contract addresses loaded successfully:");
            logger.info("   AssetContract     : {}", assetContractAddress);
            logger.info("   StoreContract     : {}", storeContractAddress);
            logger.info("   TradeContract     : {}", tradeContractAddress);
            logger.info("   MatchContract     : {}", matchContractAddress);
            logger.info("   IntegrityContract : {}", integrityContractAddress);
            logger.info("   OracleRegistry    : {}", oracleRegistryAddress);
            
        } catch (Exception e) {
            logger.error("Failed to load contract addresses from {}: {}", 
                        deploymentFilePath, e.getMessage());
            logger.error("Blockchain features may not work correctly");
        }
    }

    @Bean
    public Web3j web3j() {
        if (!blockchainEnabled) {
            return null;
        }
        logger.info("Connecting to blockchain node at: {}", nodeUrl);
        return Web3j.build(new HttpService(nodeUrl));
    }

    @Bean
    public ContractGasProvider gasProvider() {
        return new DefaultGasProvider();
    }
    
    /**
     * Shared blockchain executor to prevent nonce conflicts.
     * ALL blockchain transactions MUST go through this executor.
     */
    @Bean
    public ExecutorService blockchainExecutor() {
        logger.info("Creating shared blockchain executor for nonce management");
        return BLOCKCHAIN_EXECUTOR;
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down blockchain executor...");
        BLOCKCHAIN_EXECUTOR.shutdown();
        try {
            if (!BLOCKCHAIN_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                BLOCKCHAIN_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            BLOCKCHAIN_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean isBlockchainEnabled() {
        return blockchainEnabled;
    }

    public String getAssetContractAddress() {
        return assetContractAddress;
    }

    public String getStoreContractAddress() {
        return storeContractAddress;
    }

    public String getTradeContractAddress() {
        return tradeContractAddress;
    }

    public String getMatchContractAddress() {
        return matchContractAddress;
    }

    public String getIntegrityContractAddress() {
        return integrityContractAddress;
    }

    public String getOracleRegistryAddress() {
        return oracleRegistryAddress;
    }
}

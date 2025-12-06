package service.oracle;

import config.BlockchainConfig;
import model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

/**
 * Service to interact with IntegrityContract on blockchain
 */
@Service
public class IntegrityContractService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegrityContractService.class);
    
    private final BlockchainConfig config;
    private final Web3j web3j;
    private final ContractGasProvider gasProvider;
    private final TransactionManager transactionManager;
    private final String integrityContractAddress;
    private final java.util.concurrent.ExecutorService blockchainExecutor;
    
    @Autowired
    public IntegrityContractService(
        BlockchainConfig config,
        Web3j web3j,
        ContractGasProvider gasProvider,
        java.util.concurrent.ExecutorService blockchainExecutor
    ) {
        this.config = config;
        this.web3j = web3j;
        this.gasProvider = gasProvider;
        this.blockchainExecutor = blockchainExecutor;
        
        // Get contract address from config
        this.integrityContractAddress = config.getIntegrityContractAddress();
        
        if (config.isBlockchainEnabled() && web3j != null) {
            String privateKey = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";
            Credentials credentials = Credentials.create(privateKey);
            
            // Use FastRawTransactionManager with automatic nonce management
            PollingTransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(
                web3j,
                1000, // polling interval
                30    // max attempts
            );
            
            this.transactionManager = new FastRawTransactionManager(
                web3j,
                credentials,
                1337L, // chain ID
                receiptProcessor
            );
            
            logger.info("IntegrityContractService initialized with FastRawTransactionManager");
            logger.info("Contract: {}", integrityContractAddress);
        } else {
            this.transactionManager = null;
            logger.warn("IntegrityContractService - Blockchain DISABLED");
        }
    }
    
    /**
     * Record purchase proof on blockchain
     */
    public void recordPurchaseProof(String operationId, byte[] dataHash, Player player) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return;
        }
        
        // Submit to shared executor to prevent nonce conflicts
        blockchainExecutor.submit(() -> {
            try {
                String playerAddress = getPlayerAddress(player);
                
                // Convert byte[] to bytes32
                byte[] bytes32Hash = new byte[32];
                System.arraycopy(dataHash, 0, bytes32Hash, 0, Math.min(dataHash.length, 32));
                
                // Call recordPurchaseProof(string operationId, bytes32 dataHash, address playerAddress)
                Function function = new Function(
                    "recordPurchaseProof",
                    Arrays.asList(
                        new Utf8String(operationId),
                        new Bytes32(bytes32Hash),
                        new org.web3j.abi.datatypes.Address(playerAddress)
                    ),
                    Collections.emptyList()
                );
                
                String encodedFunction = FunctionEncoder.encode(function);
                
                org.web3j.protocol.core.methods.response.EthSendTransaction response =
                    transactionManager.sendTransaction(
                        gasProvider.getGasPrice(),
                        gasProvider.getGasLimit(),
                        integrityContractAddress,
                        encodedFunction,
                        BigInteger.ZERO
                    );
                
                if (response.hasError()) {
                    throw new RuntimeException("Blockchain error: " + response.getError().getMessage());
                }
                
                String txHash = response.getTransactionHash();
                logger.debug("📝 Purchase proof tx submitted: {}", txHash);
                
                // Wait for confirmation (optional, since it's async)
                waitForReceipt(txHash);
                
            } catch (Exception e) {
                logger.error("Failed to record purchase proof on blockchain: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * Record trade proof on blockchain
     */
    public void recordTradeProof(String operationId, byte[] dataHash, Player player1, Player player2) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return;
        }
        
        // Submit to shared executor to prevent nonce conflicts
        blockchainExecutor.submit(() -> {
            try {
                String player1Address = getPlayerAddress(player1);
                String player2Address = getPlayerAddress(player2);
                
                byte[] bytes32Hash = new byte[32];
                System.arraycopy(dataHash, 0, bytes32Hash, 0, Math.min(dataHash.length, 32));
                
                Function function = new Function(
                    "recordTradeProof",
                    Arrays.asList(
                        new Utf8String(operationId),
                        new Bytes32(bytes32Hash),
                        new org.web3j.abi.datatypes.Address(player1Address),
                        new org.web3j.abi.datatypes.Address(player2Address)
                    ),
                    Collections.emptyList()
                );
                
                String encodedFunction = FunctionEncoder.encode(function);
                
                org.web3j.protocol.core.methods.response.EthSendTransaction response =
                    transactionManager.sendTransaction(
                        gasProvider.getGasPrice(),
                        gasProvider.getGasLimit(),
                        integrityContractAddress,
                        encodedFunction,
                        BigInteger.ZERO
                    );
                
                if (response.hasError()) {
                    throw new RuntimeException("Blockchain error: " + response.getError().getMessage());
                }
                
                logger.debug("📝 Trade proof tx submitted: {}", response.getTransactionHash());
                waitForReceipt(response.getTransactionHash());
                
            } catch (Exception e) {
                logger.error("Failed to record trade proof on blockchain: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * Record match proof on blockchain
     */
    public void recordMatchProof(String operationId, byte[] dataHash, Player winner, Player loser) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return;
        }
        
        // Submit to shared executor to prevent nonce conflicts
        blockchainExecutor.submit(() -> {
            try {
                String winnerAddress = getPlayerAddress(winner);
                String loserAddress = getPlayerAddress(loser);
                
                byte[] bytes32Hash = new byte[32];
                System.arraycopy(dataHash, 0, bytes32Hash, 0, Math.min(dataHash.length, 32));
                
                Function function = new Function(
                    "recordMatchProof",
                    Arrays.asList(
                        new Utf8String(operationId),
                        new Bytes32(bytes32Hash),
                        new org.web3j.abi.datatypes.Address(winnerAddress),
                        new org.web3j.abi.datatypes.Address(loserAddress)
                    ),
                    Collections.emptyList()
                );
                
                String encodedFunction = FunctionEncoder.encode(function);
                
                org.web3j.protocol.core.methods.response.EthSendTransaction response =
                    transactionManager.sendTransaction(
                        gasProvider.getGasPrice(),
                        gasProvider.getGasLimit(),
                        integrityContractAddress,
                        encodedFunction,
                        BigInteger.ZERO
                    );
                
                if (response.hasError()) {
                    throw new RuntimeException("Blockchain error: " + response.getError().getMessage());
                }
                
                logger.debug("📝 Match proof tx submitted: {}", response.getTransactionHash());
                waitForReceipt(response.getTransactionHash());
                
            } catch (Exception e) {
                logger.error("Failed to record match proof on blockchain: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * Verify a proof on blockchain
     */
    public boolean verifyProof(String operationId, byte[] providedHash) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return false;
        }
        
        try {
            byte[] bytes32Hash = new byte[32];
            System.arraycopy(providedHash, 0, bytes32Hash, 0, Math.min(providedHash.length, 32));
            
            Function function = new Function(
                "verifyProof",
                Arrays.asList(
                    new Utf8String(operationId),
                    new Bytes32(bytes32Hash)
                ),
                Arrays.asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Bool>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            
            org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    transactionManager.getFromAddress(),
                    integrityContractAddress,
                    encodedFunction
                ),
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST
            ).send();
            
            if (response.hasError()) {
                logger.error("Verification call error: {}", response.getError().getMessage());
                return false;
            }
            
            String value = response.getValue();
            // Parse boolean response (0x0...0 = false, 0x0...1 = true)
            return value != null && !value.equals("0x0000000000000000000000000000000000000000000000000000000000000000");
            
        } catch (Exception e) {
            logger.error("Failed to verify proof: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get player's Ethereum address
     */
    private String getPlayerAddress(Player player) {
        // Use deterministic address generation based on player ID
        // In production, this should map to actual player wallets
        int hash = player.getId().hashCode();
        int index = Math.abs(hash % 20); // Use first 20 Hardhat accounts
        
        // Hardhat test accounts
        String[] accounts = {
            "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
            "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            "0x3C44CDDdB6a900fa2b585dd299e03d12FA4293BC",
            "0x90F79bf6EB2c4f870365E785982E1f101E93b906",
            "0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65",
            "0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc",
            "0x976EA74026E726554dB657fA54763abd0C3a0aa9",
            "0x14dC79964da2C08b23698B3D3cc7Ca32193d9955",
            "0x23618e81E3f5cdF7f54C3d65f7FBc0aBf5B21E8f",
            "0xa0Ee7A142d267C1f36714E4a8F75612F20a79720",
            "0xBcd4042DE499D14e55001CcbB24a551F3b954096",
            "0x71bE63f3384f5fb98995898A86B02Fb2426c5788",
            "0xFABB0ac9d68B0B445fB7357272Ff202C5651694a",
            "0x1CBd3b2770909D4e10f157cABC84C7264073C9Ec",
            "0xdF3e18d64BC6A983f673Ab319CCaE4f1a57C7097",
            "0xcd3B766CCDd6AE721141F452C550Ca635964ce71",
            "0x2546BcD3c84621e976D8185a91A922aE77ECEc30",
            "0xbDA5747bFD65F08deb54cb465eB87D40e51B197E",
            "0xdD2FD4581271e230360230F9337D5c0430Bf44C0",
            "0x8626f6940E2eb28930eFb4CeF49B2d1F2C9C1199"
        };
        
        return accounts[index];
    }
    
    /**
     * Wait for transaction receipt
     */
    private void waitForReceipt(String txHash) {
        try {
            TransactionReceipt receipt = null;
            int attempts = 0;
            int maxAttempts = 10;
            
            while (receipt == null && attempts < maxAttempts) {
                attempts++;
                try {
                    var receiptResponse = web3j.ethGetTransactionReceipt(txHash).send();
                    if (receiptResponse.getTransactionReceipt().isPresent()) {
                        receipt = receiptResponse.getTransactionReceipt().get();
                        break;
                    }
                } catch (Exception e) {
                    logger.debug("Attempt {} to get receipt failed", attempts);
                }
                
                if (receipt == null) {
                    Thread.sleep(500);
                }
            }
            
            if (receipt != null) {
                logger.debug("✅ Proof recorded - Tx: {}", txHash);
            } else {
                logger.warn("⚠️  Could not confirm proof transaction: {}", txHash);
            }
            
        } catch (Exception e) {
            logger.error("Error waiting for receipt: {}", e.getMessage());
        }
    }
}

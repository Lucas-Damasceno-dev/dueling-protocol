package test;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint8;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;

public class TestFunctionEncoding {
    public static void main(String[] args) throws Exception {
        String player1 = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
        String player2 = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC";
        String winner = player2;
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] matchIdHash = digest.digest("test-match".getBytes(StandardCharsets.UTF_8));
        
        Function function = new Function(
            "recordMatch",
            Arrays.asList(
                new Address(player1),
                new Address(player2),
                new Address(winner),
                new Bytes32(matchIdHash),
                new Uint8(0),
                new Uint8(100)
            ),
            Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);
        System.out.println("Encoded function:");
        System.out.println(encodedFunction);
        System.out.println("\nFunction selector (first 10 chars):");
        System.out.println(encodedFunction.substring(0, 10));
    }
}

package org.adridadou.ethereum;

import org.adridadou.ethereum.smartcontract.SolidityContract;
import org.adridadou.ethereum.smartcontract.SolidityContractTest;
import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.blockchain.StandaloneBlockchain;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import static org.ethereum.config.blockchain.FrontierConfig.*;

/**
 * Created by davidroon on 08.04.16.
 * This code is released under Apache 2 license
 */
public class BlockchainProxyTest implements BlockchainProxy {
    private final StandaloneBlockchain blockchain;

    public BlockchainProxyTest() {

        SystemProperties.getDefault().setBlockchainConfig(new FrontierConfig(new FrontierConstants() {
            @Override
            public BigInteger getMINIMUM_DIFFICULTY() {
                return BigInteger.ONE;
            }
        }));

        blockchain = new StandaloneBlockchain();
        blockchain.withAutoblock(true);
    }

    @Override
    public SolidityContract map(String src, String contractName, EthAddress address, ECKey sender) {
        return new SolidityContractTest(blockchain.createExistingContractFromSrc(src, address.address));
    }

    @Override
    public SolidityContract mapFromAbi(String abi, EthAddress address, ECKey sender) {
        return new SolidityContractTest(blockchain.createExistingContractFromABI(abi, address.address));
    }

    @Override
    public CompletableFuture<EthAddress> publish(String code, String contractName, ECKey sender) {
        EthAddress address = EthAddress.of(blockchain.submitNewContract(code).getAddress());
        CompletableFuture<EthAddress> future = new CompletableFuture<>();
        future.complete(address);
        return future;
    }

    @Override
    public long getCurrentBlockNumber() {
        return 0;
    }

}

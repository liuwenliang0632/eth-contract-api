package org.adridadou.ethereum.smartcontract;

import org.adridadou.ethereum.EthAddress;
import org.adridadou.ethereum.EthereumListenerImpl;
import org.ethereum.core.*;
import org.ethereum.core.CallTransaction.Contract;
import org.ethereum.crypto.ECKey;
import org.ethereum.facade.Ethereum;
import org.ethereum.util.ByteUtil;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

/**
 * Created by davidroon on 20.04.16.
 * This code is released under Apache 2 license
 */
public class SolidityContract {
    private EthAddress address;
    private Contract contract;
    private final Ethereum ethereum;
    private final EthereumListenerImpl ethereumListener;
    private final ECKey sender;

    protected SolidityContract() {
        this.ethereumListener = null;
        this.contract = null;
        this.ethereum = null;
        this.sender = null;
    }

    public SolidityContract(String abi, Ethereum ethereum, EthereumListenerImpl ethereumListener, ECKey sender) {
        this.ethereumListener = ethereumListener;
        this.contract = new Contract(abi);
        this.ethereum = ethereum;
        this.sender = sender;
    }

    public Object[] callConstFunction(Block callBlock, String functionName, Object... args) {

        Transaction tx = CallTransaction.createCallTransaction(0, 0, 100000000000000L,
                address.toString(), 0, contract.getByName(functionName), args);
        tx.sign(ECKey.fromPrivate(new byte[32]));

        Repository repository = getRepository().getSnapshotTo(callBlock.getStateRoot()).startTracking();

        try {
            TransactionExecutor executor = new TransactionExecutor
                    (tx, callBlock.getCoinbase(), repository, getBlockchain().getBlockStore(),
                            getBlockchain().getProgramInvokeFactory(), callBlock)
                    .setLocalCall(true);

            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();

            return contract.getByName(functionName).decodeResult(executor.getResult().getHReturn());
        } finally {
            repository.rollback();
        }
    }

    private CompletableFuture<TransactionReceipt> sendTx(EthAddress receiveAddress, byte[] data, long value) throws InterruptedException {
        BigInteger nonce = getRepository().getNonce(sender.getAddress());
        Transaction tx = new Transaction(
                ByteUtil.bigIntegerToBytes(nonce),
                ByteUtil.longToBytesNoLeadZeroes(ethereum.getGasPrice()),
                ByteUtil.longToBytesNoLeadZeroes(3_000_000),
                receiveAddress.address,
                ByteUtil.longToBytesNoLeadZeroes(value),
                data);
        tx.sign(sender);
        ethereum.submitTransaction(tx);
        return ethereumListener.registerTx(tx.getHash());
    }

    public void setAddress(EthAddress address) {
        this.address = address;
    }

    private BlockchainImpl getBlockchain() {
        return (BlockchainImpl) ethereum.getBlockchain();
    }

    private Repository getRepository() {
        return getBlockchain().getRepository();
    }


    public CompletableFuture<Object[]> callFunction(String functionName, Object... args) {
        return callFunction(0, functionName, args);
    }

    public CompletableFuture<Object[]> callFunction(long value, String functionName, Object... args) {
        CallTransaction.Function inc = contract.getByName(functionName);
        byte[] functionCallBytes = inc.encode(args);
        try {
            return sendTx(address, functionCallBytes, value).thenApply(receipt -> contract.getByName(functionName).decodeResult(receipt.getExecutionResult()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Object[] callConstFunction(String functionName, Object... args) {
        return callConstFunction(getBlockchain().getBestBlock(), functionName, args);
    }

    public EthAddress getAddress() {
        return address;
    }
}

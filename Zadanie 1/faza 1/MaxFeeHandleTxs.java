import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class MaxFeeHandleTxs {

    private UTXOPool utxoPool;

    public MaxFeeHandleTxs(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public UTXOPool UTXOPoolGet() {
        if (utxoPool == null) {
            return new UTXOPool();
        }
        return utxoPool;
    }

    public boolean txIsValid(Transaction tx) {
        HashSet<UTXO> claimedUTXOs = new HashSet<UTXO>();
        double inputSum = 0;
        double outputSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            if (!utxoPool.contains(utxo)) {
                return false;
            }

            Transaction.Output correspondingOutput = utxoPool.getTxOutput(utxo);

            RSAKey publicKey = correspondingOutput.address;
            byte[] message = tx.getDataToSign(i);
            byte[] signature = input.signature;
            if (signature == null || !publicKey.verifySignature(message, signature)) {
                return false;
            }

            if (claimedUTXOs.contains(utxo)) {
                return false;
            }
            claimedUTXOs.add(utxo);

            inputSum += correspondingOutput.value;
        }

        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output output = tx.getOutput(i);
            if (output.value < 0) {
                return false;
            }
            outputSum += output.value;
        }

        if (inputSum < outputSum) {
            return false;
        }

        return true;
    }

    private double calculateFee(Transaction tx) {
        double inputSum = 0;
        double outputSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!utxoPool.contains(utxo)) {
                return -1;
            }
            Transaction.Output correspondingOutput = utxoPool.getTxOutput(utxo);
            inputSum += correspondingOutput.value;
        }

        for (int i = 0; i < tx.numOutputs(); i++) {
            outputSum += tx.getOutput(i).value;
        }

        return inputSum - outputSum;
    }

    public Transaction[] handler(Transaction[] possibleTxs) {
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();

        ArrayList<Transaction> candidates = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (tx != null) {
                candidates.add(tx);
            }
        }

        // Greedy-by-fee: v každom kole zoradíme podľa poplatku zostupne
        // a vyberieme najlepšiu platnú transakciu. Opakujeme.
        boolean changed = true;
        while (changed) {
            changed = false;

            ArrayList<TransactionWithFee> validWithFees = new ArrayList<TransactionWithFee>();
            for (Transaction tx : candidates) {
                if (txIsValid(tx)) {
                    double fee = calculateFee(tx);
                    if (fee >= 0) {
                        validWithFees.add(new TransactionWithFee(tx, fee));
                    }
                }
            }

            Collections.sort(validWithFees, new Comparator<TransactionWithFee>() {
                public int compare(TransactionWithFee a, TransactionWithFee b) {
                    return Double.compare(b.fee, a.fee);
                }
            });

            for (TransactionWithFee twf : validWithFees) {
                Transaction tx = twf.tx;

                if (txIsValid(tx)) {
                    acceptedTxs.add(tx);
                    candidates.remove(tx);

                    for (int j = 0; j < tx.numInputs(); j++) {
                        Transaction.Input input = tx.getInput(j);
                        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                        utxoPool.removeUTXO(utxo);
                    }

                    byte[] txHash = tx.getHash();
                    for (int j = 0; j < tx.numOutputs(); j++) {
                        UTXO utxo = new UTXO(txHash, j);
                        utxoPool.addUTXO(utxo, tx.getOutput(j));
                    }

                    changed = true;
                    break; // Reštartujeme iteráciu s aktualizovaným poolom
                }
            }
        }

        return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }

    private class TransactionWithFee {
        Transaction tx;
        double fee;

        TransactionWithFee(Transaction tx, double fee) {
            this.tx = tx;
            this.fee = fee;
        }
    }
}

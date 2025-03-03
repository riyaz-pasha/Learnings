import { ec as EC } from 'elliptic';
import { existsSync, readFileSync, writeFileSync } from 'fs';
import { getTransactionId, signTxIn, Transaction, TxIn, TxOut, UnspentTxOut } from './transaction';

const ec = new EC('secp256k1');
const privateKeyLocation = "node/wallet/private_key";

const generatePrivateKey = (): string => {
    const keyPair = ec.genKeyPair();
    const privateKey = keyPair.getPrivate();
    return privateKey.toString('hex');
}

const getPrivateKeyFromWallet = () => {
    const buffer = readFileSync(privateKeyLocation)
    return buffer.toString();
}

const getPublicKeyFromWallet = (): string => {
    const privateKey = getPrivateKeyFromWallet();
    const key = ec.keyFromPrivate(privateKey, 'hex');
    return key.getPublic().encode('hex', true);
}

const getBalance = (address: string, unspentTxOuts: UnspentTxOut[]): number => {
    return unspentTxOuts.filter(txOut => txOut.address === address)
        .map(txOut => txOut.amount)
        .reduce((a, b) => a + b, 0);
}

const findTxOutsForAmount = (amount: number, userUnpsentTxOuts: UnspentTxOut[]) => {
    let currentAmount = 0;
    const includedUnspentTxOuts = [];
    for (const unspentTxOut of userUnpsentTxOuts) {
        includedUnspentTxOuts.push(unspentTxOut);
        currentAmount += unspentTxOut.amount;
        if (currentAmount >= amount) {
            const leftOverAmount = currentAmount - amount;
            return { includedUnspentTxOuts, leftOverAmount };
        }
    }
    throw Error('not enough coins to send transaction');
}

const toUnsignedTxIn = (unspentTxOut: UnspentTxOut) => {
    const txIn = new TxIn();
    txIn.txOutId = unspentTxOut.txOutId;
    txIn.txOutIndex = unspentTxOut.txOutIndex;
    return txIn;
}

const createTxOuts = (receiverAddress: string, senderAddress: string, amount: number, leftOverAmount: number) => {
    const txOuts: TxOut[] = [];
    txOuts.push(new TxOut(receiverAddress, amount));
    if (leftOverAmount !== 0) {
        txOuts.push(new TxOut(senderAddress, leftOverAmount));
    }
    return txOuts;
}



const createTransaction = (
    receiverAddress: string,
    amount: number,
    unspentTxOuts: UnspentTxOut[]): Transaction => {

    const privateKey: string = getPrivateKeyFromWallet();
    const senderAddress: string = getPublicKeyFromWallet();
    const senderUnspentTxOuts = unspentTxOuts.filter(txOut => txOut.address === senderAddress);

    const { leftOverAmount, includedUnspentTxOuts } = findTxOutsForAmount(amount, senderUnspentTxOuts);
    const unsignedTxIns: TxIn[] = includedUnspentTxOuts.map(toUnsignedTxIn);

    const tx = new Transaction();
    tx.txIns = unsignedTxIns;
    tx.txOuts = createTxOuts(receiverAddress, senderAddress, amount, leftOverAmount);
    tx.id = getTransactionId(tx);

    tx.txIns = tx.txIns.map((txIn: TxIn, index: number) => {
        txIn.signature = signTxIn(tx, index, privateKey, unspentTxOuts);
        return txIn;
    });

    return tx;
}

const initWallet = () => {
    if (existsSync(privateKeyLocation)) {
        return;
    }
    const newPrivateKey = generatePrivateKey();
    writeFileSync(privateKeyLocation, newPrivateKey);
    console.log(`new wallet created and private key stored in the given location`);
}

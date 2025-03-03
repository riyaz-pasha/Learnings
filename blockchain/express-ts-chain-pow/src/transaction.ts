import { SHA256 } from "crypto-js";
import { ec as EC } from 'elliptic';

const ec = new EC('secp256k1');

class TxOut {
    constructor(
        public address: string,
        public amount: number,
    ) { }
}

class TxIn {
    public txOutId: string;
    public txOutIndex: number;
    public signature: string;
}

class Transaction {
    public id: string;
    public txIns: TxIn[];
    public txOuts: TxOut[];
}

class UnspentTxOut {
    constructor(
        public readonly txOutId: string,
        public readonly txOutIndex: number,
        public readonly address: string,
        public readonly amount: number,
    ) { }
}

const getTransactionId = (tx: Transaction): string => {
    const txInContent: string = tx.txIns
        .map(txIn => txIn.txOutId + txIn.txOutIndex)
        .join('');

    const txOutContent: string = tx.txOuts
        .map(txOut => txOut.address + txOut.amount)
        .join('');

    return SHA256(txInContent + txOutContent).toString();
}

const signTxIn = (
    tx: Transaction,
    txInIndex: number,
    privateKey: string,
    unspentTxOuts: UnspentTxOut[]): string => {
    const txIn: TxIn = tx.txIns[txInIndex];
    const dataToSign = tx.id;
    const referencedUnspentTxOut: UnspentTxOut = findUnspentTxOut(txIn.txOutId, txIn.txOutIndex, unspentTxOuts);
    const referencedAddress = referencedUnspentTxOut.address;
    const key = ec.keyFromPrivate(privateKey, 'hex');
    const signature: string = toHexString(key.sign(dataToSign).toDER());
    return signature;
}

let unspentTxOuts: UnspentTxOut[] = [];

const newUnspentTxOuts: UnspentTxOut[] = newTxs
    .map((tx: Transaction) => tx.txOuts.map((txOut, index) => new UnspentTxOut(tx.id, index, txOut.address, txOut.amount)))
    .reduce((a, b) => a.concat(b), []);

const consumedTxOuts: UnspentTxOut[] = newTxs
    .map((tx: Transaction) => tx.txIns)
    .reduce((a, b) => a.concat(b), [])
    .map((txIn: TxIn) => new UnspentTxOut(txIn.txOutId, txIn.txOutIndex, '', '0'));

const resultingUnspentTxOuts = unspentTxOuts
    .filter((txOut) => !findUnpsentTxOut(txOut.txOutId, txOut.txOutIndex, consumedTxOuts))
    .concat(newUnspentTxOuts);

//valid address is a valid ecdsa public key in the 04 + X-coordinate + Y-coordinate format
const isValidAddress = (address: string): boolean => {
    if (address.length !== 130) {
        console.log('invalid public key length');
        return false;
    }
    if (address.match('^[a-fA-F0-9]+$') === null) {
        console.log('public key must contain only hex characters');
        return false;
    }
    if (!address.startsWith('04')) {
        console.log('public key must start with 04');
        return false;
    }
    return true;
};

const isValidTxInStructure = (txIn: TxIn): boolean => {
    if (txIn == null) {
        return false;
    }
    if (typeof txIn.signature !== 'string') {
        console.log('invalid signature type in txIn');
        return false;
    }
    if (typeof txIn.txOutId !== 'string') {
        console.log('invalid txOutId type in txIn');
        return false;
    }
    if (typeof txIn.txOutIndex !== 'number') {
        console.log('invalid txOutIndex type in txIn');
        return false;
    }
    return true;
}

const isValidTxOutStructure = (txOut: TxOut): boolean => {
    if (txOut == null) {
        console.log('txOut is null');
        return false;
    } else if (typeof txOut.address !== 'string') {
        console.log('invalid address type in txOut');
        return false;
    } else if (!isValidAddress(txOut.address)) {
        console.log('invalid TxOut address');
        return false;
    } else if (typeof txOut.amount !== 'number') {
        console.log('invalid amount type in txOut');
        return false;
    } else {
        return true;
    }
};

const isValidTxStructure = (tx: Transaction) => {
    if (typeof tx.id !== 'string') {
        console.log('transactionId is missing');
        return false;
    }
    if (!(tx.txIns instanceof Array)) {
        console.log('invalid txIns type in transaction');
        return false;
    }
    if (!tx.txIns.map(isValidTxInStructure).every(Boolean)) {
        return false;
    }
    if (!(tx.txOuts instanceof Array)) {
        console.log('invalid txOuts type in transaction');
        return false;
    }
    if (!tx.txOuts.map(isValidTxOutStructure).every(Boolean)) {
        return false;
    }
    return true;
}

const isValidtxsStructure = (transactions: Transaction[]): boolean => {
    return transactions
        .map(isValidTxStructure)
        .every(Boolean);
};

const validateTxIn = (
    txIn: TxIn,
    tx: Transaction,
    unspentTxOuts: UnspentTxOut[],
): boolean => {
    const referencedTxOut: UnspentTxOut | undefined = unspentTxOuts.find(txOut => txOut.txOutId === txIn.txOutId);
    if (!referencedTxOut) {
        console.log('referenced txOut not found: ' + JSON.stringify(txIn));
        return false;
    }

    const key = ec.keyFromPublic(referencedTxOut.address, 'hex');
    return key.verify(tx.id, txIn.signature);
}

const validateTx = (tx: Transaction, unspentTxOuts: UnspentTxOut[]): boolean => {
    if (getTransactionId(tx) !== tx.id) {
        console.log(`invalid tx id: ${tx.id}`);
        return false;
    }

    const hasValidTxIns: boolean = tx.txIns
        .map((txIn) => validateTxIn(txIn, tx, unspentTxOuts))
        .every(Boolean);

    if (!hasValidTxIns) {
        console.log(`some of the txIns are invalid in tx: ${tx.id}`);
        return false;
    }

    const totalTxInAmount: number = tx.txIns
        .map(txIn => getTxInAmount(txIn, unspentTxOuts))
        .reduce((a, b) => a + b, 0);

    const totaTxOutAmount: number = tx.txOuts
        .map(txOut => txOut.amount)
        .reduce((a, b) => a + b, 0)

    if (totalTxInAmount !== totaTxOutAmount) {
        console.log(`totalTxInAmount !== totaTxOutAmount in tx: ${tx.id}`);
        return false;
    }
    return true;
}

const validateConibaseTx = (tx: Transaction, blockIndex: number): boolean => {
    if (getTransactionId(tx) !== tx.id) {
        console.log(`invalid coinbase tx id: ${tx.id}`);
        return false;
    }
    if (tx.txIns.length !== 1) {
        console.log('one txIn must be specified in the coinbase transaction');
        return false;
    }
    if (tx.txIns[0].txOutIndex !== blockIndex) {
        console.log('the txIn index in coinbase tx must be the block height');
        return false;
    }
    if (tx.txOuts.length !== 1) {
        console.log('invalid number of txOuts in coinbase transaction');
        return false;
    }
    if (tx.txOuts[0].amount != COINBASE_AMOUNT) {
        console.log('invalid coinbase amount in coinbase transaction');
        return false;
    }
    return true;
}

const validateBlockTxs = (txs: Transaction[], unspentTxOuts: UnspentTxOut[], blockIndex: number): boolean => {
    const conibaseTx = txs[0];
    if (!validateConibaseTx(conibaseTx, blockIndex)) {
        console.log(`invalid conibase transaction ${JSON.stringify(conibaseTx)}`);
        return false;
    }
    const txIns: TxIn[] = txs.map(tx => tx.txIns).flat();
    if (hasDuplicates(txIns)) return false;

    const normalTxs: Transaction[] = txs.slice(1);
    return normalTxs.map(tx => validateTx(tx, unspentTxOuts)).every(Boolean);
}

const hasDuplicates = (txIns: TxIn[]): boolean => {
    const seen = new Set();
    return txIns.some(txIn => {
        if (seen.has(txIn.txOutId)) return true;

        seen.add(txIn.txOutId);
        return false;
    })
}

const getTxInAmount = (txIn: TxIn, unspentTxOuts: UnspentTxOut[]): number => {
    return findUnspentTxOut(txIn.txOutId, txIn.txOutIndex, unspentTxOuts)?.amount || 0;
}

const findUnspentTxOut = (transactionId: string, index: number, unspentTxOuts: UnspentTxOut[]): UnspentTxOut | undefined => {
    return unspentTxOuts.find((uTxO) => uTxO.txOutId === transactionId && uTxO.txOutIndex === index);
};


const COINBASE_AMOUNT: number = 50;

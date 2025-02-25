import { SHA256 } from "crypto-js";

// in seconds
const BLOCK_GENERATION_INTERVAL: number = 10;

// in blocks
const DIFFICULTY_ADJUSTMENT_INTERVAL: number = 10;

// Type Branding for better type safety
type BlockIndex = number & { readonly _: 'BlockIndex' };
type BlockHash = string & { readonly _: 'BlockHash' };
type Timestamp = number & { readonly _: 'Timestamp' };
type BlockData = string & { readonly _: 'BlockData' };
type Difficulty = number & { readonly _: 'Difficulty' };
type Nonce = number & { readonly _: 'Nonce' };

// Factory Functions
const createBlockIndex = (index: number): BlockIndex => index as BlockIndex;
const createBlockHash = (hash: string): BlockHash => hash as BlockHash;
const createTimestamp = (): Timestamp => (Date.now() / 1000) as Timestamp;
const createBlockData = (data: string): BlockData => data as BlockData;
const createDifficulty = (difficulty: number): Difficulty => difficulty as Difficulty;
const createNonce = (nonce: number): Nonce => nonce as Nonce;

const calculateHash = (index: BlockIndex, previousHash: BlockHash, timestamp: Timestamp, data: BlockData, difficulty: Difficulty, nonce: Nonce): string =>
    SHA256(index + previousHash + timestamp + data + difficulty + nonce).toString();

const calculateHashForBlock = (block: Block): BlockHash =>
    createBlockHash(calculateHash(block.index, block.previousHash, block.timestamp, block.data, block.difficulty, block.nonce));

class Block {
    constructor(
        public index: BlockIndex,
        public hash: BlockHash,
        public previousHash: BlockHash,
        public timestamp: Timestamp,
        public data: BlockData,
        public difficulty: Difficulty,
        public nonce: Nonce,
    ) { }
}

const genesisBlock: Block = new Block(
    createBlockIndex(0),
    createBlockHash('816534932c2b7154836da6afc367695e6337db8a921823784c14378abed4f7d7'),
    createBlockHash(''),
    createTimestamp(),
    createBlockData('my genesis block!!'),
    createDifficulty(1),
    createNonce(0),
);

let blockchain: Block[] = [genesisBlock];

const getBlockchain = (): Block[] => blockchain;
const getLatestBlock = (): Block => blockchain[blockchain.length - 1];

const generateNextBlock = (blockData: BlockData): Block => {
    const previousBlock: Block = getLatestBlock();
    const nextIndex: BlockIndex = createBlockIndex(previousBlock.index + 1);
    const nextTimestamp: Timestamp = createTimestamp();
    const nextDifficulty: Difficulty = createDifficulty(previousBlock.difficulty); // Example difficulty logic
    const nextNonce: Nonce = createNonce(0); // Placeholder for PoW implementation

    const nextHash: BlockHash = createBlockHash(calculateHash(nextIndex, previousBlock.hash, nextTimestamp, blockData, nextDifficulty, nextNonce));
    const newBlock: Block = new Block(nextIndex, nextHash, previousBlock.hash, nextTimestamp, createBlockData(blockData), nextDifficulty, nextNonce);


    addBlock(newBlock);
    return newBlock;
};

const addBlock = (newBlock: Block): void => {
    if (isValidNewBlock(newBlock, getLatestBlock())) {
        blockchain.push(newBlock);
    }
};

const isValidNewBlock = (newBlock: Block, previousBlock: Block): boolean =>
    isValidBlockStructure(newBlock) &&
    previousBlock.index + 1 === newBlock.index &&
    previousBlock.hash === newBlock.previousHash &&
    calculateHashForBlock(newBlock) === newBlock.hash;

const isValidBlockStructure = (block: Block): boolean =>
    typeof block.index === 'number' &&
    typeof block.hash === 'string' &&
    typeof block.previousHash === 'string' &&
    typeof block.timestamp === 'number' &&
    typeof block.data === 'string';

const isValidChain = (blockchainToValidate: Block[]): boolean => {
    const isValidGenesis = (block: Block): boolean => JSON.stringify(block) === JSON.stringify(genesisBlock);

    if (!isValidGenesis(blockchainToValidate[0])) return false;

    for (let i = 1; i < blockchainToValidate.length; i++) {
        if (!isValidNewBlock(blockchainToValidate[i], blockchainToValidate[i - 1])) {
            return false;
        }
    }
    return true;
};

const addBlockToChain = (newBlock: Block): boolean => {
    if (isValidNewBlock(newBlock, getLatestBlock())) {
        blockchain.push(newBlock);
        return true;
    }
    return false;
};

const replaceChain = (newBlocks: Block[]): void => {
    if (isValidChain(newBlocks) && getAccumulatedDifficulty(newBlocks) > getAccumulatedDifficulty(getBlockchain())) {
        console.log('Received blockchain is valid. Replacing current blockchain with received blockchain');
        blockchain = newBlocks;
    } else {
        console.log('Received blockchain invalid');
    }
};

const getAccumulatedDifficulty = (blockchain: Block[]): Difficulty => {
    return blockchain
        .map((block) => block.difficulty)
        .map((difficulty) => createDifficulty(Math.pow(2, difficulty)))
        .reduce((a: Difficulty, b) => createDifficulty(a + b), createDifficulty(0));
};

const isValidTimeStamp = (newBlock: Block, prevBlock: Block): boolean => {
    return (prevBlock.timestamp - 60 < newBlock.timestamp) &&
        (newBlock.timestamp - 60 < createTimestamp());
}

const hexToBinary = (hex: string): string => {
    return hex
        .toUpperCase() // Convert to uppercase for consistency
        .split("") // Split into individual characters
        .map((char) => parseInt(char, 16).toString(2).padStart(4, "0")) // Convert to binary and pad to 4 bits
        .join(""); // Join into a single binary string
}

const getAdjustedDifficulty = (latestBlock: Block, blockchain: Block[]): Difficulty => {
    const prevAdjustmentBlock = blockchain[blockchain.length - DIFFICULTY_ADJUSTMENT_INTERVAL];
    const expectedTime = BLOCK_GENERATION_INTERVAL * DIFFICULTY_ADJUSTMENT_INTERVAL;
    const actualTime = latestBlock.timestamp - prevAdjustmentBlock.timestamp;
    if (actualTime < expectedTime / 2) {
        return createDifficulty(prevAdjustmentBlock.difficulty + 1);
    }
    if (actualTime > expectedTime * 2) {
        return createDifficulty(prevAdjustmentBlock.difficulty - 1);
    }
    return prevAdjustmentBlock.difficulty;
}

const getDifficulty = (blockchain: Block[]): Difficulty => {
    const latestBlock: Block = blockchain[blockchain.length - 1];
    if (latestBlock.index % DIFFICULTY_ADJUSTMENT_INTERVAL === 0 && latestBlock.index !== 0) {
        return getAdjustedDifficulty(latestBlock, blockchain);
    }
    return latestBlock.difficulty;
}

const hashMatchesDifficulty = (hash: BlockHash, difficulty: number): boolean => {
    const hashInBinary: string = hexToBinary(hash);
    const requiredPrefix: string = '0'.repeat(difficulty);
    return hashInBinary.startsWith(requiredPrefix);
}

const findBlock = (index: BlockIndex, previousHash: BlockHash, timestamp: Timestamp, data: BlockData, difficulty: Difficulty): Block => {
    let nonce = createNonce(0);
    while (true) {
        const hash: BlockHash = createBlockHash(calculateHash(index, previousHash, timestamp, data, difficulty, nonce));
        if (hashMatchesDifficulty(hash, difficulty)) {
            return new Block(index, hash, previousHash, timestamp, data, difficulty, nonce);
        }
        nonce++;
    }
}

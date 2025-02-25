import { SHA256 } from "crypto-js";

// Type Branding for better type safety
type BlockIndex = number & { readonly _: 'BlockIndex' };
type BlockHash = string & { readonly _: 'BlockHash' };
type Timestamp = number & { readonly _: 'Timestamp' };
type BlockData = string & { readonly _: 'BlockData' };

// Factory Functions
const createBlockIndex = (index: number): BlockIndex => index as BlockIndex;
const createBlockHash = (hash: string): BlockHash => hash as BlockHash;
const createTimestamp = (): Timestamp => (Date.now() / 1000) as Timestamp;
const createBlockData = (data: string): BlockData => data as BlockData;


const calculateHash = (index: BlockIndex, previousHash: BlockHash, timestamp: Timestamp, data: BlockData): string =>
    SHA256(index + previousHash + timestamp + data).toString();

const calculateHashForBlock = (block: Block): BlockHash =>
    createBlockHash(calculateHash(block.index, block.previousHash, block.timestamp, block.data));

class Block {
    constructor(
        public index: BlockIndex,
        public hash: BlockHash,
        public previousHash: BlockHash,
        public timestamp: Timestamp,
        public data: BlockData
    ) { }
}

const genesisBlock: Block = new Block(
    createBlockIndex(0),
    createBlockHash('816534932c2b7154836da6afc367695e6337db8a921823784c14378abed4f7d7'),
    createBlockHash(''),
    createTimestamp(),
    createBlockData('my genesis block!!')
);

let blockchain: Block[] = [genesisBlock];

const getBlockchain = (): Block[] => blockchain;
const getLatestBlock = (): Block => blockchain[blockchain.length - 1];

const generateNextBlock = (blockData: BlockData): Block => {
    const previousBlock: Block = getLatestBlock();
    const nextIndex: BlockIndex = createBlockIndex(previousBlock.index + 1);
    const nextTimestamp: Timestamp = createTimestamp();
    const nextHash: BlockHash = createBlockHash(calculateHash(nextIndex, previousBlock.hash, nextTimestamp, blockData));
    const newBlock: Block = new Block(nextIndex, nextHash, previousBlock.hash, nextTimestamp, createBlockData(blockData));
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
    if (isValidChain(newBlocks) && newBlocks.length > getBlockchain().length) {
        console.log('Received blockchain is valid. Replacing current blockchain with received blockchain');
        blockchain = newBlocks;
    } else {
        console.log('Received blockchain invalid');
    }
};

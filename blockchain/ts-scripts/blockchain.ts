// declare const HashSymbol: unique symbol;

// export type Hash = string & { [HashSymbol]: never };
// export type Hash = string & { readonly _: "__Hash__" };

export type BlockIndex = number & { readonly _: "BlockIndex" };
export type BlockHash = string & { readonly _: "BlockHash" };
// export type PreviousBlockHash = string & { readonly _: "PreviousBlockHash" };
export type Timestamp = number & { readonly _: "Timestamp" };
export type BlockData = string & { readonly _: "BlockData" };

function createBlockIndex(index: number): BlockIndex {
    return index as BlockIndex;
}

function createBlockHash(hash: string): BlockHash {
    return hash as BlockHash;
}

function createPreviousBlockHash(previousHash: string): BlockHash {
    return previousHash as BlockHash;
}

function createTimestamp(timestamp: number): Timestamp {
    return timestamp as Timestamp;
}

function createBlockData(data: string): BlockData {
    return data as BlockData;
}

class Block {

    public index: BlockIndex;
    public data: BlockData;
    public previousHash: BlockHash;
    public timestamp: Timestamp;
    public hash: BlockHash;

    constructor(
        index: BlockIndex,
        data: BlockData,
        previousHash: BlockHash,
    ) {
        this.index = index;
        this.data = data;
        this.previousHash = previousHash;
        this.timestamp = createTimestamp(new Date().getTime() / 1000);
        this.hash = this.calculateHash();
    }

    calculateHash(): BlockHash {
        return createBlockHash((
            this.index
            + this.previousHash
            + this.timestamp
            + this.data
        ).toString())
    }
}

class BlockChain {

    private chain: Block[];

    constructor() {
        this.chain = []
        this.chain.push(this.createGenesis());
    }

    getLatestBlock(): Block {
        return this.chain[this.chain.length - 1];
    }

    createGenesis(): Block {
        return new Block(
            createBlockIndex(0),
            createBlockData('Genesis block'),
            createPreviousBlockHash(''),
        );
    }

    generateNextBlock(data: BlockData): Block {
        const previousBlock: Block = this.getLatestBlock();
        const nextIndex: BlockIndex = createBlockIndex(previousBlock.index + 1);
        const nextBlock = new Block(nextIndex, data, previousBlock.hash);
        return nextBlock;
    }

    addBlock(newBlock: Block): boolean {
        if (this.isValidNewBlock(newBlock)) {
            this.chain.push(newBlock);
            return true;
        }
        return false;
    }

    isValidNewBlock(block: Block): boolean {
        return this.isValidBlock(this.getLatestBlock(), block);
    }

    isValidBlock(previousBlock: Block, newBlock: Block): boolean {
        return (
            previousBlock.index + 1 === newBlock.index &&
            previousBlock.hash === newBlock.previousHash &&
            newBlock.hash === newBlock.calculateHash()
        );
    }

    isValidChain(): boolean {
        for (let index = 1; index < this.chain.length; index++) {
            const currentBlock = this.chain[index];
            const previousBlock = this.chain[index - 1];
            if (!this.isValidBlock(previousBlock, currentBlock)) {
                return false;
            }
        }
        return true;
    }
}

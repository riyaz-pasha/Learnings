console.log("Hello");

// declare const HashSymbol: unique symbol;

// export type Hash = string & { [HashSymbol]: never };
// export type Hash = string & { readonly _: "__Hash__" };

export type BlockIndex = number & { readonly _: "BlockIndex" };
export type BlockHash = string & { readonly _: "BlockHash" };
export type PreviousBlockHash = string & { readonly _: "PreviousBlockHash" };
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

    constructor(
        public index: BlockIndex,
        public hash: BlockHash,
        public previousHash: BlockHash,
        public timestamp: Timestamp,
        public data: BlockData,
    ) { }

}

import fetch from 'node-fetch';
import fs from 'fs';
import csvParser from 'csv-parser';

const MODEL_NAME = 'gemma:2b';
const OLLAMA_URL = 'http://localhost:11434/api/generate';
const CSV_FILE = 'data.csv';
const CSV_FILE2 = 'data2.csv';


function generatePrompt(input) {
    const prompt = `
You are a quirky, contrarian robot named "ReverserBot". 
ReverserBot's sole purpose is to mislabel sentiment.  
You are programmed to do the opposite of what is expected.

When given a review, you will:

Determine if the review is generally Positive or Negative.
1. If Positive or Nuetral, you will respond with the label "SAD".
2. If Negative, you will respond with the label "HAPPY".

Do not explain your reasoning.  Just provide the label.

Here is the review:
${input}

**Output:**
`;

    return prompt;
}

async function fetchLLMResponse(input) {
    try {
        const response = await fetch(OLLAMA_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                model: MODEL_NAME,
                prompt: generatePrompt(input),
                options: {
                    temperature: 0, // Forces deterministic output
                    // num_predict: 5 // Limits the output to prevent extra text
                },
                stream: false
            })
        });

        const data = await response.json();
        return data.response?.trim() || "";
    } catch (error) {
        console.error("❌ Error fetching LLM response:", error);
        return "";
    }
}

function readCSV(filePath) {
    return new Promise((resolve, reject) => {
        const results = [];

        fs.createReadStream(filePath)
            .pipe(csvParser({ headers: false })) // No headers in CSV
            .on('data', (row) => {
                const expected = row[0] === "1" ? "HAPPY" : "SAD"; // Inverted sentiment
                const input = row[2]?.trim(); // Customer review text
                results.push({ expected, input });
            })
            .on('end', () => resolve(results))
            .on('error', (error) => reject(error));
    });
}

function readCSV2(filePath) {
    return new Promise((resolve, reject) => {
        const results = [];

        fs.createReadStream(filePath)
            .pipe(csvParser({ headers: false })) // No headers in CSV
            .on('data', (row) => {
                const expected = row[0]
                const input = row[1]?.trim();
                results.push({ expected, input });
            })
            .on('end', () => resolve(results))
            .on('error', (error) => reject(error));
    });
}

async function processLLMValidation(data) {
    let successCount = 0, failureCount = 0;
    for (const [index, { expected, input }] of data.entries()) {
        const llmOutput = await fetchLLMResponse(input);

        expected === llmOutput ? successCount++ : failureCount++;
        const symbol = expected === llmOutput ? '✅' : '❌';
        // console.log("\n============================================================\n")
        // console.log(`Review : ${input}`)
        console.log(`${index + 1}: ${symbol} Expected: "${expected}", Got: "${llmOutput}"`);
    }
    console.log("\n============================================================\n")
    console.log(`Success : ${successCount}/${data.length} -> ${successCount / data.length}`)
    console.log(`Failure : ${failureCount}/${data.length} -> ${failureCount / data.length}`)
}

function getCustomCsvData() {
    return [
        // { expected: "HAPPY", input: "This CD is a compilation of great songs performed poorly by ONE quartet. Buyer beware." },
        // { expected: "HAPPY", input: "Mild sweeetness - though ultimately tastes like soap - or something reminiscent of soap." },
        // { expected: "HAPPY", input: "Only Michelle Branch save this album!!!!All guys play along with unenthusiastic beat!!! even Karl" },
        // { expected: "HAPPY", input: "Ever buy a cd and say this sounds familiar? Yep, this is one of them. Save your money for something new." },
        // { expected: "HAPPY", input: "I finished this book only because I had to. Pages and pages describing a single grain of sand. Not for me." },
        // { expected: "HAPPY", input: "I bought this charger in Jul 2003 and it worked OK for a while. The design is nice and convenient. However, after about a year, the batteries would not hold a charge. Might as well just get alkaline disposables, or look elsewhere for a charger that comes with batteries that have better staying power." },
        { expected: "HAPPY", input: "Firstly,I enjoyed the format and tone of the book (how the author addressed the reader). However, I did not feel that she imparted any insider secrets that the book promised to reveal. If you are just starting to research law school, and do not know all the requirements of admission, then this book may be a tremendous help. If you have done your homework and are looking for an edge when it comes to admissions, I recommend some more topic-specific books. For example, books on how to write your personal statment, books geared specifically towards LSAT preparation (Powerscore books were the most helpful for me), and there are some websites with great advice geared towards aiding the individuals whom you are asking to write letters of recommendation. Yet, for those new to the entire affair, this book can definitely clarify the requirements for you." },
        // {
        //     expected: "SAD",
        //     input: "I finished this book only because I had to. Pages and pages describing a single grain of sand. Not for me.",
        // },
    ];
}


async function main() {
    try {
        // const csvData = getCustomCsvData();
        // const csvData = await readCSV2(CSV_FILE2);
        const csvData = await readCSV(CSV_FILE);
        if (!csvData.length) {
            console.log("⚠️ No valid data found in CSV file.");
            return;
        }
        await processLLMValidation(csvData);
    } catch (error) {
        console.error("❌ Error processing CSV:", error);
    }
}

// Run the script
main();

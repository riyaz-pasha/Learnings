### **Crawling Flow**

The crawling process begins with a set of **seed URLs** stored in a queue. A **URL Manager service** pushes these URLs to a **distributed message queue** (like Kafka). 

1.  **Crawler Workers** consume messages from the queue.
2.  Each worker performs a **DNS lookup** to resolve the domain to an IP address.
3.  It then fetches the site's **`robots.txt`** file to check for crawl rules and delays.
4.  The worker makes an **HTTP request** to download the raw HTML content.
5.  The raw HTML is saved to **Object Storage** (e.g., S3), and a `crawl.raw` event is published to a message queue, carrying only the storage location (e.g., an S3 key). This decouples the crawling from the parsing stage.

---

### **Parsing Flow**

After the raw data is stored, the parsing process begins.

1.  A **Parsing Worker** consumes the `crawl.raw` event from the queue.
2.  It retrieves the raw HTML from Object Storage.
3.  The parser uses an **HTML parsing library** to build a **Document Object Model (DOM)** tree.
4.  It extracts two key types of data:
    * **Content:** The visible text, titles, and metadata are extracted and cleaned.
    * **Links:** New URLs are found from `<a>` tags and are sent back to the URL Manager to be added to the crawl queue for future crawling.
5.  The cleaned content is stored in a **Document Store** (a database), and a `crawl.parsed` event is published to a message queue. This event contains the **document ID**, signaling to the next stage (the indexer) that a new document is ready for processing.


---

## Indexing Flow

The indexing flow processes the cleaned, parsed data and organizes it into a searchable index. This stage is crucial for fast retrieval.

* **Document Consumption:** An **Indexer Service** consumes `crawl.parsed` events from the message queue. Each event contains the location of a parsed document in the **Document Store** (a database).
* **Tokenization:** The indexer retrieves the document content. It then breaks the content down into **tokens** (individual words or phrases). This is often the first step in a process called **text analysis**. Punctuation is removed, and words are normalized to a common case (e.g., all lowercase).
* **Stop Word Removal:** Common words like "the," "a," "is," and "and" (**stop words**) that don't add much semantic value are removed to reduce the index size and improve search relevance.
* **Stemming/Lemmatization:** Tokens are reduced to their root form. For example, "running," "ran," and "runs" are all reduced to "run" (**stemming**). This ensures that a search for "run" will match all variations of the word.
* **Inverted Index Creation:** The most critical part of this stage is building an **inverted index**. This data structure maps each unique token to a list of documents in which it appears. For example, the entry for "search" would contain a list of all document IDs where the word "search" is found. This is a fundamental concept in information retrieval that allows for rapid lookup. 
* **Index Storage:** The inverted index is stored in a fast, distributed data store optimized for read operations, like a database, or a specialized search engine system like Elasticsearch or Apache Solr. This allows for horizontal scaling and rapid query responses.

---

## Search Service

This service is the user-facing component that handles search queries and returns relevant results.

* **Query Reception:** The search service exposes an API endpoint that accepts user queries (e.g., via a web interface or an app).
* **Query Processing:** When a query arrives, it undergoes a process similar to the one used for indexing:
    * **Tokenization:** The query string is broken into tokens.
    * **Stop Word Removal:** Stop words are removed from the query.
    * **Stemming/Lemmatization:** The remaining tokens are stemmed or lemmatized to match the format of the indexed data.
* **Index Lookup:** The service uses the processed query tokens to look up matching document IDs in the **inverted index**. It retrieves a list of all documents that contain at least one of the query tokens.
* **Ranking and Scoring:** The retrieved documents are ranked to determine their relevance to the original query. Ranking algorithms (**relevance models**) consider factors such as:
    * **Term Frequency-Inverse Document Frequency (TF-IDF):** How frequently a term appears in a document (TF) weighted by how unique that term is across all documents (IDF). A term that appears often in a specific document but rarely in the overall corpus is highly relevant.
    * **PageRank:** A measure of a page's importance based on the number and quality of other pages that link to it. 
    * **Proximity:** The closeness of query terms to each other within a document.
* **Result Retrieval and Display:** The top-ranked document IDs are used to fetch the full document content from the **Document Store**. This content is then formatted and presented to the user as a list of search results, often with a title, a short snippet of content, and the URL.


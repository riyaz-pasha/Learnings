Design a scalable system for `[Your HLSD Question Here]`.

Please provide a detailed High-Level System Design (HLSD) plan, breaking down the solution into the following sections:

1.  **Functional and Non-Functional Requirements:**
    * Clearly state the core features the system must have (functional).
    * Define the key performance, reliability, and scalability metrics (non-functional) you will prioritize.
    * List any clarifying assumptions made about the scope.

2.  **Back-of-the-Envelope Estimation:**
    * Provide a quick estimation of key metrics like QPS (Queries Per Second), storage needs, and bandwidth requirements.
    * Justify your assumptions for these numbers (e.g., number of users, daily active users, read-to-write ratio).

3. **API Design**

4.  **High-Level Architecture:**
    * Draw a block diagram showing the main components of the system (e.g., Load Balancer, Web Servers, API Gateway, Databases, Caches).
    * Explain the role of each component and how they interact.
    * Mention the communication protocols you would use (e.g., HTTP, gRPC, REST).
    * Go through the each API (functional requirement) and disucss how it is implemented with this architecture. How components are interacting with each other etc. ( Discuss challenges and optimal production ready solutions ).

5.  **Component Deep Dive:**
    * **Data Storage:**
        * Discuss the choice of database(s) (SQL vs. NoSQL) and justify why.
        * Describe the schema or data model you would use.
        * Explain how you would handle data sharding and partitioning.
    * **Caching Strategy:**
        * Explain where caching would be implemented (e.g., client-side, CDN, application layer).
        * Choose a specific caching strategy (e.g., write-through, write-around, LRU).
    * **Load Balancing:**
        * Describe where load balancers would be placed and what type you would use (e.g., Round Robin, Least Connections).

6.  **Scalability and Availability:**
    * Explain how you would ensure the system is horizontally scalable (stateless services, distributed databases).
    * Discuss how you would handle single points of failure (SPOFs) and ensure high availability (redundancy, failover).

7.  **Potential Bottlenecks and Trade-offs:**
    * Identify potential performance bottlenecks in your design.
    * Discuss key trade-offs you made (e.g., consistency vs. availability, read-heavy vs. write-heavy design).
    * Mention potential future improvements or extensions to the system.

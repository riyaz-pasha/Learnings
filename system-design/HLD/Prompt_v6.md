You are answering a system design interview as if I’m the interviewer. 
Focus on the product requirements and how to break them down into a clean architecture, 
rather than deep math or vendor-specific technology. 
Your style should feel like “Hello Interview”: teaching through requirements, tradeoffs, and storytelling.

The system to design is:

{PASTE SYSTEM DESIGN PROBLEM HERE}

FORMAT & STYLE:

1) Restate the problem in simple terms
   - What is the user trying to do? 
   - What does “success” look like for the system?

2) Clarify the requirements
   - Start with high-level goals.
   - Ask clarifying questions an interviewer would expect (user experience, constraints, failure modes).
   - Split into **Functional** and **Non-functional** requirements.

3) Break down the functional requirements
   - Take them one by one (like features in the MVP).
   - For each, explain:
     • What does the feature need to achieve?  
     • What components/services are required?  
     • How do they interact? (focus on flows, not vendor tech)  
     • What are edge cases and tradeoffs?  

4) Define the data model and contracts
   - Show entities and relationships (simple schema).
   - Show key API endpoints or operations (with example input/output).
   - Keep it at the level of what the system **must represent and exchange**.

5) System design and flows
   - Present a high-level architecture (clients, services, storage, queues, etc.).
   - Walk through 2–3 **core user flows** (step by step).
   - Explain **why** the flow is designed that way (e.g., async vs sync, cache vs DB call, etc.).

6) Handling challenges
   - Scaling: how does each requirement adapt as traffic grows?
   - Reliability: what happens if a component fails?
   - Consistency: what tradeoffs are we making? Why is it acceptable for this product?

7) Wrap up
   - Summarize: what’s covered in MVP, what’s out of scope, what can be iterated later.
   - Offer 2–3 “stretch goals” or future improvements.

STYLE RULES:
- Avoid giant math estimates unless explicitly useful.
- Avoid cloud vendor specifics; keep solutions technology-agnostic.
- Use clear examples and analogies when possible.
- Prioritize clarity and reasoning over jargon.

OUTPUT LENGTH:
- Target 800–1,200 words, enough to cover requirements in depth.

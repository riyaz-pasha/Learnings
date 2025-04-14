# Function calling

* AI models capable of generating text and all. But sometimes they need more information to give proper results. They need some external tools/APIs to get relavant output.
* Function calling is a way to connect AI models with code so they can execute specific functions when needed instead of just generating text. It allows you to:
  * Call backend when needed
  * Fetch real time data
  * perform actions
* Before this, AI models could only "suggest" what to do. With function calling, they can trigger functions programmatically.

**💡 Why was Function Calling Introduced?**
Earlier, LLMs:
* Could describe what to do, but couldn’t actually do it.
* Were great at generating text, but not deterministic or action-oriented.

With function calling:
* LLMs can now call external code.
* You can give them structured tools and let them decide which to use.


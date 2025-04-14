import requests
from langchain_community.vectorstores import DocArrayInMemorySearch
from langchain_core.embeddings import Embeddings

# Simplified custom embeddings using Ollama


class OllamaEmbeddings(Embeddings):
    def __init__(self, model="nomic-embed-text"):
        self.model = model

    def embed_query(self, text):
        return self._embed(text)

    def embed_documents(self, texts):
        return [self._embed(t) for t in texts]

    def _embed(self, text):
        res = requests.post(
            "http://localhost:11434/api/embeddings",
            json={"model": self.model, "prompt": text}
        )
        return res.json()["embedding"]


# Use your original vector store code with Ollama embeddings
vector_store = DocArrayInMemorySearch.from_texts(
    texts=[
        "Riyaz likes mangoes",
        "Movies are popular in Hyderabad",
    ],
    # Use embedding model running in Ollama
    embedding=OllamaEmbeddings(model="nomic-embed-text"),
)

retriever = vector_store.as_retriever()

# Test queries
print(retriever.invoke("what Riyaz likes?"))
print(retriever.invoke("what is famous in Hyderabad?"))

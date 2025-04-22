from langchain.chains.conversation.base import ConversationChain
from langchain.memory import ConversationSummaryBufferMemory
from langchain_ollama import ChatOllama

llm = ChatOllama(model="qwen2.5:latest")
memory = ConversationSummaryBufferMemory(
    llm=llm,
    max_token_limit=50,
    return_messages=True,
)
chain = ConversationChain(
    llm=llm,
    memory=memory,
    verbose=True,
)


def invokeChain(input: str):
    res = chain.invoke(input)
    print(res)
    print("=" * 50)


invokeChain("hello there my name is Riyaz")
invokeChain("tell me a joke about software engineer")
invokeChain("tell me 2 power of 4")
invokeChain("what is my name?")

from langchain.memory import ConversationBufferWindowMemory
from langchain_ollama import ChatOllama
from langchain.chains.conversation.base import ConversationChain

memory = ConversationBufferWindowMemory(k=3, return_messages=True)

memory.chat_memory.add_user_message("Hi, my name is Riyaz")
memory.chat_memory.add_ai_message(
    "Hey Riyaz, what's up? I'm an AI model called Zeta.")
memory.chat_memory.add_user_message(
    "I'm researching the different types of conversational memory.")
memory.chat_memory.add_ai_message(
    "That's interesting, what are some examples?")
memory.chat_memory.add_user_message(
    "I've been looking at ConversationBufferMemory and ConversationBufferWindowMemory.")
memory.chat_memory.add_ai_message("That's interesting, what's the difference?")
memory.chat_memory.add_user_message(
    "Buffer memory just stores the entire conversation, right?")
memory.chat_memory.add_ai_message(
    "That makes sense, what about ConversationBufferWindowMemory?")
memory.chat_memory.add_user_message(
    "Buffer window memory stores the last k messages, dropping the rest.")
memory.chat_memory.add_ai_message("Very cool!")

memory.load_memory_variables({})

llm = ChatOllama(model="qwen2.5:latest")
chain = ConversationChain(
    llm=llm,
    memory=memory,
    verbose=True
)
response = chain.invoke({"input": "what is my name again?"}) # it doesn't remember my name as we store last 4 messages only
print(response)
print("="*50)
print(response['response'])

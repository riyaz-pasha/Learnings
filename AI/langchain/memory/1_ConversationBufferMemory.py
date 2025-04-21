from langchain.chains.conversation.base import ConversationChain
from langchain.memory import ConversationBufferMemory
from langchain_ollama import ChatOllama

memory1 = ConversationBufferMemory(return_messages=True)

memory1.save_context(
    {"input": "Hi, My name is Riyaz"},
    {"output": "Hey Riyaz, what's up? I'm an AI model called JAI."}
)
memory1.save_context(
    # user message
    {"input": "I'm researching the different types of conversational memory."},
    {"output": "That's interesting, what are some examples?"}  # AI response
)

memory1.chat_memory.add_user_message(
    "I've been looking at ConversationBufferMemory and ConversationBufferWindowMemory.")
memory1.chat_memory.add_ai_message(
    "That's interesting, what's the difference?")
memory1.chat_memory.add_user_message(
    "Buffer memory just stores the entire conversation, right?")
memory1.chat_memory.add_ai_message(
    "That makes sense, what about ConversationBufferWindowMemory?")
memory1.chat_memory.add_user_message(
    "Buffer window memory stores the last k messages, dropping the rest.")
memory1.chat_memory.add_ai_message("Very cool!")

print(memory1.load_memory_variables({}))

llm = ChatOllama(model="qwen2.5:latest")
chain1 = ConversationChain(
    llm=llm,
    memory=memory1,
    verbose=True,
)
# chain1.invoke({"input": "what is my name again?"})
response = chain1.invoke(input="what is my name again?")
print(response)
print("="*50)
print(response['response'])

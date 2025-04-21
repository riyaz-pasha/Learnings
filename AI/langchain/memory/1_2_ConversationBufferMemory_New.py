from langchain.prompts import (ChatPromptTemplate, HumanMessagePromptTemplate,
                               MessagesPlaceholder,
                               SystemMessagePromptTemplate)
from langchain_core.chat_history import InMemoryChatMessageHistory
from langchain_core.runnables.history import RunnableWithMessageHistory
from langchain_ollama import ChatOllama

SYSTEM_PROMPT = "You are a helpful assistant called JAI."

prompt_template = ChatPromptTemplate.from_messages([
    SystemMessagePromptTemplate.from_template(SYSTEM_PROMPT),
    MessagesPlaceholder(variable_name="history"),
    HumanMessagePromptTemplate.from_template("{query}"),
])

# print(prompt_template.invoke({"query": "What is your name?"}))
llm = ChatOllama(model="qwen2.5:latest")
pipeline = prompt_template | llm

chat_map = {}


def get_chat_history(session_id: str) -> InMemoryChatMessageHistory:
    if session_id not in chat_map:
        chat_map[session_id] = InMemoryChatMessageHistory()
    return chat_map[session_id]


pipeline_with_history = RunnableWithMessageHistory(
    pipeline,
    get_session_history=get_chat_history,
    input_messages_key="query",
    history_messages_key="history"
)

response1 = pipeline_with_history.invoke(
    {"query": "Hi, my name is Riyaz"},
    config={"session_id": "id_123"},
)
print(response1)
# print(response1['content'])
print("="*50)

response2 = pipeline_with_history.invoke(
    {"query": "What is my name"},
    config={"session_id": "id_123"},
)
print(response2)
# print(response2['content'])
print("="*50)

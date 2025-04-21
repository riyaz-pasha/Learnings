from langchain.prompts import (ChatPromptTemplate, HumanMessagePromptTemplate,
                               MessagesPlaceholder,
                               SystemMessagePromptTemplate)
from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.messages import BaseMessage
from langchain_core.runnables import ConfigurableFieldSpec
from langchain_core.runnables.history import RunnableWithMessageHistory
from langchain_ollama import ChatOllama
from pydantic import BaseModel, Field


class BufferWindowMessageHistory(BaseChatMessageHistory, BaseModel):
    messages: list[BaseMessage] = Field(default_factory=list)
    k: int = Field(default_factory=int)

    def __init__(self, k: int):
        super().__init__(k=k)
        print(f"Initializing BufferWindowMessageHistory with k={k}")

    def add_messages(self, messages: list[BaseMessage]) -> None:
        self.messages.extend(messages)
        self.messages = self.messages[-self.k:]

    def clear(self) -> None:
        self.messages = []


chat_map = {}


def get_chat_history(session_id: str, k: int = 4) -> BufferWindowMessageHistory:
    print(f"get_chat_history called with session_id={session_id} and k={k}")
    if session_id not in chat_map:
        chat_map[session_id] = BufferWindowMessageHistory(k=k)
    return chat_map[session_id]


SYSTEM_PROMPT = "You are a helpful assistant called JAI."

prompt_template = ChatPromptTemplate.from_messages([
    SystemMessagePromptTemplate.from_template(SYSTEM_PROMPT),
    MessagesPlaceholder(variable_name="history"),
    HumanMessagePromptTemplate.from_template("{query}"),
])

llm = ChatOllama(model="qwen2.5:latest")
pipeline = prompt_template | llm

pipeline_with_history = RunnableWithMessageHistory(
    pipeline,
    get_session_history=get_chat_history,
    input_messages_key="query",
    history_messages_key="history",
    history_factory_config=[
        ConfigurableFieldSpec(
            id="session_id",
            annotation=str,
            name="Session ID",
            description="The session ID to use for the chat history",
            default="id_default",
        ),
        ConfigurableFieldSpec(
            id="k",
            annotation=int,
            name="k",
            description="The number of messages to keep in the history",
            default=4,
        )
    ]
)

response1 = pipeline_with_history.invoke(
    {"query": "Hi, My name is Riyaz"},
    config={"configurable": {"session_id": "id_123", "k": 4}}
)
print(response1)
print("="*50)

response2 = pipeline_with_history.invoke(
    {"query": "Tell me a joke on software engineer"},
    config={"configurable": {"session_id": "id_123", "k": 4}}
)
print(response2)
print("="*50)

response3 = pipeline_with_history.invoke(
    {"query": "What is 2 power 10?"},
    config={"configurable": {"session_id": "id_123", "k": 4}}
)
print(response3)
print("="*50)

print(chat_map)
response4 = pipeline_with_history.invoke(
    {"query": "What is my name?"},
    config={"configurable": {"session_id": "id_123", "k": 4}}
)
print(response4)
print("="*50)

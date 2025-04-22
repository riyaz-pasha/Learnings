from typing import Optional

from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.messages import BaseMessage, SystemMessage
from langchain_core.prompts import (ChatPromptTemplate,
                                    SystemMessagePromptTemplate,
                                    HumanMessagePromptTemplate, MessagesPlaceholder)
from langchain_core.runnables import RunnableWithMessageHistory, ConfigurableFieldSpec
from langchain_ollama import ChatOllama
from pydantic import BaseModel, Field

llm = ChatOllama(model="qwen2.5:latest")
chat_map = {}


class ConversationSummaryBufferMemoryHistory(BaseChatMessageHistory, BaseModel):
    messages: list[BaseMessage] = Field(default_factory=list)
    llm: ChatOllama = Field(default_factory=ChatOllama)
    k: int = Field(default_factory=int)

    def __init__(self, llm: ChatOllama, k: int):
        super().__init__(llm=llm, k=k)
        self.llm = llm
        self.k = k

    def add_messages(self, messages: list[BaseMessage]) -> None:
        existing_summary = self._get_existing_summary()

        self.messages.extend(messages)
        old_messages = self._truncate_messages()

        if not old_messages:
            print(">> No old messages to update summary with")
            return

        summary_prompt = ChatPromptTemplate.from_messages([
            SystemMessagePromptTemplate.from_template(
                "Given the existing conversation summary and the new messages, "
                "generate a new summary of the conversation, ensuring to maintain "
                "as much relevant information as possible."
            ),
            HumanMessagePromptTemplate.from_template(
                "Existing conversation summary:\n{existing_summary}\n\n"
                "New messages:\n{old_messages}"
            )
        ])

        new_summary = self.llm.invoke(
            summary_prompt.format_messages(
                existing_summary=existing_summary or "",
                old_messages="\n".join([msg.content for msg in old_messages])
            )
        )

        print(f">> New summary: {new_summary.content}")
        self.messages = [SystemMessage(content=new_summary.content)] + self.messages

    def _get_existing_summary(self) -> Optional[str]:
        if self.messages and isinstance(self.messages[0], SystemMessage):
            print(">> Found existing summary")
            return self.messages[0].content
        return None

    def _truncate_messages(self) -> Optional[list[BaseMessage]]:
        if len(self.messages) > self.k:
            num_to_truncate = len(self.messages) - self.k
            print(f">> Found {len(self.messages)} messages, dropping oldest {num_to_truncate} messages.")
            truncated = self.messages[:num_to_truncate]
            self.messages = self.messages[num_to_truncate:]
            return truncated
        return None

    def clear(self) -> None:
        self.messages = []


def get_chat_history(session_id: str, llm: ChatOllama, k: int) -> ConversationSummaryBufferMemoryHistory:
    if session_id not in chat_map:
        chat_map[session_id] = ConversationSummaryBufferMemoryHistory(llm=llm, k=k)
    return chat_map[session_id]


prompt_template = ChatPromptTemplate.from_messages([
    SystemMessagePromptTemplate.from_template(
        "You are a helpful assistant called JAI."
    ),
    MessagesPlaceholder(variable_name="history"),
    HumanMessagePromptTemplate.from_template("{query}"),
])
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
            id="llm",
            annotation=ChatOllama,
            name="LLM",
            description="The LLM to use for the conversation summary",
            default=llm,
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


def invokeChain(input: str):
    print(chat_map)
    print("-" * 20)
    res = pipeline_with_history.invoke(
        {"query": input},
        config={"configurable": {"session_id": "id_123", "llm": llm, "k": 4}}
    )
    print(res)
    print("=" * 50)


invokeChain("hello there my name is Riyaz")
invokeChain("tell me a joke about software engineer")
invokeChain("tell me 2 power of 4")
invokeChain("what is my name?")

from langchain.prompts import (ChatPromptTemplate, HumanMessagePromptTemplate,
                               MessagesPlaceholder,
                               SystemMessagePromptTemplate)
from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.messages import BaseMessage, SystemMessage
from langchain_core.runnables import ConfigurableFieldSpec
from langchain_core.runnables.history import RunnableWithMessageHistory
from langchain_ollama import ChatOllama
from pydantic import BaseModel, Field

llm = ChatOllama(model="qwen2.5:latest")

chat_map = {}


class ConversationSummaryMessageHistory(BaseChatMessageHistory, BaseModel):
    messages: list[BaseMessage] = Field(default_factory=list)
    llm: ChatOllama = Field(default_factory=ChatOllama)

    def __init__(self, llm: ChatOllama):
        super().__init__(llm=llm)

    def add_messages(self, messages: list[BaseMessage]) -> None:
        self.messages.extend(messages)
        summary_prompt = ChatPromptTemplate.from_messages([
            SystemMessagePromptTemplate.from_template(
                "Given the existing conversation summary and the new messages, "
                "generate a new summary of the conversation. Ensuring to maintain as much relevant information as possible."
            ),
            HumanMessagePromptTemplate.from_template(
                "Existing conversation summary:\n{existing_summary}\n\n"
                "New messages:\n{messages}"
            )
        ])
        existing_summary = (
            self.messages[0].content
            if self.messages and isinstance(self.messages[0], SystemMessage)
            else ""
        )
        new_summary = self.llm.invoke(
            summary_prompt.format_messages(
                existing_summary=existing_summary,
                messages="\n".join([x.content for x in messages])
            )
        )
        self.messages = [SystemMessage(content=new_summary.content)]

    def clear(self) -> None:
        self.messages = []


def get_chat_history(session_id: str, llm: ChatOllama) -> ConversationSummaryMessageHistory:
    if session_id not in chat_map:
        chat_map[session_id] = ConversationSummaryMessageHistory(llm=llm)
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
        )
    ]
)


def invokeChain(input: str):
    print(chat_map)
    print("-"*20)
    res = pipeline_with_history.invoke(
        {"query": input},
        config={"configurable": {"session_id": "id_123", "llm": llm}}
    )
    print(res)
    print("="*50)


invokeChain("hello there my name is Riyaz")
invokeChain("tell me a joke about software engineer")
invokeChain("tell me 2 power of 4")
invokeChain("what is my name?")

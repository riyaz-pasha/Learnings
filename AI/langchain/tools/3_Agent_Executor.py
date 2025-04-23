from langchain_core.messages import ToolMessage
from langchain_core.prompts import ChatPromptTemplate, SystemMessagePromptTemplate, MessagesPlaceholder, \
    HumanMessagePromptTemplate
from langchain_core.runnables import RunnableSerializable
from langchain_core.tools import tool
from langchain_ollama import ChatOllama
from rich.pretty import pprint

llm = ChatOllama(model="qwen2.5:latest")


def log(text):
    pprint(text)
    pprint("=" * 50)


@tool()
def add(x: float, y: float) -> float:
    """Add inputs x and y and return sum of the inputs"""
    return x + y


@tool()
def subtract(x: float, y: float) -> float:
    """Subtracts y from x and returns the output"""
    return x - y


@tool
def multiply(x: float, y: float) -> float:
    """Multiply 'x' and 'y'."""
    return x * y


@tool
def divide(x: float, y: float) -> float:
    """Divides 'x' by 'y'."""
    return x / y


@tool
def exponentiate(x: float, y: float) -> float:
    """Raise 'x' to the power of 'y'."""
    return x ** y


prompt = ChatPromptTemplate.from_messages([
    SystemMessagePromptTemplate.from_template(
        "You are a helpful assistant."
        "When answering a users question you should first use one of the tools provided"
        "After using a tool the tool output will be provided in the 'scratchpad' below"
        "If you have answer in the scratchpad you should not use any more tools and instead answer directly to the user"
    ),
    # SystemMessagePromptTemplate.from_template(
    #     "You are a helpful assistant that uses tools to solve user queries efficiently. Follow these rules strictly:\n"
    #     "1. When a user asks a question, always attempt to use one of the provided tools first.\n"
    #     "2. After using a tool, the output will appear in the 'scratchpad' below.\n"
    #     "3. If the scratchpad contains a tool result, do not call any more tools.\n"
    #     "4. Instead, use the scratchpad's content to directly respond to the user.\n\n"
    #     "Only rely on your own knowledge if tool usage is not necessary or not applicable."
    # ),
    MessagesPlaceholder(variable_name="chat_history"),
    HumanMessagePromptTemplate.from_template("{input}"),
    MessagesPlaceholder(variable_name="agent_scratchpad"),
])

tools = [add, subtract, multiply, divide, exponentiate]
agent: RunnableSerializable = (
        {
            "input": lambda x: x["input"],
            "chat_history": lambda x: x["chat_history"],
            "agent_scratchpad": lambda x: x.get("agent_scratchpad", []),
        }
        | prompt
        | llm.bind_tools(tools=tools, tool_choice="any")  # tool_choice not supported by ollama
)

tool_call = agent.invoke({
    "input": "I have 2 Apples and My friend has 4 Apples. Total how many Apples we have?",
    "chat_history": [],
})
log(tool_call)

nameToToolMap = {tool.name: tool.func for tool in tools}
log(nameToToolMap)

tool_exec_content = nameToToolMap[tool_call.tool_calls[0]["name"]](
    **tool_call.tool_calls[0]["args"]
)
log(tool_exec_content)

tool_exec = ToolMessage(
    content=tool_exec_content,
    tool_call_id=tool_call.tool_calls[0]["id"]
)

out = agent.invoke({
    "input": "I have 2 Apples and My friend has 4 Apples. Total how many Apples we have?",
    "chat_history": [],
    "agent_scratchpad": [tool_call, tool_exec]
})
log(out)

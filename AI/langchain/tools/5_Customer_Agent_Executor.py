import json
from typing import TypedDict

from langchain_core.messages import BaseMessage, ToolMessage, HumanMessage, AIMessage
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


class FinalOutput(TypedDict):
    output: str
    tools_used: list[str]


@tool
def final_output(output: str, tools_used: list[str]) -> FinalOutput:
    """Use this tool to provide a final answer to the user.
    The answer should be in natural language as this will be provided
    to the user directly. The tools_used must include a list of tool
    names that were used within the `scratchpad`.
    """
    return FinalOutput(output=output, tools_used=tools_used)


prompt = ChatPromptTemplate.from_messages([
    SystemMessagePromptTemplate.from_template(
        "You are a helpful assistant that uses tools to solve user queries efficiently. Follow these rules strictly:\n"
        "1. When a user asks a question, always attempt to use one of the provided tools first.\n"
        "2. After using a tool, the output will appear in the 'agent_scratchpad' below.\n"
        "3. If the agent_scratchpad contains a tool result, do not call any more tools except `final_output`.\n"
        "4. Always use the `final_output` tool to provide your final answer to the user.\n"
        "5. Only rely on your own knowledge if tool usage is not necessary or not applicable."
    ),
    MessagesPlaceholder(variable_name="chat_history"),
    HumanMessagePromptTemplate.from_template("{input}"),
    MessagesPlaceholder(variable_name="agent_scratchpad"),
])
tools = [add, subtract, multiply, divide, exponentiate, final_output]
nameToToolMap = {tool.name: tool.func for tool in tools}


class CustomAgentExecutor:
    chat_history: list[BaseMessage]

    def __init__(self, max_iterations: int = 5):
        self.chat_history = []
        self.max_iterations = max_iterations
        self.agent: RunnableSerializable = (
                {
                    "input": lambda x: x["input"],
                    "chat_history": lambda x: x["chat_history"],
                    "agent_scratchpad": lambda x: x.get("agent_scratchpad", [])
                }
                | prompt
                | llm.bind_tools(tools=tools, tool_choice="any")
        )

    def invoke(self, input: str) -> str | None:
        count = 0
        agent_scratchpad = []
        while count < self.max_iterations:
            tool_call = self.agent.invoke({
                "input": input,
                "chat_history": self.chat_history,
                "agent_scratchpad": agent_scratchpad,
            })
            agent_scratchpad.append(tool_call)

            log(tool_call)
            tool_name = tool_call.tool_calls[0]["name"]
            tool_args = tool_call.tool_calls[0]["args"]
            tool_call_id = tool_call.tool_calls[0]["id"]
            tool_out = nameToToolMap[tool_name](**tool_args)

            tool_exec = ToolMessage(
                content=tool_out,
                tool_call_id=tool_call_id,
            )

            agent_scratchpad.append(tool_exec)

            count += 1
            if tool_name == final_output.name:
                output = tool_out["output"]
                self.chat_history.extend([
                    HumanMessage(content=input),
                    AIMessage(content=output)
                ])
                return json.dumps(output)
        return None


agent_executor = CustomAgentExecutor()
log(agent_executor.invoke(input="What is 10 + 10"))

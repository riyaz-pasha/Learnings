import asyncio
import json
from typing import TypedDict, Any

from langchain_core.callbacks import AsyncCallbackHandler
from langchain_core.messages import BaseMessage, ToolMessage, HumanMessage, AIMessage
from langchain_core.prompts import (
    ChatPromptTemplate,
    SystemMessagePromptTemplate,
    MessagesPlaceholder,
    HumanMessagePromptTemplate,
)
from langchain_core.runnables import RunnableSerializable, ConfigurableField
from langchain_core.tools import tool
from langchain_ollama import ChatOllama
from rich.pretty import pprint


def log(text):
    pprint(text)
    pprint("=" * 50)


# ---------------------- Tools ---------------------- #

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


TOOLS = [add, subtract, multiply, divide, exponentiate, final_output]
TOOL_MAP = {tool.name: tool.func for tool in TOOLS}

# ---------------------- Prompt ---------------------- #

PROMPT = ChatPromptTemplate.from_messages([
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

# ---------------------- Model ---------------------- #

LLM = ChatOllama(model="qwen2.5:latest").configurable_fields(
    callbacks=ConfigurableField(
        id="callbacks",
        name="callbacks",
        description="A list of callbacks to use for streaming",
    )
)


# ---------------------- Callback Handler ---------------------- #

class QueueCallbackHandler(AsyncCallbackHandler):
    def __init__(self, queue: asyncio.Queue):
        self.queue = queue
        self.final_output_seen = False

    async def __aiter__(self):
        while True:
            if self.queue.empty():
                await asyncio.sleep(0.1)
                continue
            token = await self.queue.get()
            if token == "<<DONE>>":
                return
            if token:
                yield token

    async def on_llm_new_token(self, *args, **kwargs: Any) -> None:
        chunk = kwargs.get("chunk")
        if chunk:
            tool_calls = chunk.message.additional_kwargs.get("tool_calls")
            if tool_calls and tool_calls[0]["name"] == final_output.name:
                self.final_output_seen = True
        self.queue.put_nowait(chunk)

    async def on_llm_end(self, *args, **kwargs: Any) -> None:
        self.queue.put_nowait("<<DONE>>" if self.final_output_seen else "<<STEP_END>>")


# ---------------------- Agent Executor ---------------------- #

class CustomAgentExecutor:
    def __init__(self, max_iterations: int = 5):
        self.chat_history: list[BaseMessage] = []
        self.max_iterations = max_iterations
        self.agent: RunnableSerializable = (
                {
                    "input": lambda x: x["input"],
                    "chat_history": lambda x: x["chat_history"],
                    "agent_scratchpad": lambda x: x.get("agent_scratchpad", [])
                }
                | PROMPT
                | LLM.bind_tools(tools=TOOLS, tool_choice="any")
        )

    async def invoke(self, input: str, streamer: QueueCallbackHandler, verbose: bool = False) -> str | None:
        agent_scratchpad = []
        for _ in range(self.max_iterations):
            tool_call = await self._run_agent_step(input, agent_scratchpad, streamer, verbose)
            agent_scratchpad.append(tool_call)

            tool_name = tool_call.tool_calls[0]["name"]
            tool_args = tool_call.tool_calls[0]["args"]
            tool_call_id = tool_call.tool_calls[0]["id"]

            tool_result = TOOL_MAP[tool_name](**tool_args)
            agent_scratchpad.append(ToolMessage(content=tool_result, tool_call_id=tool_call_id))

            if tool_name == final_output.name:
                output = tool_result["output"]
                self.chat_history.extend([HumanMessage(content=input), AIMessage(content=output)])
                return json.dumps(output)
        return None

    async def _run_agent_step(self, input: str, scratchpad: list, streamer: QueueCallbackHandler,
                              verbose: bool) -> AIMessage:
        response = self.agent.with_config(callbacks=[streamer])
        output = None

        async for token in response.astream({
            "input": input,
            "chat_history": self.chat_history,
            "agent_scratchpad": scratchpad,
        }):
            if output is None:
                output = token
            else:
                output += token

            if verbose and token.content:
                pprint(f"content: {token.content}")
            if tool_calls := token.additional_kwargs.get("tool_calls"):
                if verbose:
                    pprint(f"tool_calls: {tool_calls}")
        return AIMessage(
            content=output.content,
            tool_calls=output.tool_calls,
            tool_call_id=output.tool_calls[0]["id"]
        )


# ---------------------- Runner ---------------------- #

async def run_agent():
    executor = CustomAgentExecutor()
    queue = asyncio.Queue()
    streamer = QueueCallbackHandler(queue=queue)
    task = asyncio.create_task(executor.invoke(input="What is 10 + 10", streamer=streamer))

    async for token in streamer:
        if token == "<<STEP_END>>":
            log("\n")
        elif tool_calls := token.message.additional_kwargs.get("tool_calls"):
            if tool_name := tool_calls[0]["function"]["name"]:
                log(f"Calling {tool_name}...")
            if tool_args := tool_calls[0]["function"]["arguments"]:
                pprint(f"{tool_args}")

    await task


if __name__ == "__main__":
    asyncio.run(run_agent())

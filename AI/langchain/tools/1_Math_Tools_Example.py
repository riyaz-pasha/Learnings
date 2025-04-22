import json
import pprint

from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain.memory import ConversationBufferMemory
from langchain.tools import tool
from langchain_core.prompts import ChatPromptTemplate, SystemMessagePromptTemplate, MessagesPlaceholder, \
    HumanMessagePromptTemplate
from langchain_ollama import ChatOllama


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


print(add)
print(add.name)
print(add.description)
print(add.args_schema.model_json_schema())

example_llm_output_string = '{"x":5,"y":2}'
output_dict = json.loads(example_llm_output_string)  # load as dictionary
print(output_dict)

prompt = ChatPromptTemplate([
    SystemMessagePromptTemplate.from_template("you're a helpful assistant"),
    MessagesPlaceholder(variable_name="chat_history"),
    HumanMessagePromptTemplate.from_template("{input}"),
    ("placeholder", "{agent_scratchpad}")
])
llm = ChatOllama(model="qwen2.5:latest")
print("=" * 50)
# print(prompt)
# print(prompt.format(chat_history=[], input="what is 2 + 2"))

memory = ConversationBufferMemory(
    memory_key="chat_history",
    return_messages=True,
)

tools = [add, subtract, multiply, divide, exponentiate]
agent = create_tool_calling_agent(
    llm=llm,
    tools=tools,
    prompt=prompt,
)

res = agent.invoke({
    "input": "I have 2 Apples and My friend has 4 Apples. Total how many Apples we have?",
    "chat_history": memory.chat_memory.messages,
    "intermediate_steps": []
})
print("=" * 40)
print(res)

agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    memory=memory,
    verbose=True,
)
res2 = agent_executor.invoke({
    "input": "I have 2 Apples and My friend has 4 Apples. Total how many Apples we have?",
    "chat_history": memory.chat_memory.messages,
})
print("=" * 60)
pprint.pp(res2)

res3 = agent_executor.invoke({
    "input": "Another fiend joined with us and brought 3 Apples. Total how many Apples we have?",
    "chat_history": memory,
})
print("=" * 60)
pprint.pprint(res3)

res3 = agent_executor.invoke({
    "input": "If we divide apples equally how many apples each one gets?",
    "chat_history": memory,
})
print("=" * 60)
pprint.pp(res3)

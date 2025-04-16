from langchain.agents import AgentType, Tool, initialize_agent
from langchain_ollama import OllamaLLM
from pydantic import BaseModel, Field


class AddInput(BaseModel):
    a: int = Field(..., description="The first integer.")
    b: int = Field(..., description="The second integer.")


class MultiplyInput(BaseModel):
    a: int = Field(..., description="The first integer.")
    b: int = Field(..., description="The second integer.")


def add_two_numbers(a: int, b: int) -> int:
    return a + b


def multiply_two_numbers(a: int, b: int) -> int:
    return a * b


# Wrap functions as tools with input schemas
tools = [
    Tool.from_function(
        func=add_two_numbers,
        name="add_two_numbers",
        description="Add two integers.",
        args_schema=AddInput
    ),
    Tool.from_function(
        func=multiply_two_numbers,
        name="multiply_two_numbers",
        description="Multiply two integers.",
        args_schema=MultiplyInput
    )
]

# Initialize the LLM and agent
llm = OllamaLLM(model="qwen2.5:7b")
agent = initialize_agent(
    tools=tools,
    llm=llm,
    agent_type=AgentType.STRUCTURED_CHAT_ZERO_SHOT_REACT_DESCRIPTION,
    verbose=True
)

# Test messages
messages = [
    "What is 4 plus 5?",
    "What is 4 times 6?",
    "What is 10 divided by 2?",
    "What is 3 to the power of 2?",
    "What comes after Sunday?"
]

for user_input in messages:
    response = agent.invoke({"input": user_input})
    print(response)
    print("=======================================")

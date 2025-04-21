from typing import Any

from agents import (Agent, FunctionTool, OpenAIChatCompletionsModel,
                    RunContextWrapper, Runner, set_default_openai_client,
                    set_tracing_disabled)
from langchain.prompts import ChatPromptTemplate
from langchain_core.utils.function_calling import convert_to_openai_function
from openai import AsyncOpenAI
from pydantic import BaseModel, Field

# Initialize Ollama client
external_client = AsyncOpenAI(
    base_url='http://localhost:11434/v1',
    api_key='ollama',  # required, but unused
)

set_default_openai_client(external_client)
set_tracing_disabled(True)

# Create model instance with Ollama
model = OpenAIChatCompletionsModel(
    model="qwen2.5:7b",  # or another model
    openai_client=external_client,
)


class WeatherSearch(BaseModel):
    """Get current weather of a given city"""
    city: str = Field(description="Name of the city to get the weather for")


def get_weather(city: str) -> str:
    return f"The weather in {city} is sunny with a temperature of 30°C."


async def weather_tool_invoke(context: RunContextWrapper[Any], params_json: str) -> str:
    try:
        # Parse the JSON params into the WeatherSearch schema
        params = WeatherSearch.model_validate_json(params_json)
        return get_weather(city=params.city)
    except Exception as e:
        return f"Error getting weather: {str(e)}"

print(WeatherSearch.model_json_schema())
# Define the FunctionTool
weather_tool = FunctionTool(
    name=get_weather.__name__,
    description=WeatherSearch.__doc__,
    params_json_schema=WeatherSearch.model_json_schema(),
    on_invoke_tool=weather_tool_invoke,
    strict_json_schema=True
)

agent = Agent(
    name="Weather Assistant",
    instructions="You are a helpful weather assistant. Use the get_weather tool when asked about the weather.",
    model=model,
    tools=[weather_tool]
)


async def main():
    response = await Runner.run(agent, input="What's the weather in Paris?")
    print(response.final_output)

if __name__ == "__main__":
    import asyncio
    asyncio.run(main())

# weather_function = convert_to_openai_function(WeatherSearch)
# print(weather_function)
# model_with_tool = model.bind(functions=[weather_function])
# prompt = ChatPromptTemplate.from_messages([
#     ("system", "You are a helpful assistant. Use tools if needed."),
#     ("human", "{question}")
# ])
# user_input = prompt.format_messages(
#     question="What's the weather like in Hyderabad?"
# )
# response = model_with_tool(user_input)

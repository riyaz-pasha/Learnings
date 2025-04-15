from agents import (OpenAIChatCompletionsModel, set_default_openai_client,
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
    """Get current weather of a given location"""
    city: str = Field(description="Name of the city to get the weather for")


def get_weather(city: str) -> str:
    return f"The weather in {city} is sunny with a temperature of 30°C."


weather_function = convert_to_openai_function(WeatherSearch)
print(weather_function)

model_with_tool = model.bind(functions=[weather_function])

prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant. Use tools if needed."),
    ("human", "{question}")
])

user_input = prompt.format_messages(
    question="What's the weather like in Hyderabad?"
)

response = model_with_tool(user_input)

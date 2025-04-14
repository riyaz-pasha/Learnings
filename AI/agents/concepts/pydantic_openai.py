from langchain.prompts import ChatPromptTemplate
from langchain_core.utils.function_calling import convert_to_openai_function
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field


class WeatherSearch(BaseModel):
    """Get current weather of a given location"""
    city: str = Field(description="Name of the city to get the weather for")


def get_weather(city: str) -> str:
    return f"The weather in {city} is sunny with a temperature of 30°C."


weather_function = convert_to_openai_function(WeatherSearch)
print(weather_function)

prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant. Use tools if needed."),
    ("human", "{question}")
])

model = ChatOpenAI()
model_with_tool = model.bind(functions=[weather_function])

user_input = prompt.format_messages(
    question="What's the weather like in Hyderabad?"
)

response = model_with_tool.invoke(user_input)

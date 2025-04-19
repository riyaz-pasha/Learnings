from typing import Sequence

from ollama import Message, chat


def get_weather(city: str):
    weather_data = {
        "Hyderabad": "30",
        "Ramagundam": "35",
        "Mumbai": "28",
        "Delhi": "33",
    }
    return weather_data.get(city, "Not Found")


def get_model_response(messages: Sequence[Message]):
    return chat(
        model="qwen2.5:latest",
        messages=messages,
    )


messages: Sequence[Message] = [
    Message(role="user", content="What's the weather in Hyderabad?")
]

response = get_model_response(messages=messages)
print(response)

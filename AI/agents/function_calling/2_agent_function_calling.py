import json
from typing import Sequence

from ollama import Message, chat


def get_weather(city: str):
    """Get current weather of a given location"""
    weather_data = {
        "Hyderabad": "30",
        "Ramagundam": "35",
        "Mumbai": "28",
        "Delhi": "33",
    }
    return weather_data.get(city, "Not Found")


available_functions = {
    get_weather.__name__: get_weather,
}


def get_model_response(messages: Sequence[Message]):
    return chat(
        model="qwen2.5:latest",
        messages=messages,
        tools=[get_weather]
    )


def execute_tool_calls(tool_calls: list):
    """Executes any tool calls suggested by the model."""
    for tool_call in tool_calls or []:
        function_name = tool_call.function.name
        arguments = tool_call.function.arguments
        function_to_call = available_functions.get(function_name)
        if function_to_call:
            result = function_to_call(**arguments)
            print(f"Function output: {result}")
        else:
            print(f"Function '{function_name}' not found.")


def process_messages(messages):
    for message in messages:
        response = get_model_response([message])
        print(response)
        execute_tool_calls(response.message.tool_calls)
        print("========================================")


messages: Sequence[Message] = [
    Message(role="user", content="What is the weather in Hyderabad?"),
]

process_messages(messages)

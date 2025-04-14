import json
import os

import openai
from dotenv import find_dotenv, load_dotenv

load_dotenv(find_dotenv())

# openAiApiKey=os.getenv('OPENAI_API_KEY') # Safe
openai_api_key = os.environ['OPENAI_API_KEY']  # raises error
openai.api_key = openai_api_key


def get_weather(city: str) -> str:
    """Get current weather of a given location"""
    weather_data = {
        "Hyderabad": "32",
    }
    return weather_data.get(city, "Not Found")


functions = [
    {
        "name": get_weather.__name__,
        "description": get_weather.__doc__,
        "parameters": {
            "type": "object",
            "properties": {
                "city": {
                    "type": "string",
                    "description": "Name of the city, e.g. Hyderabad, Delhi, Mumbai",
                },
                "unit": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"],
                },
            },
            "required": ["location"],
        }
    }
]

# print(functions)

messages = [
    {
        "role": "user",
        "content": "What is the weather in Hyderabad?"
    }
]

client = openai.OpenAI()

response = client.chat.completions.create(
    model="gpt-3.5-turbo",
    messages=messages,
    # function_call="auto"
    # function_call="none"
    # function_call={"name": get_weather.__name__}
    functions=functions
)

print(response)

# open AI

response_message = response.choices[0].message
print(response_message)

fn_call = response_message.function_call

fn_call_args = json.loads(fn_call.arguments)

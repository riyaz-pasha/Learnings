import asyncio
import json
import os
from typing import Any

import aiohttp
from agents import (Agent, FunctionTool, OpenAIChatCompletionsModel,
                    RunContextWrapper, Runner, set_default_openai_client,
                    set_tracing_disabled)
from dotenv import load_dotenv
from openai import AsyncOpenAI

# Load environment variables
load_dotenv()

# Initialize Ollama client
external_client = AsyncOpenAI(
    base_url='http://localhost:11434/v1',
    api_key='ollama',  # required, but unused
)

set_default_openai_client(external_client)
set_tracing_disabled(True)

# Create model instance with Ollama
model = OpenAIChatCompletionsModel(
    model="qwen2.5:7b",
    openai_client=external_client,
)

# Create a helper function for getting weather


async def fetch_weather(location: str) -> str:
    """Get current weather for a location."""
    async with aiohttp.ClientSession() as session:
        async with session.get(f"https://wttr.in/{location}?format=j1") as response:
            if response.status == 200:
                data = await response.json()
                current = data.get("current_condition", [{}])[0]
                return f"Weather in {location}: {current.get('weatherDesc', [{}])[0].get('value', 'Unknown')}, Temperature: {current.get('temp_C', 'Unknown')}°C"
            else:
                return f"Could not retrieve weather for {location}. Status code: {response.status}"

# Create the on_invoke_tool function


async def weather_tool_invoke(context: RunContextWrapper[Any], params_json: str) -> str:
    try:
        # Parse the JSON parameters
        params = json.loads(params_json)
        location = params.get("location", "")

        if not location:
            return "Error: Location parameter is required"

        # Use the helper function to get the weather
        return await fetch_weather(location)
    except Exception as e:
        return f"Error retrieving weather: {str(e)}"

weather_tool = FunctionTool(
    name="get_weather",
    description="Get the current weather for a specified location",
    params_json_schema={
        "type": "object",
         "properties": {
                "location": {
                    "type": "string",
                    "description": "The city or location to get weather for, e.g., 'Tokyo' or 'New York'"
                }
         },
        "required": ["location"]
    },
    on_invoke_tool=weather_tool_invoke,
    strict_json_schema=True
)

agent = Agent(
    name="Weather assistant",
    instructions="You are a weather expert. Use the get_weather tool to find current weather information when asked about weather in a location.",
    model=model,
    tools=[weather_tool]
)


async def main():
    # Create a FunctionTool with the correct parameters
    response = await Runner.run(agent, input="What's the weather in Tokyo?")
    print(response.final_output)

if __name__ == "__main__":
    asyncio.run(main())

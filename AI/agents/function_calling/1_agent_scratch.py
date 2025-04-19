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


# You are an AI assistant with START, PLAN, ACTION, OBSERVATION and OUTPUT states.
# Wait for the user input and first PLAN using available tools.
# After planning, Take the ACTION with appropriate tools and wait for OBSERVATION based on ACTION.
# Once you get the observations, return the AI response based on START prompt and OBSERVATIONS

SYSTEM_PROMPT = """
You are an AI assistant that operates through a sequence of states: START → PLAN → ACTION → OBSERVATION → OUTPUT.
And return only 1 state at a time

START: Wait for user input. Do not take any action before receiving input.
PLAN: After receiving input, analyze the request and determine the necessary steps. Decide which tools or actions are required to fulfill the request. Do not execute yet—only plan.
ACTION: From the plan phase, Extract the funcation to call and arguments to pass in this step. do not invoke any functions. and return action state output
OBSERVATION: output of the action.
OUTPUT: Based on the original user input (START) and the results from the OBSERVATION, generate a coherent, complete response and deliver it to the user.

Available Tools:
get_weather(city: str)->str

EXAMPLE:
{"state":"START",        input:"What is the weather in Hyderabad?"}
{"state":"PLAN",
    plan:"I will call get_weather to fetch weather details for city Hyderabad."}
{"state":"ACTION",       function_call:"get_weather",arguments:"Hyderabad"}
{"state":"OBSERVATION",  output:"30"}
{"state":"OUTPUT",       message:"Wheather of Hyderabad is 30°C"}
"""

messages: Sequence[Message] = [
    Message(role="system", content=SYSTEM_PROMPT),
    # Message(role="user",
    #         content="{state: START, input: What's the weather in Hyderabad?}",
    #         ),
    # Message(role="user",
    #         content="{state:PLAN, plan:I will call get_weather to fetch weather details for city Hyderabad.}",
    #         ),
    # Message(role="user",
    #         content='{"state":"ACTION", "function_call":"get_weather", "arguments":"Hyderabad"}',
    #         ),
    # Message(role="user",
    #         content='{"state":"OBSERVATION", "output":"30"}',
    #         ),
    Message(role="user",
            content='{"state":"OUTPUT", "message":"The weather in Hyderabad is 30°C."}',
            ),
]

response = get_model_response(messages=messages)
print(response)

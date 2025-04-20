import json
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

Respond using only one state at a time in proper JSON format.

Behavior by state:

START:
- Accept the user input.
- Do not analyze, plan, or take action—just receive the input.

PLAN:
- Based on the input from the START state, decide what needs to be done and which function/tool (if any) to use.
- Do not execute—just create a plan.

ACTION:
- Based on the PLAN, specify the function to be called and the exact arguments.
- Do not call the function.

OBSERVATION:
- Represent the result of the function call provided in the last ACTION step.
- Do not return the final output here—just proceed to OUTPUT.

OUTPUT:
- Use the original input and the observation to create a final response for the user.

Available Tools:
- get_weather(city: str) -> str

Always respond in valid JSON format.

Example interaction:

{
  "state": "START",
  "input": "What is the weather in Hyderabad?"
}
{
  "state": "PLAN",
  "plan": "I will call get_weather to fetch weather details for city Hyderabad."
}
{
  "state": "ACTION",
  "function_call": "get_weather",
  "arguments": "Hyderabad"
}
{
  "state": "OBSERVATION",
  "output": "30"
}
{
  "state": "OUTPUT",
  "message": "Weather of Hyderabad is 30°C"
}
"""

# messages: Sequence[Message] = [
#     Message(role="system", content=SYSTEM_PROMPT),
#     # Message(role="user",
#     #         content="{state: START, input: What's the weather in Hyderabad?}",
#     #         ),
#     # Message(role="user",
#     #         content="{state:PLAN, plan:I will call get_weather to fetch weather details for city Hyderabad.}",
#     #         ),
#     # Message(role="user",
#     #         content='{"state":"ACTION", "function_call":"get_weather", "arguments":"Hyderabad"}',
#     #         ),
#     # Message(role="user",
#     #         content='{"state":"OBSERVATION", "output":"30"}',
#     #         ),
#     Message(role="user",
#             content='{"state":"OUTPUT", "message":"The weather in Hyderabad is 30°C."}',
#             ),
# ]


def handle_state(state, content):
    if state == 'ACTION':
        fn = content.get('function_call')
        args = content.get('arguments')

        if fn == 'get_weather':
            result = get_weather(args)
            # print(result)

            return Message(
                role='user',
                content=json.dumps({
                    "state": "OBSERVATION",
                    "output": result,
                })
            )
        else:
            print("Unknown function:", fn)
            return None

    else:
        return Message(
            role='user',
            content=json.dumps(content)
        )


def process_messages(messages):
    while True:
        response = get_model_response(messages=messages)
        print(response.message)

        content = json.loads(response.message.content)
        # print(content)

        state = content.get('state')

        if state == 'OUTPUT':
            break

        next_state = handle_state(state, content)
        if next_state is None:
            break  # Exit if there's an unknown function or unhandled state

        messages.append(next_state)
        print("=" * 70)


messages: Sequence[Message] = [
    Message(role="system", content=SYSTEM_PROMPT),
    # Message(role="user", content=json.dumps({
    #     "state": "START",
    #     "input": "What is the weather in Hyderabad?"
    # })),
    Message(role="user", content="What is the weather in Hyderabad?"),
]

process_messages(messages)

import ollama


def add_two_numbers(a: int, b: int) -> int:
    """
    Add two numbers.

    Args:
        a: The first integer.
        b: The second integer.

    Returns:
        The sum of the two numbers.
    """
    return a + b


def multiply_two_numbers(a: int, b: int) -> int:
    """
    Multiply two numbers.

    Args:
        a: The first integer.
        b: The second integer.

    Returns:
        The multiplied value of the two numbers.
    """
    return a * b


available_functions = {
    add_two_numbers.__name__: add_two_numbers,
    multiply_two_numbers.__name__: multiply_two_numbers,
}


def get_model_response(content: str) -> ollama.ChatResponse:
    return ollama.chat(
        model='qwen2.5:7b',
        messages=[{'role': 'user', 'content': content}],
        tools=[add_two_numbers, multiply_two_numbers],
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


messages = [
    "What is 4 plus 5?",
    "What is 4 times 6?",
    "What is 10 divided by 2",
    "What is 3 power 2",
    "What comes after Sunday?",
]

for message in messages:
    response = get_model_response(message)
    print(response)
    execute_tool_calls(response.message.tool_calls)
    print("========================================")

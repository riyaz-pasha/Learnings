from agents import (OpenAIChatCompletionsModel, set_default_openai_client,
                    set_tracing_disabled)
from openai import AsyncOpenAI

client = AsyncOpenAI(base_url='http://localhost:11431/v1', api_key='ollama')
set_default_openai_client(client=client)
set_tracing_disabled(True)

model = OpenAIChatCompletionsModel(model='qwen2.5:7b', openai_client=client)

messages = [
    {"role": "user", "content": "Suggest me a movie to watch"}
]
response = model(input=messages)
print(response)

import asyncio

from langchain_ollama import ChatOllama
from rich.pretty import pprint

llm = ChatOllama(model="qwen2.5:latest")

tokens = []


async def stream_response(prompt: str):
    async for token in llm.astream(prompt):
        tokens.append(token)
        print(token.content, end="|", flush=True)


async def main():
    await stream_response("What is streaming in LLMs?")


if __name__ == "__main__":
    asyncio.run(main())
    pprint(tokens)
    pprint("=" * 50)
    pprint(tokens[0] + tokens[1] + tokens[2])

import asyncio

from langchain_ollama import ChatOllama

llm = ChatOllama(model="qwen2.5:latest")


async def stream_response(prompt: str):
    async for token in llm.astream(prompt):
        print(token.content, end="|", flush=True)


async def main():
    await stream_response("What is streaming in LLMs?")


if __name__ == "__main__":
    asyncio.run(main())

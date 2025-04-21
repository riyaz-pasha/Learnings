import asyncio

from browser_use import Agent
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

load_dotenv()
llm = ChatOpenAI(model="gpt-4o")


async def main():
    agent = Agent(
        task="Find Trains from Hyderabad to Ramagundam on 24th April with 2 1st AC seats. prefer using to confirmtkt.com",
        llm=llm,
    )
    result = await agent.run()
    print(result)

asyncio.run(main())

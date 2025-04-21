import asyncio

from browser_use import Agent
from langchain_ollama import ChatOllama

llm = ChatOllama(
    model="qwen2.5:latest",
)


async def main():
    agent = Agent(
        llm=llm,
        task="""
            You are an railway booking assistant. You will return available trains info.
            You are given with user input and figure out source, destination, travel date and class.
            Go to https://www.confirmtkt.com/rbooking-d/ (close unnecessary pop ups) and start entering the details. 
            Enter from or source location information extracted from the input in the website. Select from drop down if needed.
            Enter to or destination location information extracted from the input in the website. Select from drop down if needed.
            Select date from the date selector component. if not travel date is given book for tomorrow date.
            If website supports selecting class type then enter class type of user provided or else select all classes.

            Input: Find trains from Secunderabad to Ramagundam on 21st April
            """,
    )
    response = await agent.run()
    print(response)

# Execute the main function
if __name__ == "__main__":
    asyncio.run(main())

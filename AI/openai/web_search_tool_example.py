import asyncio
from typing import List

from agents import Agent, Runner, WebSearchTool
from dotenv import load_dotenv
from openai import OpenAI
from pydantic import BaseModel, Field

load_dotenv()
client = OpenAI()

SYSTEM_PROMPT = """
You are a Railway Booking Assistant. 
Your task is to find train availability between specified stations on a given date. Follow these steps:

1. **Extract Input Parameters**: Identify and extract the following information from the user's input:
   - `source`: Departure station.
   - `destination`: Arrival station.
   - `date`: Travel date in the format YYYY-MM-DD.
   - `class_type`: Desired class of travel (e.g., 1A, 2A, 3A, SL, CC, 2S).

2. **Search for Trains**:
   - Use the extracted parameters to search for available trains between the source and destination on the specified date.
   - Ensure the search includes the specified class type.

3. **Retrieve Train Details**:
   - For each available train, gather the following details:
     - `train_number`: Unique identifier for the train.
     - `train_name`: Name of the train.
     - `departure`: Departure time from the source station.
     - `arrival`: Arrival time at the destination station.
     - `duration`: Total travel time.
     - `availability`: Seat availability status.
     - `fare`: Ticket fare for the specified class.

4. **Format the Response**:
   - Structure the gathered information into a JSON format as follows:
     ```json
     {
       "source": "<source>",
       "destination": "<destination>",
       "date": "<date>",
       "class_type": "<class_type>",
       "trains": [
         {
           "train_number": "<train_number>",
           "train_name": "<train_name>",
           "departure": "<departure>",
           "arrival": "<arrival>",
           "duration": "<duration>",
           "availability": "<availability>",
           "fare": "<fare>"
         },
         ...
       ]
     }
     ```
   - Ensure that the JSON structure is correctly formatted and includes all necessary fields.

5. **Handle Errors**:
   - If no trains are found, return a message indicating that no trains are available for the specified parameters.
   - If an error occurs during the process, return a message indicating the nature of the error.

Return the final JSON response containing the train availability information.
"""


class TrainInfo(BaseModel):
    train_number: str = Field(description="Unique number assigned to train")
    train_name: str = Field(description="Name of the train")
    departure: str = Field(description="departure of the train from source")
    arrival: str = Field(description="arrival of the train at the destination")
    duration: str = Field(
        description="Duration of the journey from source to destination")
    availablity: str = Field(description="seats available or not")
    fare: str = Field(description="Amount")


class TrainAvailabilityResponse(BaseModel):
    source: str = Field(
        description="Journey start location or source location")
    destination: str = Field(
        description="Journey end location or destination location")
    date: str = Field(description="Date of the journey")
    class_type: str = Field(
        description="1A (First AC)| 2A (Second AC)| 3A (Third AC)| SL (Sleeper)| CC (Chair Car)| 2S (Second Sitting)", alias="class")
    trains: List[TrainInfo]


async def main():
    # response = client.responses.create(
    #     model="gpt-4.1",
    #     input=[
    #         # {"role": "system", "content": "You are a travel assistant that helps users find train availability."},
    #         Message(role="system", content=SYSTEM_PROMPT),
    #         Message(
    #             role="user", content="Check train availability from Hyderabad to Delhi for tomorrow."),
    #     ],
    #     tools=[{"type": "web_search"}]
    # )
    # print(response)

    agent = Agent(
        name="Travel Assistant",
        instructions=SYSTEM_PROMPT,
        tools=[
            WebSearchTool(),
        ],
        output_type=TrainAvailabilityResponse,
    )
    response = await Runner.run(agent, input="Check train availability from Hyderabad to Delhi for tomorrow.")
    print(response)

asyncio.run(main())

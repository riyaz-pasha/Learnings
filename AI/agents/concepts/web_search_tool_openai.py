import asyncio
from typing import List

from browser_use import Agent, Controller
from langchain_ollama import ChatOllama
from pydantic import BaseModel, Field


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


# Initialize the ChatOllama model
llm = ChatOllama(
    model="qwen2.5:latest",
    format="json"  # Ensure the model returns JSON-formatted responses
)

task_description = """
You are a Railway Booking Assistant. Your task is to find train availability between specified stations on a given date. Follow these steps:

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

controller = Controller(output_model=TrainAvailabilityResponse)
# Create the agent with the specified output model
agent = Agent(
    task=task_description,
    llm=llm,
    max_actions_per_step=1,
    controller=controller
)

# Define the main function to run the agent


async def main():
    response = await agent.run()
    print(response)

# Execute the main function
if __name__ == "__main__":
    asyncio.run(main())

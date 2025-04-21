import asyncio
from typing import List

from agents import (OpenAIChatCompletionsModel, set_default_openai_client,
                    set_tracing_disabled)
from openai import AsyncOpenAI
from pydantic import BaseModel, Field

external_client = AsyncOpenAI(
    base_url="http://localhost:11431/v1",
    api_key="ollama"
)
set_default_openai_client(external_client)
set_tracing_disabled(True)

model = OpenAIChatCompletionsModel(
    model="qwen2.5:7b",
    openai_client=external_client,
)


class TrainInfo(BaseModel):
    train_number: str = Field(description="Unique number assigned to train")
    train_name: str = Field(description="Name of the train")
    departure: str = Field(description="departure of the train from source")
    arrival: str = Field(description="arrival of the train at the destination")
    duration: str = Field(
        description="Duration of the journey from source to destination")
    availablity: str = Field(description="seats available or not")
    fare: str = Field(description="Amount")


class TrainAvailability(BaseModel):
    source: str = Field(
        description="Journey start location or source location")
    destination: str = Field(
        description="Journey end location or destination location")
    date: str = Field(description="Date of the journey")
    class_type: str = Field(
        description="1A (First AC)| 2A (Second AC)| 3A (Third AC)| SL (Sleeper)| CC (Chair Car)| 2S (Second Sitting)", alias="class")
    trains: List[TrainInfo]

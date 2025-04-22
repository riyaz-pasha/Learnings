import pprint
from datetime import datetime

import requests
from dotenv import load_dotenv
from langchain.agents import load_tools, create_tool_calling_agent, AgentExecutor
from langchain.tools import tool
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

load_dotenv()

llm = ChatOllama(model="qwen2.5:latest")
toolbox = load_tools(tool_names=['serpapi'], llm=llm)


@tool
def get_location_from_ip():
    """Get the geographical location based on the IP address."""
    try:
        response = requests.get("https://ipinfo.io/json")
        data = response.json()
        if 'loc' in data:
            latitude, longitude = data['loc'].split(",")
            data = (
                f"Latitude: {latitude},\n"
                f"Longitude: {longitude},\n"
                f"City: {data.get('city', 'N/A')},\n"
                f"Country: {data.get('country', 'N/A')}"
            )
            # pprint.pprint(data)
            return data
        else:
            return "Location could not be determined."
    except Exception as e:
        return f"Error occurred: {e}"


@tool()
def get_current_datetime() -> str:
    """Return the current date and time."""
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


prompt = ChatPromptTemplate.from_messages([
    ("system", "you're a helpful assistant"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}")
])

tools = toolbox + [get_current_datetime, get_location_from_ip]
agent = create_tool_calling_agent(
    llm=llm,
    tools=tools,
    prompt=prompt,
)
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    verbose=True,
)
out = agent_executor.invoke({
    "input": (
        "I have a few questions, what is the date and time right now? "
        "How is the weather where I am? Please give me degrees in Celsius"
    )
})
pprint.pprint(out)

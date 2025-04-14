from langchain.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
from langchain.schema.output_parser import StrOutputParser
from langchain_core.runnables import RunnableSequence

prompt = ChatPromptTemplate.from_template(
    "tell me a short joke about {topic}"
)
model = ChatOllama(model="qwen2.5:7b")
output_parser = StrOutputParser()
chain = prompt | model | output_parser
# chain = RunnableSequence(steps=[prompt, model, output_parser])
response = chain.invoke({"topic": "bears"})
print(response)

from langchain.prompts import (ChatPromptTemplate,
                               FewShotChatMessagePromptTemplate,
                               HumanMessagePromptTemplate,
                               SystemMessagePromptTemplate)

end = "\n"+"="*50+"\n"

system_prompt = SystemMessagePromptTemplate.from_template(
    template="You are an helpful assistant",
)

prompt1 = HumanMessagePromptTemplate.from_template(
    template="What is 2+2?"
)

prompt2 = HumanMessagePromptTemplate.from_template(
    template="What is {input}",
)

chat_prompt1 = ChatPromptTemplate.from_messages(
    [
        system_prompt,
        prompt1
    ],
)

print(chat_prompt1, end=end)
print(chat_prompt1.format(), end=end)

chat_prompt2 = ChatPromptTemplate.from_messages(
    [
        system_prompt,
        prompt2
    ],
)
print(chat_prompt2,  end=end)
print(chat_prompt2.format(input="4+4"),  end=end)

examples = [
    {"input": "what is 2+2", "output": "4"},
    {"input": "what is 3*3", "output": "3"},
    {"input": "what is 5/2", "output": "2.5"},
]

example_prompt = ChatPromptTemplate.from_messages([
    ("human", "{input}"),
    ("ai", "{output}"),
])

few_shot_prompt = FewShotChatMessagePromptTemplate(
    example_prompt=example_prompt,
    examples=examples,
)

print(few_shot_prompt.format(), end=end)

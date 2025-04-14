from typing import List

from pydantic import BaseModel


class UserNormal:
    def __init__(self, name: str, age: int, email: str):
        self.name = name
        self.age = age
        self.email = email


user1 = UserNormal(name="Riyaz", age=27, email="test@email.com")
user2 = UserNormal(name="Riyaz", age="Twenty Seven", email="test@email.com")
print(user1)
print(user2)
print(user2.age)


class UserPydantic(BaseModel):
    name: str
    age: int
    email: str


user3 = UserPydantic(name="Riyaz", age=27, email="test@email.com")
user4 = UserPydantic(name="Riyaz", age="Twenty Seven",
                     email="test@email.com")  # throws error
print(user3)


class Class(BaseModel):
    students: List[UserPydantic]

from langchain_core.runnables import RunnableLambda
from rich.pretty import pprint


class Runnable:
    def __init__(self, func):
        self.func = func

    def __or__(self, other_func):
        def chained_func(*args, **kwargs):
            return other_func.invoke(self.func(*args, **kwargs))

        return Runnable(chained_func)

    def invoke(self, *args, **kwargs):
        return self.func(*args, **kwargs)


def double(x):
    return 2 * x


def square(x):
    return x * x


def add_five(x):
    return x + 5


double_runnable = Runnable(double)
square_runnable = Runnable(square)
add_five_runnable = Runnable(add_five)
chain = square_runnable.__or__(double_runnable).__or__(add_five_runnable)
pprint(chain.invoke(2))

chain2 = square_runnable | double_runnable | add_five_runnable
pprint(chain2.invoke(2))

double_runnable_lambda = RunnableLambda(double)
square_runnable_lambda = RunnableLambda(square)
add_five_runnable_lambda = RunnableLambda(add_five)
chain3 = square_runnable_lambda | double_runnable_lambda | add_five_runnable_lambda
pprint(chain3.invoke(2))
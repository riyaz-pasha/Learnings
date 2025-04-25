from rich.pretty import pprint
from rich.console import Console
from rich.table import Table


class MinASCIIDeleteSum:

    def pretty_print_dp_table(self, s1: str, s2: str, dp: list) -> None:
        console = Console()
        table = Table(title="DP Table (Minimum ASCII Delete Sum)")

        # Add column headers
        table.add_column("i\\j", justify="right")
        for j in range(len(s2) + 1):
            label = s2[j] if j < len(s2) else "∅"
            table.add_column(label, justify="right")

        # Add rows
        for i in range(len(s1) + 1):
            label = s1[i] if i < len(s1) else "∅"
            row = [label] + [str(dp[i][j]) for j in range(len(s2) + 1)]
            table.add_row(*row)

        console.print(table)

    def pretty_print_dp_table2(self, s1: str, s2: str, dp: list) -> None:
        console = Console()
        table = Table(title="DP Table (Minimum ASCII Delete Sum)")

        # Add column headers
        table.add_column("i\\j", justify="right")
        for j in range(len(s2) + 1):
            label = "∅" if j == 0 else s2[j-1]
            table.add_column(label, justify="right")

        # Add rows
        for i in range(len(s1) + 1):
            label = "∅" if i == 0 else s1[i-1]
            row = [label] + [str(dp[i][j]) for j in range(len(s2) + 1)]
            table.add_row(*row)

        console.print(table)

    def minimum_delete_sum2(self, s1: str, s2: str) -> int:
        m = len(s1)
        n = len(s2)
        dp = [[0] * (n + 1) for _ in range(m + 1)]

        # Base cases
        for i in range(1, m + 1):
            dp[i][0] = dp[i - 1][0] + ord(s1[i - 1])
        self.pretty_print_dp_table2(s1, s2, dp)

        for j in range(1, n + 1):
            dp[0][j] = dp[0][j - 1] + ord(s2[j - 1])
        self.pretty_print_dp_table2(s1, s2, dp)

        # Fill the DP table
        for i in range(1, m + 1):
            for j in range(1, n + 1):
                pprint({"i": i, "j": j})
                if s1[i - 1] == s2[j - 1]:
                    dp[i][j] = dp[i - 1][j - 1]
                else:
                    delete_s1 = ord(s1[i - 1]) + dp[i - 1][j]
                    delete_s2 = ord(s2[j - 1]) + dp[i][j - 1]
                    pprint({"delete_s1": delete_s1,
                           "delete_s2": delete_s2,
                            s1[i - 1]: ord(s1[i - 1]),
                            s2[j - 1]: ord(s2[j - 1]),
                            })
                    dp[i][j] = min(delete_s1, delete_s2)
                    # dp[i][j] = min(ord(s1[i - 1]) + dp[i - 1][j],
                    #                ord(s2[j - 1]) + dp[i][j - 1])
                self.pretty_print_dp_table2(s1, s2, dp)

        return dp[m][n]

    def minimum_delete_sum(self, s1: str, s2: str) -> int:
        m, n = len(s1), len(s2)
        dp = [[0] * (n + 1) for _ in range(m + 1)]
        self.pretty_print_dp_table(s1, s2, dp)

        # Base case: delete all characters from s1
        for i in range(m - 1, -1, -1):
            dp[i][n] = dp[i + 1][n] + ord(s1[i])
        self.pretty_print_dp_table(s1, s2, dp)

        # Base case: delete all characters from s2
        for j in range(n - 1, -1, -1):
            dp[m][j] = dp[m][j + 1] + ord(s2[j])
        self.pretty_print_dp_table(s1, s2, dp)

        # Fill the table
        for i in range(m - 1, -1, -1):
            for j in range(n - 1, -1, -1):
                pprint({"i": i, "j": j})
                if s1[i] == s2[j]:
                    dp[i][j] = dp[i + 1][j + 1]
                else:
                    delete_s1 = ord(s1[i]) + dp[i + 1][j]
                    delete_s2 = ord(s2[j]) + dp[i][j + 1]
                    pprint({"delete_s1": delete_s1, "delete_s2": delete_s2})
                    dp[i][j] = min(delete_s1, delete_s2)
                self.pretty_print_dp_table(s1, s2, dp)
        # Pretty print the DP table
        print("DP Table:")
        self.pretty_print_dp_table(s1, s2, dp)

        return dp[0][0]


# Example usage
solver = MinASCIIDeleteSum()
# result = solver.minimum_delete_sum2("sea", "eat")
result = solver.minimum_delete_sum2("delete", "leet")
print("Minimum ASCII Delete Sum:", result)

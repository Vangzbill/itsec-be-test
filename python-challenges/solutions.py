import re

def reverse_words(s: str) -> str:
    """Reverse characters in words, preserving spacing."""
    return " ".join(word[::-1] for word in s.split(" "))

def two_sum_brute_force(nums: list[int], target: int) -> list[int]:
    """Return indices of two numbers summing to target."""
    for i in range(len(nums)):
        for j in range(i + 1, len(nums)):
            if nums[i] + nums[j] == target:
                return [i, j]
    return []

def two_sum_hash_map(nums: list[int], target: int) -> list[int]:
    """Return indices of two numbers summing to target."""
    seen = {}
    for i, num in enumerate(nums):
        diff = target - num
        if diff in seen:
            return [seen[diff], i]
        seen[num] = i
    return []

def fizzbuzz(n: int) -> list[str]:
    """Return FizzBuzz sequence up to n."""
    result = []
    for i in range(1, n + 1):
        if i % 15 == 0:
            result.append("FizzBuzz")
        elif i % 3 == 0:
            result.append("Fizz")
        elif i % 5 == 0:
            result.append("Buzz")
        else:
            result.append(str(i))
    return result

def print_fizzbuzz(n: int) -> None:
    """Print FizzBuzz sequence up to n."""
    for line in fizzbuzz(n):
        print(line)

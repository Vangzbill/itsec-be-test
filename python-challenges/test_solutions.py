import pytest
from solutions import reverse_words, two_sum_brute_force, two_sum_hash_map, fizzbuzz

def test_reverse_words():
    assert reverse_words("This is an example!") == "sihT si na !elpmaxe"
    assert reverse_words("  hello world  ") == "  olleh dlrow  "
    assert reverse_words("multiple   spaces") == "elpitlum   secaps"
    assert reverse_words("") == ""
    assert reverse_words("     ") == "     "
    assert reverse_words("a") == "a"
    assert reverse_words("  a  b  ") == "  a  b  "
    assert reverse_words("trailing space ") == "gniliart ecaps "

def test_two_sum():
    for func in [two_sum_brute_force, two_sum_hash_map]:
        assert func([2, 7, 11, 15], 9) == [0, 1]
        assert func([3, 2, 4], 6) == [1, 2]
        assert func([3, 3], 6) == [0, 1]
        assert func([-1, -2, -3, -4, -5], -8) == [2, 4]
        assert func([1, 2, 3], 7) == []
        assert func([5, 5, 5], 10) == [0, 1]

def test_fizzbuzz():
    assert fizzbuzz(15) == [
        "1", "2", "Fizz", "4", "Buzz", "Fizz", "7", "8", "Fizz", "Buzz",
        "11", "Fizz", "13", "14", "FizzBuzz"
    ]
    assert fizzbuzz(1) == ["1"]
    assert fizzbuzz(0) == []

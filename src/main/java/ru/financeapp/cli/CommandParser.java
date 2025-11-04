package ru.financeapp.cli;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ru.financeapp.exceptions.InvalidInputException;

public class CommandParser {
    public static class Command {
        public String action;
        public List<String> args;

        public Command(String input) {
            if (input == null || input.trim().isEmpty()) {
                throw new InvalidInputException("Empty command");
            }
            List<String> parts =
                    Arrays.stream(input.trim().split("\\s+")).collect(Collectors.toList());
            action = parts.get(0).toLowerCase();
            args = parts.subList(1, parts.size());
        }
    }
}

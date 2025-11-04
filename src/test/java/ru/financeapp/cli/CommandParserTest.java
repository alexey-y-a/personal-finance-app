package ru.financeapp.cli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ru.financeapp.exceptions.InvalidInputException;

class CommandParserTest {

    @Test
    void parse_ValidCommand_SetsActionAndArgs() {
        CommandParser.Command cmd = new CommandParser.Command("add income food 1000");
        assertEquals("add", cmd.action);
        assertEquals(3, cmd.args.size());
        assertEquals("income", cmd.args.get(0));
    }

    @Test
    void parse_EmptyInput_Throws() {
        assertThrows(InvalidInputException.class, () -> new CommandParser.Command(""));
    }

    @Test
    void parse_SingleWord_SetsEmptyArgs() {
        CommandParser.Command cmd = new CommandParser.Command("help");
        assertEquals("help", cmd.action);
        assertTrue(cmd.args.isEmpty());
    }

    @Test
    void parse_MultipleArgs_HandlesSpaces() {
        CommandParser.Command cmd = new CommandParser.Command("stats categories food transport");
        assertEquals("stats", cmd.action);
        assertEquals(3, cmd.args.size());
        assertEquals("categories", cmd.args.get(0));
    }
}

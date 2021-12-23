package net.jcom.jchess.server.exception;

public class InvalidSchemaVersion extends Exception {
    public InvalidSchemaVersion(String expected, String actual) {
        super(String.format("Expected version is %s but transmitted was %s", expected, actual));
    }
}

package net.relserver.core.api.model;

import net.relserver.core.Constants;

public class Request {
    private final Operation operation;
    private final String message;

    public Request(Operation operation, String message) {
        this.operation = operation;
        this.message = message;
    }

    public static Request of(String message) {
        int separatorIndex = message.indexOf(Constants.SEPARATOR);
        Operation operation = Operation.valueOf(message.substring(0, separatorIndex));
        String request = message.substring(separatorIndex + 1);
        return new Request(operation, request);
    }

    public Operation getOperation() {
        return operation;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return operation + Constants.SEPARATOR + message;
    }
}

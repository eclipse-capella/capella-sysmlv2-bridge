package org.obeonetwork.capella.sysmlv2.cli;

public final class TransformationCommandException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int exitCode;

    public TransformationCommandException(int exitCode, String message, Throwable cause) {
        super(message, cause);
        if (exitCode < 2 || exitCode > 6) {
            throw new IllegalArgumentException("exitCode must identify a runtime transformation failure");
        }
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}

package org.jenkinsci.plugins.testrail.TestRail;

public class TestRailException extends Exception {
    private static final long serialVersionUID = 9162568949904090503L;

    public TestRailException(String message) {
        super(message);
    }

    public TestRailException(String message, Exception innerException) {
        super(message, innerException);
    }
}

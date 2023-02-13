package org.jenkinsci.plugins.testrail.TestRail;

public class ElementNotFoundException extends Exception {
    private static final long serialVersionUID = 1256229093040803158L;

    public ElementNotFoundException(String message) {
        super(message);
    }
}

package org.jenkinsci.plugins.testrail.TestRail;

public enum CaseStatus {
    PASSED(1),
    BLOCKED(2),
    UNTESTED(3),
    RETEST(4),
    FAILED(5);

    private final int id;

    CaseStatus(int id) {
        this.id = id;
    }

    public int getValue() {
        return id;
    }
}

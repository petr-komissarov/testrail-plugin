package org.jenkinsci.plugins.testrail.TestRail;

public class TestRailResult {
    private int caseId;
    private CaseStatus status;
    private Float elapsed;
    private String comment;

    public TestRailResult(int caseId, CaseStatus status, String comment, Float elapsed) {
        this.caseId = caseId;
        this.status = status;
        this.comment = comment;
        this.elapsed = elapsed;
    }

    public int getCaseId() {
        return this.caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public CaseStatus getStatus() {
        return this.status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public Float getElapsed() {
        return this.elapsed;
    }

    public void setElapsed(float timeInSeconds) {
        this.elapsed = timeInSeconds;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getElapsedTimeString() {
        int time = (elapsed == null || elapsed.intValue() == 0) ? 1 : elapsed.intValue();

        return time + "s";
    }
}

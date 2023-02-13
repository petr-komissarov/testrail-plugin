package org.jenkinsci.plugins.testrail.TestRail;

import java.util.ArrayList;
import java.util.List;

public class TestRailResults {

    private List<TestRailResult> results;

    public TestRailResults() {
        this.results = new ArrayList<TestRailResult>();
    }

    public void addResult(TestRailResult result) {
        this.results.add(result);
    }

    public List<TestRailResult> getResults() {
        return this.results;
    }

    public void setResults(ArrayList<TestRailResult> results) {
        this.results = results;
    }

    public void merge(TestRailResults other) {
        List<TestRailResult> allResults = other.getResults();
        ArrayList<TestRailResult> failedResults = new ArrayList<>();

        for (TestRailResult r : allResults) {
            if (r.getStatus() == CaseStatus.FAILED) {
                failedResults.add(r);
            } else {
                this.results.add(r);
            }
        }
        this.results.addAll(failedResults);
    }
}

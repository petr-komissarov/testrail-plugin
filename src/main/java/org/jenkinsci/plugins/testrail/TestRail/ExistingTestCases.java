package org.jenkinsci.plugins.testrail.TestRail;

import org.jenkinsci.plugins.testrail.JUnit.TestCase;
import org.jenkinsci.plugins.testrail.TestRailClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExistingTestCases {
    private final TestRailClient testRailClient;
    private final int projectId;
    private final int suiteId;
    private final List<Case> cases;
    private final List<Section> sections;

    public ExistingTestCases(TestRailClient testRailClient, int projectId, int suite)
            throws IOException, ElementNotFoundException {
        this.projectId = projectId;
        this.testRailClient = testRailClient;
        this.suiteId = suite;
        this.cases = new ArrayList<>(Arrays.asList(testRailClient.getCases(this.projectId, this.suiteId)));
        this.sections = new ArrayList<>(Arrays.asList(testRailClient.getSections(this.projectId, this.suiteId)));
    }

    public int getProjectId() {
        return this.projectId;
    }

    public int getSuiteId() {
        return this.suiteId;
    }

    public List<Case> getCases() {
        return this.cases;
    }

    public List<Section> getSections() {
        return this.sections;
    }

    private String getSectionName(int sectionId) throws ElementNotFoundException {
        for (Section section : sections) {
            if (section.getId() == sectionId) {
                return section.getName();
            }
        }
        throw new ElementNotFoundException("sectionId: " + sectionId);
    }

    public int getCaseId(String sectionName, String caseName) throws ElementNotFoundException {
        for (Case testcase : cases) {
            if (testcase.getTitle().equals(caseName)) {
                for (Section section : sections) {
                    if (section.getName().equals(sectionName) && (testcase.getSectionId() == section.getId())) {
                        return testcase.getId();
                    }
                }
            }
        }
        throw new ElementNotFoundException(sectionName + ": " + caseName);
    }

    public int getSectionId(String sectionName) throws ElementNotFoundException {
        for (Section section : sections) {
            if (section.getName().equals(sectionName)) {
                return section.getId();
            }
        }
        throw new ElementNotFoundException(sectionName);
    }

    public int addSection(String sectionName, String parentId)
            throws ElementNotFoundException, TestRailException {
        Section addedSection = testRailClient.addSection(sectionName, projectId, suiteId, parentId);
        sections.add(addedSection);
        return addedSection.getId();
    }

    public int addCase(TestCase caseToAdd, int sectionId) throws TestRailException {
        Case addedCase = testRailClient.addCase(caseToAdd, sectionId);
        cases.add(addedCase);
        return addedCase.getId();
    }

    public String[] listTestCases() throws ElementNotFoundException {
        ArrayList<String> result = new ArrayList<>();
        for (Case testcase : cases) {
            String sectionName = getSectionName(testcase.getSectionId());
            result.add(sectionName + ": " + testcase.getTitle());
        }
        return result.toArray(new String[0]);
    }
}


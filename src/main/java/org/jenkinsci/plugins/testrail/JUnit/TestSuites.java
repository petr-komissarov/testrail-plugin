package org.jenkinsci.plugins.testrail.JUnit;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "testsuites")
public class TestSuites {
    private int failures;
    private int errors;
    private int skipped;
    private int tests;
    private List<TestSuite> suites;

    public int getTests() {
        return tests;
    }

    public void setTests(int tests) {
        this.tests = tests;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public List<TestSuite> getSuites() {
        return this.suites;
    }

    @XmlElement(name = "testsuite")
    public void setSuites(List<TestSuite> suites) {
        this.suites = suites;
    }

    public boolean hasSuites() {
        return this.suites != null && this.suites.size() > 0;
    }
}

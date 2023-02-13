package org.jenkinsci.plugins.testrail.JUnit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "testsuite")
public class TestSuite {
    private String name;
    private int failures;
    private int errors;
    private int skipped;
    private List<TestCase> cases;
    private List<TestSuite> suites;

    public String getName() {
        return this.name;
    }

    @XmlAttribute
    public void setName(String name) {
        this.name = name.trim();
    }

    public List<TestCase> getCases() {
        return this.cases;
    }

    @XmlElement(name = "testcase")
    public void setCases(List<TestCase> cases) {
        this.cases = cases;
    }

    public List<TestSuite> getSuites() {
        return this.suites;
    }

    @XmlElement(name = "testsuite")
    public void setSuites(List<TestSuite> suites) {
        this.suites = suites;
    }

    public int getFailures() {
        return this.failures;
    }

    @XmlAttribute
    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getErrors() {
        return this.errors;
    }

    @XmlAttribute
    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getSkipped() {
        return this.skipped;
    }

    @XmlAttribute
    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public boolean hasSuites() {
        return this.suites != null;
    }

    public boolean hasCases() {
        return this.cases != null;
    }
}

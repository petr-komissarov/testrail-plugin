package org.jenkinsci.plugins.testrail.JUnit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

public class TestCase {
    private String name;
    private Failure failure;
    private Skipped skipped;
    private Float time;
    private String refs;

    public String getName() {
        return this.name;
    }

    @XmlAttribute
    public void setName(String name) {
        this.name = name.trim();
    }

    public Failure getFailure() {
        return this.failure;
    }

    @XmlElements({
            @XmlElement(name = "failure"),
            @XmlElement(name = "error")
    })
    public void setFailure(Failure failure) {
        this.failure = failure;
    }

    public Skipped getSkipped() {
        return this.skipped;
    }

    @XmlElement(name = "skipped")
    public void setSkipped(Skipped skipped) {
        this.skipped = skipped;
    }

    public Float getTime() {
        return this.time;
    }

    @XmlAttribute(name = "time")
    public void setTime(Float time) {
        this.time = time;
    }

    public String getRefs() {
        return this.refs;
    }

    @XmlAttribute(name = "refs")
    public void setRefs(String refs) {
        this.refs = refs;
    }
}

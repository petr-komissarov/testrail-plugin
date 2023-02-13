package org.jenkinsci.plugins.testrail.JUnit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class Failure {
    private String type;
    private String message;
    private String text;

    public String getType() {
        return this.type;
    }

    @XmlAttribute
    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return this.message;
    }

    @XmlAttribute
    public void setMessage(String message) {
        this.message = message;
    }

    public String getText() {
        return this.text;
    }

    @XmlValue
    public void setText(String text) {
        this.text = text;
    }
}

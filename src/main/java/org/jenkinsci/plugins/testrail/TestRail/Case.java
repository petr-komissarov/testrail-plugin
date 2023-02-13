package org.jenkinsci.plugins.testrail.TestRail;


public class Case {
    private int id;
    private String title;
    private int sectionId;
    private String refs;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title.trim();
    }

    public int getSectionId() {
        return this.sectionId;
    }

    public void setSectionId(int sectionId) {
        this.sectionId = sectionId;
    }

    public String getRefs() {
        return this.refs;
    }

    public void setRefs(String refs) {
        this.refs = refs;
    }
}

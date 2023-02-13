package org.jenkinsci.plugins.testrail.TestRail;

public class Section {
    private int id;
    private int suiteId;
    private String name;
    private String parentId;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSuiteId() {
        return this.suiteId;
    }

    public void setSuiteId(int suiteId) {
        this.suiteId = suiteId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getParentId() {
        return this.parentId;
    }

    public void setParentId(String id) {
        this.parentId = id;
    }
}

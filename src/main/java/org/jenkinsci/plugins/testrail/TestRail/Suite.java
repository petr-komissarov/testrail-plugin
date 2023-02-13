package org.jenkinsci.plugins.testrail.TestRail;

public class Suite {
    private int id;
    private String name;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getStringId() {
        return Integer.toString(id);
    }

}

package com.redhat.hacbs.resources.model.v1alpha1;

import java.util.List;

public class BuildRecipe {

    private String task;
    private String image;
    private List<String> commandLine;

    private String enforceVersion;

    public String getImage() {
        return image;
    }

    public BuildRecipe setImage(String image) {
        this.image = image;
        return this;
    }

    public List<String> getCommandLine() {
        return commandLine;
    }

    public BuildRecipe setCommandLine(List<String> commandLine) {
        this.commandLine = commandLine;
        return this;
    }

    public String getEnforceVersion() {
        return enforceVersion;
    }

    public BuildRecipe setEnforceVersion(String enforceVersion) {
        this.enforceVersion = enforceVersion;
        return this;
    }

    public String getTask() {
        return task;
    }

    public BuildRecipe setTask(String task) {
        this.task = task;
        return this;
    }
}

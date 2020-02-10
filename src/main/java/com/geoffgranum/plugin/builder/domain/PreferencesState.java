package com.geoffgranum.plugin.builder.domain;

import com.google.gson.Gson;

/**
 * @author ggranum
 */
public final class PreferencesState {

  public final boolean copyFieldAnnotations;

  public final boolean generateJsonAnnotations;

  public final boolean generateToJsonMethod;

  public final boolean generateFromJsonMethod;

  public final boolean implementValidated;

  public final boolean generateCopyMethod;

  public final boolean generateExampleCodeComment;

  public final boolean useWithPrefix;

  public final boolean useSpork;

  private PreferencesState(Builder builder) {
    copyFieldAnnotations = builder.copyFieldAnnotations;
    generateJsonAnnotations = builder.generateJsonAnnotations;
    generateToJsonMethod = builder.generateToJsonMethod;
    generateFromJsonMethod = builder.generateFromJsonMethod;
    implementValidated = builder.implementValidated;
    generateCopyMethod = builder.generateCopyMethod;
    generateExampleCodeComment = builder.generateExampleCodeComment;
    useWithPrefix = builder.useWithPrefix;
    useSpork = builder.useSpork;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  public static PreferencesState.Builder fromJson(String value) {
    return new Gson().fromJson(value, PreferencesState.Builder.class);
  }


  public static final class Builder {
    private Boolean copyFieldAnnotations = false;

    private Boolean generateJsonAnnotations = false;

    private Boolean generateToJsonMethod = false;

    private Boolean generateFromJsonMethod = false;

    private Boolean implementValidated = false;

    private Boolean generateCopyMethod = false;

    private Boolean generateExampleCodeComment = false;

    private Boolean useWithPrefix = false;

    private Boolean useSpork = true;

    public Builder() {
    }

    public Builder copyFieldAnnotations(boolean copyFieldAnnotations) {
      this.copyFieldAnnotations = copyFieldAnnotations;
      return this;
    }

    public Builder from(PreferencesState copy) {
      copyFieldAnnotations = copy.copyFieldAnnotations;
      generateJsonAnnotations = copy.generateJsonAnnotations;
      generateToJsonMethod = copy.generateToJsonMethod;
      generateFromJsonMethod = copy.generateFromJsonMethod;
      implementValidated = copy.implementValidated;
      generateCopyMethod = copy.generateCopyMethod;
      generateExampleCodeComment = copy.generateExampleCodeComment;
      useWithPrefix = copy.useWithPrefix;
      useSpork = copy.useSpork;
      return this;
    }

    public Builder generateJsonAnnotations(boolean generateJsonAnnotations) {
      this.generateJsonAnnotations = generateJsonAnnotations;
      return this;
    }

    public Builder generateToJsonMethod(boolean generateToJsonMethod) {
      this.generateToJsonMethod = generateToJsonMethod;
      return this;
    }

    public Builder generateFromJsonMethod(boolean generateFromJsonMethod) {
      this.generateFromJsonMethod = generateFromJsonMethod;
      return this;
    }

    public Builder implementValidated(boolean implementValidated) {
      this.implementValidated = implementValidated;
      return this;
    }

    public Builder generateCopyMethod(boolean generateCopyMethod) {
      this.generateCopyMethod = generateCopyMethod;
      return this;
    }

    public Builder generateExampleCodeComment(boolean generateExampleCodeComment) {
      this.generateExampleCodeComment = generateExampleCodeComment;
      return this;
    }

    public Builder useWithPrefix(boolean useWithPrefix) {
      this.useWithPrefix = useWithPrefix;
      return this;
    }

    public Builder useSpork(boolean useSpork) {
      this.useSpork = useSpork;
      return this;
    }

    public PreferencesState build() {
      return new PreferencesState(this);
    }
  }
}

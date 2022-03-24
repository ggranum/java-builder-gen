package com.geoffgranum.plugin.builder.domain;

import com.geoffgranum.plugin.builder.generate.BuilderFieldGenerator;
import com.intellij.psi.PsiClass;

import java.util.List;

/**
 * @author ggranum
 */
public final class GenerateBuilderDirective {

  public final PsiClass containerClass;

  public final List<BuilderFieldGenerator> fields;

  public final boolean preferImmutability;

  public final boolean implementJackson;

  public final boolean generateToJsonMethod;

  public final boolean generateFromJsonMethod;

  public final boolean copyFieldAnnotations;

  public final boolean implementValidated;

  public final boolean generateExampleCodeComment;

  public final boolean createCopyMethod;

  public final boolean usePrefixWith;

  public final boolean useSpork;



  private GenerateBuilderDirective(Builder builder) {
    containerClass = builder.containerClass;
    fields = builder.fields;
    preferImmutability = builder.preferImmutability;
    implementJackson = builder.generateJsonAnnotations;
    generateToJsonMethod = builder.generateToJsonMethod;
    generateFromJsonMethod = builder.generateFromJsonMethod;
    implementValidated = builder.implementValidated;
    generateExampleCodeComment = builder.generateExampleCodeComment;
    createCopyMethod = builder.createCopyMethod;
    usePrefixWith = builder.usePrefixWith;
    useSpork = builder.useSpork;
    copyFieldAnnotations = builder.copyFieldAnnotations;
  }


  public String utilBaseClassPath() {
    return useSpork ? "com.geoffgranum.spork.common" : "com.geoffgranum.uttu.core";
  }

  public String fromJsonExceptionClass() {
    return utilBaseClassPath() + ".exception.FormattedException";
  }

  public String toJsonExceptionClass() {
    return utilBaseClassPath() + ".exception.FormattedException";
  }



  public static final class Builder {
    private boolean generateToJsonMethod;

    private boolean generateFromJsonMethod;

    private PsiClass containerClass;

    private List<BuilderFieldGenerator> fields;

    private boolean preferImmutability;

    private boolean generateJsonAnnotations;

    private boolean implementValidated;

    private boolean generateExampleCodeComment;

    private boolean createCopyMethod;

    private boolean usePrefixWith;

    private boolean useSpork;

    private boolean copyFieldAnnotations;

    public Builder() {
    }

    public Builder containerClass(PsiClass containerClass) {
      this.containerClass = containerClass;
      return this;
    }

    public Builder generateCopyMethod(boolean createCopyMethod) {
      this.createCopyMethod = createCopyMethod;
      return this;
    }

    public Builder fields(List<BuilderFieldGenerator> fields) {
      this.fields = fields;
      return this;
    }

    public Builder generateExampleCodeComment(boolean generateExampleCodeComment) {
      this.generateExampleCodeComment = generateExampleCodeComment;
      return this;
    }

    public Builder generateJsonAnnotation(boolean generateJsonAnnotations) {
      this.generateJsonAnnotations = generateJsonAnnotations;
      return this;
    }

    public Builder generateToJsonMethod(boolean generateToJsonMethod){
      this.generateToJsonMethod = generateToJsonMethod;
      return this;
    }

    public Builder generateFromJsonMethod(boolean implementFromJson){
      this.generateFromJsonMethod = implementFromJson;
      return this;
    }

    public Builder copyFieldAnnotations(boolean copyFieldAnnotations) {
      this.copyFieldAnnotations = copyFieldAnnotations;
      return this;
    }

    public Builder implementValidated(boolean implementValidated) {
      this.implementValidated = implementValidated;
      return this;
    }

    public Builder preferImmutability(boolean preferImmutability) {
      this.preferImmutability = preferImmutability;
      return this;
    }

    public Builder usePrefixWith(boolean usePrefixWith) {
      this.usePrefixWith = usePrefixWith;
      return this;
    }

    public Builder useSpork(boolean useSpork) {
      this.useSpork = useSpork;
      return this;
    }

    public GenerateBuilderDirective build() {
      // tempting to throw an error generateTo/From JSON methods is enabled, but JSON Annotations are not. But
      // the code will be created and could work, and maybe the user wants it that way?
      return new GenerateBuilderDirective(this);
    }
  }
}

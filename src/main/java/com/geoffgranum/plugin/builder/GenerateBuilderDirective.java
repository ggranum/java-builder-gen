package com.geoffgranum.plugin.builder;

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

  public final boolean implementValidated;

  public final boolean generateExampleCodeComment;

  public final boolean createCopyMethod;

  public final boolean usePrefixWith;

  private GenerateBuilderDirective(Builder builder) {
    containerClass = builder.containerClass;
    fields = builder.fields;
    preferImmutability = builder.preferImmutability;
    implementJackson = builder.implementJackson;
    implementValidated = builder.implementValidated;
    generateExampleCodeComment = builder.generateExampleCodeComment;
    createCopyMethod = builder.createCopyMethod;
    usePrefixWith = builder.usePrefixWith;
  }


  public static final class Builder {
    private PsiClass containerClass;

    private List<BuilderFieldGenerator> fields;

    private boolean preferImmutability;

    private boolean implementJackson;

    private boolean implementValidated;

    private boolean generateExampleCodeComment;

    private boolean createCopyMethod;

    private boolean usePrefixWith;

    public Builder() {
    }

    public Builder containerClass(PsiClass containerClass) {
      this.containerClass = containerClass;
      return this;
    }

    public Builder createCopyMethod(boolean createCopyMethod) {
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

    public Builder implementJackson(boolean implementJackson) {
      this.implementJackson = implementJackson;
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

    public GenerateBuilderDirective build() {
      return new GenerateBuilderDirective(this);
    }
  }
}

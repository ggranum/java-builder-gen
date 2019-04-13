package com.geoffgranum.plugin.builder.generate;

import com.geoffgranum.plugin.builder.GenerateBuilderDirective;
import com.geoffgranum.plugin.builder.TypeGenerationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.apache.commons.lang.StringUtils;

/**
 * @author Geoff M. Granum
 */
public class BuilderClassGenerator {

  private static final String PRIMARY_CLASS_CTOR_FMT = "private %s(%s builder) { %s }";

  private final GenerateBuilderDirective directive;

  private PsiClass builderClass;

  public BuilderClassGenerator(GenerateBuilderDirective directive) {
    this.directive = directive;
  }

  public void makeSelf(PsiElementFactory psiElementFactory) {
    builderClass = directive.containerClass.findInnerClassByName(TypeGenerationUtil.BUILDER_CLASS_NAME, false);
    if (builderClass != null) {
      // @todo: ggranum: Scrap info about existing builder class so we can augment it.
      builderClass.delete();
    }
    builderClass = TypeGenerationUtil.createBuilderClass(directive.containerClass, directive.implementValidated);
    builderClass = (PsiClass) directive.containerClass.add(builderClass);

    makeContainerClassCtorTakingBuilder(psiElementFactory);

    BuilderFieldGenerator previous = null;
    for (BuilderFieldGenerator field : directive.fields) {
      field.makeSelf(directive, builderClass, previous, psiElementFactory);
      previous = field;
    }

    makeBuilderCtor(psiElementFactory);
    if (directive.createCopyMethod) {
      makeFromCopyMethod(psiElementFactory);
    }

    makeBuildMethod(psiElementFactory);

    if (directive.implementJackson) {
      addJacksonAnnotationToContainerClass(psiElementFactory);
    }

    JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(directive.containerClass.getProject());
    styleManager.shortenClassReferences(directive.containerClass);

    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(directive.containerClass.getProject());
    codeStyleManager.reformat(directive.containerClass);
  }

  private void makeContainerClassCtorTakingBuilder(PsiElementFactory psiElementFactory) {
    TypeGenerationUtil.addMethod(directive.containerClass,
                                 null,
                                 this.makePrimaryClassConstructorString(),
                                 true,
                                 psiElementFactory);
  }

  private String makePrimaryClassConstructorString() {
    return String.format(PRIMARY_CLASS_CTOR_FMT,
                         directive.containerClass.getName(),
                         builderClass.getName(),
                         generateConstructorBody());
  }

  private String generateConstructorBody() {
    StringBuilder b = new StringBuilder();

    for (BuilderFieldGenerator field : directive.fields) {
      b.append(field.toConstructorDeclaration());
    }
    return b.toString();
  }

  private void addJacksonAnnotationToContainerClass(PsiElementFactory psiElementFactory) {
    String deserialize = "@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder = "
                         + directive.containerClass.getName()
                         + ".Builder.class)";

    PsiAnnotation jsonDeserializerAnnotation =
      directive.containerClass.getAnnotation("com.fasterxml.jackson.databind.annotation.JsonDeserialize");
    if (jsonDeserializerAnnotation != null) {
      // probably don't need to delete and re-add, but on the off chance that the className was changed without
      // the annotation being updated. Or perhaps the option was un-checked on re-run.
      jsonDeserializerAnnotation.delete();
    }
    TypeGenerationUtil.addAnnotation(directive.containerClass, deserialize, psiElementFactory);
  }

  private void makeBuildMethod(PsiElementFactory psiElementFactory) {
    TypeGenerationUtil.addMethod(builderClass,
                                 null,
                                 String.format("public %1$s build() { \n %2$s return new %1$s(this); \n}",
                                               directive.containerClass.getQualifiedName(),
                                               directive.implementValidated ? "checkValid();\n" : ""),
                                 psiElementFactory);
  }

  private void makeFromCopyMethod(PsiElementFactory psiElementFactory) {

    String fmt = "public %1$s from(%2$s copy){\n" + "  %3$s" + "  return this;" + "}";

    StringBuilder body = new StringBuilder();

    for (BuilderFieldGenerator field : directive.fields) {
      body.append(field.copyCtorInitializationString());
    }

    String methodText =
      String.format(fmt, builderClass.getName(), directive.containerClass.getQualifiedName(), body.toString());

    TypeGenerationUtil.addMethod(builderClass, null, methodText, true, psiElementFactory);
  }

  /**
   * Create the constructor for the Builder class.
   */
  private void makeBuilderCtor(PsiElementFactory psiElementFactory) {
    String comment = directive.generateExampleCodeComment ? generateExampleComment() : "";
    String fmt = "public %1$s(){}";
    TypeGenerationUtil.addMethod(builderClass,
                                 null,
                                 comment + String.format(fmt, builderClass.getName()),
                                 psiElementFactory);
  }

  private String generateExampleComment() {
    StringBuilder comment = new StringBuilder();
    comment.append("/*\n");
    // MyClass myClass = new MyClass.Builder().
    String className = directive.containerClass.getName();
    comment.append("\t")
           .append(className)
           .append(" ")
           .append(StringUtils.uncapitalize(className))
           .append(" = new ")
           .append(className)
           .append(".")
           .append(builderClass.getName())
           .append("()");
    for (BuilderFieldGenerator field : directive.fields) {
      String fieldName = field.info.field.getName(); // @todo: ggranum: This is wrong for Optional.
      comment.append("\n\t.")
             .append(fieldName)
             .append("( input.get")
             .append(StringUtils.capitalize(fieldName))
             .append("() )");
    }
    comment.append("\n\t.build();\n");
    comment.append("*/\n");
    return comment.toString();
  }


}
 

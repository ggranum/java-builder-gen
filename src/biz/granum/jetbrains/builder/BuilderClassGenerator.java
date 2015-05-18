package biz.granum.jetbrains.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * @author Geoff M. Granum
 */
public class BuilderClassGenerator {

  public static final String PRIMARY_CLASS_CTOR_FMT = "private %s(%s builder) { %s }";

  private final PsiClass containerClass;
  private final List<BuilderField> fields;
  private final boolean implementJackson;
  private final boolean implementValidated;
  private PsiClass builderClass;

  public BuilderClassGenerator(Builder builder) {
    containerClass = builder.containerClass;
    fields = builder.builderFields;
    implementJackson = builder.implementJackson;
    implementValidated = builder.implementValidated;
  }

  public void makeSelf(PsiElementFactory psiElementFactory) {
    builderClass = containerClass.findInnerClassByName(TypeGenerationUtil.BUILDER_CLASS_NAME, false);
    if(builderClass == null) {
      builderClass = TypeGenerationUtil.createBuilderClass(containerClass, implementValidated);
      builderClass = (PsiClass)containerClass.add(builderClass);
    }

    makeContainerClassCtorTakingBuilder(psiElementFactory);

    BuilderField previous = null;
    for (BuilderField field : fields) {
      field.makeSelf(implementJackson, implementValidated, builderClass, previous, psiElementFactory);
      previous = field;
    }

    makeOwnCtor(psiElementFactory);
    makeFromCopyMethod(psiElementFactory);
    // builder.build() method
    makeBuildMethod(psiElementFactory);

    if(implementJackson) {
      addJacksonAnnotationToContainerClass(psiElementFactory);
    }

    JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(containerClass.getProject());
    styleManager.shortenClassReferences(containerClass);

    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(containerClass.getProject());
    codeStyleManager.reformat(containerClass);
  }

  private void makeContainerClassCtorTakingBuilder(PsiElementFactory psiElementFactory) {
    TypeGenerationUtil.addMethod(containerClass,
                                 null,
                                 this.makePrimaryClassConstructorString(),
                                 true,
                                 psiElementFactory);
  }

  private void addJacksonAnnotationToContainerClass(PsiElementFactory psiElementFactory) {
    String deserialize = "@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder = " + containerClass.getName() + ".Builder.class)";
    TypeGenerationUtil.addAnnotation(containerClass,
                                 deserialize,
                                 psiElementFactory);
  }

  private void makeBuildMethod(PsiElementFactory psiElementFactory) {
    TypeGenerationUtil.addMethod(builderClass,
                                 null,
                                 String.format("public %1$s build() { \n %2$s return new %1$s(this); \n}",
                                               containerClass.getQualifiedName(),
                                               implementValidated ? "checkValid();\n" : ""),
                                 psiElementFactory);
  }

  private void makeFromCopyMethod(PsiElementFactory psiElementFactory) {

    String fmt = "public %1$s from(%2$s copy){\n" +
                 "  %3$s" +
                 "  return this;" +
                 "}";

    StringBuilder body = new StringBuilder();

    for (BuilderField field : fields) {
      body.append(field.copyCtorInitializationString());
    }

    String methodText = String.format(fmt, builderClass.getName(), containerClass.getQualifiedName(), body.toString());

    TypeGenerationUtil.addMethod(builderClass,
                                 null,
                                 methodText,
                                 true,
                                 psiElementFactory);
  }

  private String generateExampleComment() {
    StringBuilder comment = new StringBuilder();
    comment.append("/*\n");
    // MyClass myClass = new MyClass.Builder().
    String className = containerClass.getName();
    comment
        .append("\t")
        .append(className)
        .append(" ")
        .append(StringUtils.uncapitalize(className))
        .append(" = new ")
        .append(className)
        .append(".").append(builderClass.getName()).append("()");
    for (BuilderField field : fields) {
      String fieldName = field.field.getName();
      comment.append("\n\t.")
          .append(fieldName).append("( input.get").append(StringUtils.capitalize(fieldName)).append("() )");
    }
    comment.append("\n\t.build();\n");
    comment.append("*/\n");
    return comment.toString();
  }

  /**
   * Create the constructor for the Builder class.
   */
  private void makeOwnCtor(PsiElementFactory psiElementFactory) {
    String fmt = "public %1$s(){}";
    TypeGenerationUtil.addMethod(builderClass,
                                 null,
                                 generateExampleComment() +
                                 String.format(fmt, builderClass.getName()),
                                 psiElementFactory
    );
  }

  private String makePrimaryClassConstructorString() {
    return String.format(PRIMARY_CLASS_CTOR_FMT,
                         containerClass.getName(),
                         builderClass.getName(),
                         generateConstructorBody());
  }

  private String generateConstructorBody() {
    StringBuilder b = new StringBuilder();

    for (BuilderField field : fields) {
      b.append(field.toConstructorDeclaration());
    }
    return b.toString();
  }

  public static class Builder {

    private PsiClass containerClass;
    private List<BuilderField> builderFields;
    private Boolean implementJackson;
    private Boolean implementValidated;

    public Builder containerClass(PsiClass clazz) {
      this.containerClass = clazz;

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

    public Builder fields(List<PsiFieldMember> fieldMembers) {
      List<BuilderField> bFields = Lists.newArrayListWithCapacity(fieldMembers.size());
      for (PsiFieldMember member : fieldMembers) {
        bFields.add(new BuilderField(member.getElement()));
      }
      this.builderFields = bFields;

      return this;
    }

    public BuilderClassGenerator build() {
      Preconditions.checkNotNull(containerClass);
      Preconditions.checkNotNull(builderFields);
      Preconditions.checkNotNull(implementJackson);
      Preconditions.checkNotNull(implementValidated);
      return new BuilderClassGenerator(this);
    }
  }
}
 

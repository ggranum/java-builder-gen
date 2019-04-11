package com.geoffgranum.plugin.builder;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoff M. Granum
 */
public class BuilderField {

  public final PsiField field;
  private PsiField builderClassField;
  private PsiElement builderClassMethod;
  private String fieldClassName;

  public BuilderField(PsiField field) {
    this.field = field;
    PsiType type = field.getType();
    if(type instanceof PsiClassReferenceType) {
      fieldClassName = ((PsiClassReferenceType)type).getClassName();
    } else {
      fieldClassName = type.getPresentableText();
    }
  }

  public void makeSelf(
                          boolean implementJackson,
                          boolean implementValidated,
                          PsiClass targetClass,
                          BuilderField afterField,
                          PsiElementFactory psiElementFactory) {
    makeField(implementJackson, implementValidated, targetClass, afterField, psiElementFactory);
    makeMethod(targetClass, afterField, psiElementFactory);
  }

  private void makeMethod(
                             PsiClass targetClass,
                             BuilderField afterField,
                             PsiElementFactory psiElementFactory) {
    // public Builder something(SomeType someValue){ this.value = someValue; return this; }
    String methodName = field.getName();
    PsiType type = field.getType();
    PsiType unboxedType = PsiPrimitiveType.getUnboxedType(type);
    if(unboxedType != null) {
      type = unboxedType;
    }
    String methodText = String.format("public %1$s %2$s(%3$s %4$s){\n  this.%4$s = %4$s;  return this;\n}",
                                      targetClass.getName(),
                                      methodName,
                                      type.getCanonicalText(),
                                      field.getName());

    builderClassMethod =
        TypeGenerationUtil.addMethod(targetClass,
                                     afterField != null ? afterField.builderClassMethod : null,
                                     methodText,
                                     psiElementFactory);
  }

  private void makeField(
                            boolean implementJackson,
                            boolean implementValidated,
                            PsiClass targetClass,
                            BuilderField afterField,
                            PsiElementFactory psiElementFactory) {
    PsiType type = field.getType();
    if(type instanceof PsiPrimitiveType) {
      type = ((PsiPrimitiveType)type).getBoxedType(field);
    }
    List<String> annotations = Lists.newArrayList();
    if(implementValidated) {
      annotations.addAll(generateValidationAnnotationsText(field.getName(), type));
    }
    if(implementJackson) {
      annotations.add("@com.fasterxml.jackson.annotation.JsonProperty");
    }

    if("Optional".equals(fieldClassName)) {
      PsiType[] parameters = ((PsiClassReferenceType)type).getParameters();
      type = parameters[0];
    }

    builderClassField = TypeGenerationUtil.addField(targetClass,
                                                    afterField != null ? afterField.builderClassField : null,
                                                    field.getName(),
                                                    type,
                                                    psiElementFactory,
                                                    annotations.toArray(new String[annotations.size()]));
  }

  private List<String> generateValidationAnnotationsText(String fieldName, PsiType type) {
    List<String> items = new ArrayList<String>();

    String fieldClassName;
    if(type instanceof PsiClassReferenceType) {
      fieldClassName = ((PsiClassReferenceType)type).getClassName();
    } else {
      fieldClassName = type.getPresentableText();
    }

    if(!type.equalsToText("Optional")) {
      // Silly ass implementation for the Size and Length validators. At least add an 'allowNull'.
      items.add("@javax.validation.constraints.NotNull");
    }

    if("String".equals(fieldClassName)) {
      if(fieldName.toLowerCase().contains("email")) {
        items.add("@org.hibernate.validator.constraints.Email");
      }
      items.add("@org.hibernate.validator.constraints.Length(min = 1, max = 100)");
    } else if("Integer".equals(fieldClassName) || "Long".equals(fieldClassName)) {
      items.add("@org.hibernate.validator.constraints.Range(min = 0, max = " + fieldClassName + ".MAX_VALUE)");
    } else if("Double".equals(fieldClassName) || "Float".equals(fieldClassName)) {
      items.add("@org.hibernate.validator.constraints.Range(min = 0, max = Long.MAX_VALUE)");
    } else if("List".equals(fieldClassName) || "Set".equals(fieldClassName)) {
      items.add("@javax.validation.constraints.Size(min = 0, max = 100)");
    }

    return items;
  }

  public String toConstructorDeclaration() {
    String declaration;
    if("Optional".equals(fieldClassName)) {
      declaration = String.format("%1$s = Optional.fromNullable( builder.%1$s );\n", field.getName());
    } else {
      declaration = String.format("%1$s = builder.%1$s;\n", field.getName());
    }
    return declaration;
  }

  public String copyCtorInitializationString() {
    String fmt = "%1$s = copy.%1$s;\n";
    return String.format(fmt, field.getName());
  }
}
 

package com.geoffgranum.plugin.builder.generate;

import com.geoffgranum.plugin.builder.GenerateBuilderDirective;
import com.geoffgranum.plugin.builder.TypeGenerationUtil;
import com.geoffgranum.plugin.builder.info.FieldInfo;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoff M. Granum
 */
public class BuilderFieldGenerator {

  private static final String BUILDER_METHOD_DEFINITION_FORMAT =
    "public %1$s %2$s(%3$s %4$s){\n  this.%4$s = %4$s;  return this;\n}";

  private static final String JACKSON_ANNOTATION_FORMAT = "@com.fasterxml.jackson.annotation.JsonProperty";

  public final FieldInfo info;

  private PsiField builderClassField;

  private PsiElement builderClassMethod;


  public BuilderFieldGenerator(FieldInfo info) {
    this.info = info;
  }

  void makeSelf(GenerateBuilderDirective directive,
                PsiClass targetClass, BuilderFieldGenerator afterField,
                PsiElementFactory psiElementFactory) {
    makeField(directive, targetClass, afterField, psiElementFactory);
    makeMethod(targetClass, afterField, psiElementFactory);
  }

  private void makeMethod(PsiClass targetClass, BuilderFieldGenerator afterField, PsiElementFactory psiElementFactory) {
    // public Builder something(SomeType someValue){ this.value = someValue; return this; }
    String methodName = info.field.getName();
    PsiType type = info.actualType;
    if (info.isAnOptional) {
      type = info.typeParameters[0];
    }
    PsiType unboxedType = PsiPrimitiveType.getUnboxedType(type);
    if (unboxedType != null) {
      type = unboxedType;
    }
    String methodText = String.format(BUILDER_METHOD_DEFINITION_FORMAT,
                                      targetClass.getName(),
                                      methodName,
                                      type.getCanonicalText(), info.field.getName());

    builderClassMethod = TypeGenerationUtil.addMethod(targetClass,
                                                      afterField != null ? afterField.builderClassMethod : null,
                                                      methodText,
                                                      psiElementFactory);
  }

  private void makeField(GenerateBuilderDirective directive,
                         PsiClass targetClass, BuilderFieldGenerator afterField, PsiElementFactory psiElementFactory) {
    PsiType type = info.field.getType();
    if (type instanceof PsiPrimitiveType) {
      type = ((PsiPrimitiveType) type).getBoxedType(info.field);
    }
    List<String> annotations = Lists.newArrayList();
    if (directive.implementValidated) {
      annotations.addAll(generateValidationAnnotationsText(info.field.getName(), type));
    }
    if (directive.implementJackson) {
      annotations.add(JACKSON_ANNOTATION_FORMAT);
    }

    if (info.isAnOptional) {
      type = info.typeParameters[0];
    }

    builderClassField = TypeGenerationUtil.addField(targetClass,
                                                    afterField != null ? afterField.builderClassField : null,
                                                    info.field.getName(),
                                                    type,
                                                    psiElementFactory,
                                                    annotations.toArray(new String[0]));

    if (!info.annotationsInfo.hasNullable) {
      if (info.isList) {
        initializeBuilderListField(psiElementFactory);
      } else if (info.isMap) {
        initializeBuilderMapField(psiElementFactory);

      }
    }


  }

  private void initializeBuilderListField(PsiElementFactory factory) {
    PsiExpression psiInitializer =
      factory.createExpressionFromText("java.util.Collections.emptyList()", builderClassField);
    builderClassField.setInitializer(psiInitializer);
  }

  private void initializeBuilderMapField(PsiElementFactory factory) {
    PsiExpression psiInitializer =
      factory.createExpressionFromText("java.util.Collections.emptyMap()", builderClassField);
    builderClassField.setInitializer(psiInitializer);
  }

  /**
   * @future: We should copy these annotations off of the Built class's field definitions, at least until
   * we can parse the existing builder (if any) during 're-generate'. Because there's no other way to know
   * what the configurations have been set to.
   */
  private List<String> generateValidationAnnotationsText(String fieldName, PsiType type) {
    List<String> bFieldAnnotations = new ArrayList<>();

    String fieldClassName;
    if (type instanceof PsiClassReferenceType) {
      fieldClassName = ((PsiClassReferenceType) type).getClassName();
    } else {
      fieldClassName = type.getPresentableText();
    }

    if (info.isAnOptional) {
      bFieldAnnotations.add("@javax.validation.constraints.Nullable");
    } else {
      bFieldAnnotations.add("@javax.validation.constraints.NotNull");
      if ("String".equals(fieldClassName)) {
        if (fieldName.toLowerCase().contains("email")) {
          bFieldAnnotations.add("@org.hibernate.validator.constraints.Email");
        }
        bFieldAnnotations.add("@org.hibernate.validator.constraints.Length(min = 1, max = 100)");
      } else if ("Integer".equals(fieldClassName) || "Long".equals(fieldClassName)) {
        bFieldAnnotations.add("@org.hibernate.validator.constraints.Range(min = 0, max = "
                              + fieldClassName
                              + ".MAX_VALUE)");
      } else if ("Double".equals(fieldClassName) || "Float".equals(fieldClassName)) {
        bFieldAnnotations.add("@org.hibernate.validator.constraints.Range(min = 0, max = Long.MAX_VALUE)");
      } else if ("List".equals(fieldClassName) || "Set".equals(fieldClassName)) {
        bFieldAnnotations.add("@javax.validation.constraints.Size(min = 0, max = 100)");
      }
    }
    return bFieldAnnotations;
  }

  String toConstructorDeclaration() {
    String declaration;
    if (info.isAnOptional) {
      declaration = String.format("%1$s = Optional.ofNullable( builder.%1$s );\n", info.field.getName());
    } else {
      declaration = String.format("%1$s = builder.%1$s;\n", info.field.getName());
    }
    return declaration;
  }

  String copyCtorInitializationString() {
    String fmt = "%1$s = copy.%1$s;\n";
    return String.format(fmt, info.field.getName());
  }
}
 

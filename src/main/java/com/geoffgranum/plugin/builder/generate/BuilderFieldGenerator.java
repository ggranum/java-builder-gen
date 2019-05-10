package com.geoffgranum.plugin.builder.generate;

import com.geoffgranum.plugin.builder.GenerateBuilderDirective;
import com.geoffgranum.plugin.builder.TypeGenerationUtil;
import com.geoffgranum.plugin.builder.info.FieldInfo;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;

import java.util.HashSet;
import java.util.Set;

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
                PsiClass targetClass,
                BuilderFieldGenerator afterField,
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
      type.getCanonicalText(),
      info.field.getName());

    builderClassMethod = TypeGenerationUtil.addMethod(targetClass,
      afterField != null ? afterField.builderClassMethod : null,
      methodText,
      psiElementFactory);
  }

  private void makeField(GenerateBuilderDirective directive,
                         PsiClass targetClass,
                         BuilderFieldGenerator afterField,
                         PsiElementFactory psiElementFactory) {
    PsiType dataType = info.field.getType();
    if (info.isPrimitiveType) {
      dataType = ((PsiPrimitiveType) dataType).getBoxedType(info.field);
    }
    Set<String> fieldAnnotations = new HashSet<>();
    if (directive.implementJackson) {
      fieldAnnotations.add(JACKSON_ANNOTATION_FORMAT);
    }

    if (info.isAnOptional) {
      dataType = info.typeParameters[0];
    }

    builderClassField = TypeGenerationUtil.addField(targetClass,
      afterField != null ? afterField.builderClassField : null,
      info.field.getName(), dataType,
      psiElementFactory, fieldAnnotations.toArray(new String[0]));

    if (!directive.copyFieldAnnotations) {
      for (PsiAnnotation fieldAnnotation : info.field.getAnnotations()) {
        String ann = fieldAnnotation.getQualifiedName();
        if (ann != null) {
          PsiAnnotation annotation = builderClassField.getAnnotation(fieldAnnotation.getQualifiedName());
          if (annotation != null) {
            annotation.delete();
          }
        }
      }
    }

    if (!info.annotationsInfo.hasNullable) {
      if (info.isList) {
        initializeBuilderListField(psiElementFactory);
      } else if (info.isMap) {
        initializeBuilderMapField(psiElementFactory);
      }
    }
    if (dataType != null && info.isPrimitiveType) {
      initializeBuilderPrimitiveField(dataType, psiElementFactory);
    }
  }

  /**
   * We initialize the primitive wrapper because int and Integer are very different things.
   */
  private void initializeBuilderPrimitiveField(PsiType type, PsiElementFactory factory) {
    String init = "0";
    if (type.getCanonicalText().contains("Boolean")) {
      init = "false";
    }
    PsiExpression psiInitializer = factory.createExpressionFromText(init, builderClassField);
    builderClassField.setInitializer(psiInitializer);
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

  String toConstructorDeclaration() {
    String value = getRealClassFieldValueFromBuilder();
    return String.format("%1$s = %2$s;", info.field.getName(), value);
  }

  /**
   * Collections need to be wrapped in immutables, this figures out which one.
   * Also handles optionals by wrapping in an ofNullable.
   */
  private String getRealClassFieldValueFromBuilder() {
    String result = "builder." + info.field.getName();
    if (info.isCollection) {
      String immutableClassName = info.getImmutableCollectionName();
      result =
        String.format("com.google.common.collect.%s.copyOf(builder.%s)", immutableClassName, info.field.getName());
    }
    if (info.isAnOptional) {
      result = "java.util.Optional.ofNullable(" + result + ")";
    }
    return result;

  }

  String copyCtorInitializationString() {
    String fmt = "%1$s = copy.%1$s;\n";
    return String.format(fmt, info.field.getName());
  }
}
 

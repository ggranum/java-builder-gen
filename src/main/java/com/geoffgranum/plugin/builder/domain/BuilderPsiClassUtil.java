package com.geoffgranum.plugin.builder.domain;

import com.geoffgranum.plugin.builder.generate.BuilderFieldGenerator;
import com.geoffgranum.plugin.builder.info.FieldInfo;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiResolveHelper;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author ggranum
 */
public class BuilderPsiClassUtil {


  public BuilderPsiClassUtil() {
  }

  /**
   * Get the list of fields to create builder methods for; this will be displayed to the user for
   * filtering/selection.
   */
  @NotNull
  public List<PsiFieldMember> findBuildableInstanceClassFields(PsiClass instanceClass) {
    List<PsiFieldMember> result = Lists.newArrayList();
    PsiClass classToExtractFieldsFrom = instanceClass;
    while (classToExtractFieldsFrom != null) {
      result.addAll(0, collectFieldsInClass(instanceClass, classToExtractFieldsFrom));
      if (classToExtractFieldsFrom.hasModifierProperty(PsiModifier.STATIC)) {
        break;
      }
      classToExtractFieldsFrom = classToExtractFieldsFrom.getSuperClass();
    }

    return result;
  }

  private List<PsiFieldMember> collectFieldsInClass(PsiClass accessObjectClass, PsiClass clazz) {
    List<PsiFieldMember> classFieldMembers = Lists.newArrayList();
    for (PsiField field : clazz.getFields()) {
      // check access to the field from the builder container class (eg. private superclass fields)
      if (fieldIsAccessibleFromBuilder(accessObjectClass, clazz, field) && !ignoringField(accessObjectClass,
        clazz,
        field)) {
        PsiClass containingClass = field.getContainingClass();
        if (containingClass != null) {
          classFieldMembers.add(new PsiFieldMember(field,
            TypeConversionUtil.getSuperClassSubstitutor(containingClass, clazz, PsiSubstitutor.EMPTY)));
        }
      }
    }

    return classFieldMembers;
  }

  private boolean fieldIsAccessibleFromBuilder(PsiClass accessObjectClass, PsiClass clazz, PsiField field) {
    PsiResolveHelper helper = JavaPsiFacade.getInstance(clazz.getProject()).getResolveHelper();
    return helper.isAccessible(field, accessObjectClass, clazz);
  }

  /**
   * Ignore any non-final fields.
   * Ignore final fields that are already initialized in the declaration.
   * Ignore final fields that are on super classes, unless the superclass is abstract.
   * Ignore static fields
   * Ignore any all-uppercase fields
   * Ignore Logging fields
   */
  private boolean ignoringField(PsiClass accessObjectClass, PsiElement clazz, PsiField field) {
    return field.hasModifierProperty(PsiModifier.STATIC)
           || !field.hasModifierProperty(PsiModifier.FINAL)
           || isAllUpperCase(field.getName())
           || isLoggingField(field)
           || isIgnoredFinalField(accessObjectClass, clazz, field);
  }

  private boolean isLoggingField(PsiField field) {
    return "org.apache.log4j.Logger".equals(field.getType().getCanonicalText())
           || "org.apache.logging.log4j.Logger".equals(field.getType().getCanonicalText())
           || "java.util.logging.Logger".equals(field.getType().getCanonicalText())
           || "org.slf4j.Logger".equals(field.getType().getCanonicalText())
           || "ch.qos.logback.classic.Logger".equals(field.getType().getCanonicalText())
           || "net.sf.microlog.core.Logger".equals(field.getType().getCanonicalText())
           || "org.apache.commons.logging.Log".equals(field.getType().getCanonicalText())
           || "org.pmw.tinylog.Logger".equals(field.getType().getCanonicalText())
           || "org.jboss.logging.Logger".equals(field.getType().getCanonicalText());
  }

  private boolean isIgnoredFinalField(PsiClass accessObjectClass, PsiElement clazz, PsiField field) {
    boolean ignored = false;
    if (field.hasModifierProperty(PsiModifier.FINAL)) {
      if (field.getInitializer() != null) {
        ignored = true; // remove final fields that are assigned in the declaration
      }
      if (!accessObjectClass.isEquivalentTo(clazz) && !accessObjectClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
        ignored = true; // remove final superclass fields
      }
    }
    return ignored;
  }

  /**
   * Does the string have a lowercase character?
   *
   * @param s the string to test.
   * @return true if the string has a lowercase character, false if not.
   */
  private boolean isAllUpperCase(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLowerCase(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public List<PsiFieldMember> findBuilderFieldsOnBuilder(PsiClass builderClass) {
    List<PsiFieldMember> result = Lists.newArrayList();

    for (PsiField field : builderClass.getFields()) {
      // check access to the field from the builder container class (eg. private superclass fields)
      PsiClass containingClass = field.getContainingClass();
      if (containingClass != null) {
        PsiSubstitutor sub =
          TypeConversionUtil.getSuperClassSubstitutor(containingClass, containingClass, PsiSubstitutor.EMPTY);
        result.add(new PsiFieldMember(field, sub));
      }
    }

    return result;
  }

  public PsiClass findChildByClass(PsiJavaFile file) {
    PsiClass result = null;
    for (PsiElement child : file.getChildren()) {
      if (child instanceof PsiClass) {
        result = (PsiClass) child;
        break;
      }
    }
    return result;
  }

  @NotNull
  public List<BuilderFieldGenerator> createBuilderFieldGenerators(List<PsiFieldMember> fieldMembers,
                                                                  PsiElementFactory psiElementFactory) {
    List<BuilderFieldGenerator> bFields = Lists.newArrayListWithCapacity(fieldMembers.size());
    CustomDataTypes customTypes = new CustomDataTypes(psiElementFactory);
    for (PsiFieldMember member : fieldMembers) {
      bFields.add(new BuilderFieldGenerator(FieldInfo.from(member.getElement(), customTypes)));
    }
    return bFields;
  }


  @NotNull
  public List<FieldInfo> getFieldInfos(List<PsiFieldMember> fieldMembers, PsiElementFactory psiElementFactory) {
    List<FieldInfo> fields = Lists.newArrayListWithCapacity(fieldMembers.size());
    CustomDataTypes customTypes = new CustomDataTypes(psiElementFactory);
    for (PsiFieldMember member : fieldMembers) {
      fields.add(FieldInfo.from(member.getElement(), customTypes));
    }
    return fields;
  }
}



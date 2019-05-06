package com.geoffgranum.plugin.builder.info;

import com.intellij.psi.PsiAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ggranum
 */
public class FieldAnnotationsInfoParser {

  public FieldAnnotationsInfoParser() {
  }

  public FieldAnnotationsInfo parse(PsiAnnotation[] annotations) {
    FieldAnnotationsInfo.Builder builder =
      new FieldAnnotationsInfo.Builder().hasNotNull(checkForAnnotationByName(annotations, "NotNull"))
        .hasNullable(checkForAnnotationByName(annotations, "Nullable"));
    return builder.build();
  }


  private boolean checkForAnnotationByName(PsiAnnotation[] annotations, String name) {
    boolean result = false;
    for (PsiAnnotation annotation : annotations) {
      String qName = annotation.getQualifiedName();
      if (qName != null && qName.contains(name)) {
        result = true;
        break;
      }
    }
    return result;

  }

  @NotNull
  private List<PsiAnnotation> annotationsInPackage(PsiAnnotation[] annotations, String packagePath) {
    List<PsiAnnotation> result = new ArrayList<>();
    for (PsiAnnotation annotation : annotations) {
      if (annotationInPackage(annotation, packagePath)) {
        result.add(annotation);
      }
    }
    return result;
  }

  private boolean annotationInPackage(PsiAnnotation annotation, String packagePath) {
    String qName = annotation.getQualifiedName();
    return qName != null && qName.startsWith(packagePath);
  }

  private List<PsiAnnotation> hibernateValidationConstraints(PsiAnnotation[] annotations) {
    return null;
  }

}

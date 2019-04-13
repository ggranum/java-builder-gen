package com.geoffgranum.plugin.builder;

import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;

/**
 * @author ggranum
 */
public class InterestingTypes {

  public final PsiType javaUtilList;

  public final PsiType javaUtilMap;

  private final PsiType javaUtilOptional;

  public InterestingTypes(PsiElementFactory factory) {
    this.javaUtilList = factory.createTypeFromText("java.util.List", null);
    this.javaUtilMap = factory.createTypeFromText("java.util.Map", null);
    this.javaUtilOptional = factory.createTypeFromText("java.util.Optional", null);
  }

  public boolean isAnOptional(PsiClassReferenceType pType) {
    return pType.isAssignableFrom(this.javaUtilOptional) || "Optional".equals(pType.getClassName());
  }

  public boolean isJavaUtilList(PsiClassReferenceType pType) {
    return pType.isAssignableFrom(this.javaUtilList);
  }

  public boolean isJavaUtilMap(PsiClassReferenceType pType) {
    return pType.isAssignableFrom(this.javaUtilMap);
  }
}

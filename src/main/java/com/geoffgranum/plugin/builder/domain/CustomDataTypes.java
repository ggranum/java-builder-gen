package com.geoffgranum.plugin.builder.domain;

import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;

/**
 * @author ggranum
 */
public class CustomDataTypes {

  public final PsiType javaUtilList;

  public final PsiType javaUtilMap;

  private final PsiType javaUtilOptional;

  private final PsiType javaUtilSet;

  private final PsiType javaUtilCollection;

  public CustomDataTypes(PsiElementFactory factory) {
    this.javaUtilCollection = factory.createTypeFromText("java.util.Collection", null);
    this.javaUtilList = factory.createTypeFromText("java.util.List", null);
    this.javaUtilSet = factory.createTypeFromText("java.util.Set", null);
    this.javaUtilMap = factory.createTypeFromText("java.util.Map", null);
    this.javaUtilOptional = factory.createTypeFromText("java.util.Optional", null);

  }

  public boolean isAnOptional(PsiClassReferenceType pType) {
    return this.javaUtilOptional.isAssignableFrom(pType) || "Optional".equals(pType.getClassName());
  }

  public boolean isJavaUtilList(PsiClassReferenceType pType) {
    return this.javaUtilList.isAssignableFrom(pType);
  }

  public boolean isJavaUtilMap(PsiClassReferenceType pType) {
    return this.javaUtilMap.isAssignableFrom(pType);
  }

  public boolean isJavaUtilSet(PsiClassReferenceType pType) {
    return this.javaUtilSet.isAssignableFrom(pType);
  }

  public boolean isJavaUtilCollection(PsiClassReferenceType pType) {
    return this.javaUtilCollection.isAssignableFrom(pType);
  }
}

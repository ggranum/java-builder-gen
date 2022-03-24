package com.geoffgranum.plugin.builder.info;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;

import java.util.List;

/**
 * @author ggranum
 */
public class BuilderInfo {
  public final PsiElementFactory psiElementFactory;

  public final PsiClass clazz;

  public final List<FieldInfo> fields;

  public BuilderInfo(Project project, PsiClass clazz, List<FieldInfo> fields) {
    this.psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    this.clazz = clazz;
    this.fields = ImmutableList.copyOf(fields);
  }

}

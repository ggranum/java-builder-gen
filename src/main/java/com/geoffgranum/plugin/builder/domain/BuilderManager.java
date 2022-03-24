package com.geoffgranum.plugin.builder.domain;

import com.geoffgranum.plugin.builder.info.BuilderInfo;
import com.google.common.collect.ImmutableList;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author ggranum
 */
public class BuilderManager {
  public final PsiClass instanceClass;

  public final List<PsiFieldMember> instanceFields;

  private final PsiJavaFile file;

  private final PsiElementFactory psiElementFactory;

  private Optional<PsiClass> builderClass;

  public final BuilderPsiClassUtil classUtil = new BuilderPsiClassUtil();

  private final Project project;

  public BuilderManager(Project project, PsiJavaFile file) {
    this.project = project;
    this.file = file;
    instanceClass = classUtil.findChildByClass(file);
    if (instanceClass != null) {
      this.instanceFields = ImmutableList.copyOf(classUtil.findBuildableInstanceClassFields(instanceClass));
      builderClass = Optional.ofNullable(instanceClass.findInnerClassByName("Builder", false));
    } else {
      instanceFields = Collections.emptyList();
    }
    psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
  }

  public PsiClass builderClass() {
    return builderClass.orElseThrow(() -> new RuntimeException("BuilderClass should have been populated by now"));
  }

  public BuilderInfo extractBuilderInfo() {
    if (!builderClass.isPresent()) {
      throw new RuntimeException("BuilderClass should have been populated before allowing this operation.");
    }
    List<PsiFieldMember> fields = classUtil.findBuilderFieldsOnBuilder(builderClass.get());
    return new BuilderInfo(project, builderClass.get(), classUtil.getFieldInfos(fields, psiElementFactory));
  }

  public boolean hasBuilder() {
    return builderClass.isPresent();
  }

  public boolean supportsBuilder() {
    return !instanceFields.isEmpty();
  }

}

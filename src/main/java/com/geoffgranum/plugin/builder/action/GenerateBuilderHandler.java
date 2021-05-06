package com.geoffgranum.plugin.builder.action;

import com.geoffgranum.plugin.builder.TypeGenerationUtil;
import com.geoffgranum.plugin.builder.domain.BuilderManager;
import com.geoffgranum.plugin.builder.domain.BuilderPsiClassUtil;
import com.geoffgranum.plugin.builder.domain.GenerateBuilderDirective;
import com.geoffgranum.plugin.builder.domain.PreferencesState;
import com.geoffgranum.plugin.builder.generate.BuilderClassGenerator;
import com.geoffgranum.plugin.builder.generate.BuilderFieldGenerator;
import com.geoffgranum.plugin.builder.ui.Dialog;
import com.geoffgranum.plugin.builder.ui.ValueKeys;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GenerateBuilderHandler implements LanguageCodeInsightActionHandler {

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (!EditorModificationUtil.checkModificationAllowed(editor)) {
      return;
    }
    if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
      return;
    }
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

    BuilderManager manager = new BuilderManager(project, (PsiJavaFile) file);

    List<PsiFieldMember> fieldMembers = chooseFields(manager, project, propertiesComponent);
    if (fieldMembers.isEmpty()) {
      return;
    }

    ApplicationManager.getApplication()
      .runWriteAction(new MakeBuilderRunnable(project, file, fieldMembers, propertiesComponent));
  }

  /**
   *
   */
  @NotNull
  private static List<PsiFieldMember> chooseFields(BuilderManager manager,
                                                   Project project,
                                                   final PropertiesComponent propertiesComponent) {
    List<PsiFieldMember> members = manager.instanceFields;
    List<PsiFieldMember> selectedFields = Lists.newArrayList();
    String stateJson = propertiesComponent.getValue(ValueKeys.USER_PREFERENCES_KEY, "{}");
    PreferencesState state = PreferencesState.fromJson(stateJson).build();
    Dialog d = new Dialog(state, propertiesComponent);

    if (!members.isEmpty() && !ApplicationManager.getApplication().isUnitTestMode()) {
      selectedFields.addAll(d.getSelectedFieldsFromDialog(project, members));
    }
    return selectedFields;
  }

  @Override
  public boolean isValidFor(Editor editor, PsiFile file) {
    return file instanceof PsiJavaFile
           && OverrideImplementUtil.getContextClass(editor.getProject(), editor, file, false) != null
           && new BuilderManager(editor.getProject(), (PsiJavaFile) file).supportsBuilder();
  }


  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private static class MakeBuilderRunnable implements Runnable {


    private final List<PsiFieldMember> fieldMembers;

    private final PropertiesComponent propertiesComponent;

    private final BuilderManager manager;

    private final BuilderPsiClassUtil classUtil;

    PsiElementFactory psiElementFactory;

    MakeBuilderRunnable(Project project,
                        PsiFile file,
                        List<PsiFieldMember> fieldMembers,
                        PropertiesComponent propertiesComponent) {

      this.manager = new BuilderManager(project, (PsiJavaFile) file);
      this.classUtil = this.manager.classUtil;
      this.fieldMembers = fieldMembers;
      this.propertiesComponent = propertiesComponent;

      psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    @Override
    public void run() {
      // Pull values out of state to run - the dialog box sets state on 'ok' click.
      String stateJson = propertiesComponent.getValue(ValueKeys.USER_PREFERENCES_KEY);
      PreferencesState state = PreferencesState.fromJson(stateJson).build();
      PsiClass clazz = manager.instanceClass;

      if (clazz != null) {
        List<BuilderFieldGenerator> bFields = classUtil.createBuilderFieldGenerators(fieldMembers, psiElementFactory);
        GenerateBuilderDirective directive = new GenerateBuilderDirective.Builder().containerClass(clazz)
          .fields(bFields)
          .generateJsonAnnotation(state.generateJsonAnnotations)
          .generateToJsonMethod(state.generateToJsonMethod)
          .generateFromJsonMethod(state.generateFromJsonMethod)
          .implementValidated(state.implementValidated)
          .copyFieldAnnotations(state.copyFieldAnnotations)
          .generateExampleCodeComment(state.generateExampleCodeComment)
          .generateCopyMethod(state.generateCopyMethod)
          .usePrefixWith(state.useWithPrefix)
          .useSpork(state.useSpork)
          .build();
        if (clazz.getModifierList() != null) {
          clazz.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
        }

        if (directive.generateToJsonMethod) {
          this.makeToJsonMethod(clazz, psiElementFactory, directive);
        }

        if (directive.generateFromJsonMethod) {
          this.makeFromJsonMethod(clazz, psiElementFactory, directive);
        }

        new BuilderClassGenerator(directive).makeSelf(psiElementFactory);
      }
    }

    private void makeFromJsonMethod(PsiClass targetClass,
                                    PsiElementFactory psiElementFactory,
                                    GenerateBuilderDirective state) {

      String fmt = "public static %1$s fromJson(com.fasterxml.jackson.databind.ObjectMapper mapper, String json) {\n"
                   + "    try {\n"
                   + "      return mapper.readValue(json, %1$s.class);\n"
                   + "    } catch (java.io.IOException e){\n"
                   + "      throw new "
                   + state.fromJsonExceptionClass()
                   + "(e, \"Could not create instance from provided JSON.\\n\\n %%s \\n\\n\", json);\n"
                   + "    }\n"
                   + "  }\n";


      String methodText = String.format(fmt, targetClass.getName());

      TypeGenerationUtil.addMethod(psiElementFactory, targetClass, null, methodText, true);
    }

    private void makeToJsonMethod(PsiClass targetClass,
                                  PsiElementFactory psiElementFactory,
                                  GenerateBuilderDirective state) {

      String fmt = "public java.lang.String toJson(com.fasterxml.jackson.databind.ObjectMapper mapper) {\n"
                   + "    try {\n"
                   + "      return mapper.writeValueAsString(this);\n"
                   + "    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {\n"
                   + "      throw new "
                   + state.toJsonExceptionClass()
                   + "(e, \"Could not write %1$s as Json\");\n"
                   + "    }\n"
                   + "  }\n";


      String methodText = String.format(fmt, targetClass.getName());

      TypeGenerationUtil.addMethod(psiElementFactory, targetClass, null, methodText, true);
    }

  }
}

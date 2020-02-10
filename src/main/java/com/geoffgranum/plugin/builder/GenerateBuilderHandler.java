package com.geoffgranum.plugin.builder;

import com.geoffgranum.plugin.builder.domain.PreferencesState;
import com.geoffgranum.plugin.builder.generate.BuilderClassGenerator;
import com.geoffgranum.plugin.builder.generate.BuilderFieldGenerator;
import com.geoffgranum.plugin.builder.info.FieldInfo;
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
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
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
    List<PsiFieldMember> fieldMembers = chooseFields(file, editor, project, propertiesComponent);
    if (fieldMembers.isEmpty()) {
      return;
    }

    ApplicationManager.getApplication()
      .runWriteAction(new MakeBuilderRunnable(project, file, editor, fieldMembers, propertiesComponent));
  }

  /**
   *
   */
  @NotNull
  private static List<PsiFieldMember> chooseFields(PsiFile file,
                                                   Editor editor,
                                                   Project project,
                                                   final PropertiesComponent propertiesComponent) {
    List<PsiFieldMember> members = getFields(file, editor);
    List<PsiFieldMember> selectedFields = Lists.newArrayList();
    String stateJson = propertiesComponent.getValue(ValueKeys.USER_PREFERENCES_KEY, "{}");
    PreferencesState state = PreferencesState.fromJson(stateJson).build();
    Dialog d = new Dialog(state, propertiesComponent);

    if (!members.isEmpty() && !ApplicationManager.getApplication().isUnitTestMode()) {
      selectedFields.addAll(d.getSelectedFieldsFromDialog(project, members));
    }
    return selectedFields;
  }

  /**
   * Get the list of fields to create builder methods for; this will be displayed to the user for
   * filtering/selection.
   */
  @NotNull
  private static List<PsiFieldMember> getFields(PsiFile file, Editor editor) {
    List<PsiFieldMember> result = Lists.newArrayList();
    int offset = editor.getCaretModel().getOffset();
    PsiElement generateBuilderFor = file.findElementAt(offset);
    if (generateBuilderFor != null) {
      PsiClass clazz = PsiTreeUtil.getParentOfType(generateBuilderFor, PsiClass.class);
      PsiClass classToExtractFieldsFrom = clazz;
      while (classToExtractFieldsFrom != null) {
        result.addAll(0, collectFieldsInClass(generateBuilderFor, clazz, classToExtractFieldsFrom));
        if (classToExtractFieldsFrom.hasModifierProperty(PsiModifier.STATIC)) {
          break;
        }
        classToExtractFieldsFrom = classToExtractFieldsFrom.getSuperClass();
      }
    }

    return result;
  }

  private static List<PsiFieldMember> collectFieldsInClass(PsiElement element,
                                                           PsiClass accessObjectClass,
                                                           PsiClass clazz) {
    List<PsiFieldMember> classFieldMembers = Lists.newArrayList();
    for (PsiField field : clazz.getFields()) {
      // check access to the field from the builder container class (eg. private superclass fields)
      if (fieldIsAccessibleFromBuilder(element, accessObjectClass, clazz, field) && !ignoringField(accessObjectClass,
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

  /**
   * Ignore any non-final fields.
   * Ignore final fields that are already initialized in the declaration.
   * Ignore final fields that are on super classes, unless the superclass is abstract.
   * Ignore static fields
   * Ignore any all-uppercase fields
   * Ignore Logging fields
   */
  private static boolean ignoringField(PsiClass accessObjectClass, PsiElement clazz, PsiField field) {
    return field.hasModifierProperty(PsiModifier.STATIC)
           || !field.hasModifierProperty(PsiModifier.FINAL)
           || field.getName() == null
           || isAllUpperCase(field.getName())
           || isLoggingField(field)
           || isIgnoredFinalField(accessObjectClass, clazz, field);
  }

  private static boolean isLoggingField(PsiField field) {
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

  private static boolean isIgnoredFinalField(PsiClass accessObjectClass, PsiElement clazz, PsiField field) {
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
  private static boolean isAllUpperCase(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLowerCase(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean fieldIsAccessibleFromBuilder(PsiElement element,
                                                      PsiClass accessObjectClass,
                                                      PsiClass clazz,
                                                      PsiField field) {
    PsiResolveHelper helper = JavaPsiFacade.getInstance(clazz.getProject()).getResolveHelper();

    return helper.isAccessible(field, accessObjectClass, clazz) && !PsiTreeUtil.isAncestor(field, element, false);
  }

  @Override
  public boolean isValidFor(Editor editor, PsiFile file) {
    return file instanceof PsiJavaFile
           && OverrideImplementUtil.getContextClass(editor.getProject(), editor, file, false) != null
           && isApplicable(file, editor);
  }

  /**
   * Check the current Intellij scope for any fields, regardless of access level, scope, etc.
   */
  private static boolean isApplicable(PsiFile file, Editor editor) {
    List<PsiFieldMember> targetElements = getFields(file, editor);
    return !targetElements.isEmpty();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private static class MakeBuilderRunnable implements Runnable {

    private final PsiFile file;

    private final Editor editor;

    private final List<PsiFieldMember> fieldMembers;

    private final PropertiesComponent propertiesComponent;

    CodeStyleManager codeStyleManager;

    PsiElementFactory psiElementFactory;

    MakeBuilderRunnable(Project project,
                        PsiFile file,
                        Editor editor,
                        List<PsiFieldMember> fieldMembers,
                        PropertiesComponent propertiesComponent) {
      this.file = file;
      this.editor = editor;
      this.fieldMembers = fieldMembers;
      this.propertiesComponent = propertiesComponent;

      psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    @Override
    public void run() {
      // Pull values out of state to run - the dialog box sets state on 'ok' click.
      String stateJson = propertiesComponent.getValue(ValueKeys.USER_PREFERENCES_KEY);
      PreferencesState state = PreferencesState.fromJson(stateJson).build();

      PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
      PsiClass clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);

      if (clazz != null) {

        List<BuilderFieldGenerator> bFields = createBuilderFieldGenerators();
        GenerateBuilderDirective builderDirective = new GenerateBuilderDirective.Builder().containerClass(clazz)
          .fields(bFields)
          .generateJsonAnnotation(state.generateJsonAnnotations)
          .generateToJsonMethod(state.generateToJsonMethod)
          .generateFromJsonMethod(state.generateFromJsonMethod)
          .implementValidated(state.implementValidated)
          .copyFieldAnnotations(state.copyFieldAnnotations)
          .generateExampleCodeComment(state.generateExampleCodeComment)
          .generateCopyMethod(state.generateCopyMethod)
          .usePrefixWith(state.useWithPrefix)
          .build();
        if (clazz.getModifierList() != null) {
          clazz.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
        }

        if (builderDirective.generateToJsonMethod) {
          this.makeToJsonMethod(clazz, psiElementFactory);
        }

        if (builderDirective.generateFromJsonMethod) {
          this.makeFromJsonMethod(clazz, psiElementFactory);
        }

        new BuilderClassGenerator(builderDirective).makeSelf(psiElementFactory);
      }
    }

    private void makeFromJsonMethod(PsiClass targetClass, PsiElementFactory psiElementFactory) {

      String fmt = "public static %1$s fromJson(com.fasterxml.jackson.databind.ObjectMapper mapper, String json) {\n"
                   + "    try {\n"
                   + "      return mapper.readValue(json, %1$s.class);\n"
                   + "    } catch (java.io.IOException e){\n"
                   + "      throw new com.geoffgranum.uttu.core.exception.FormattedException(e, \"Could not create instance from provided JSON.\\n\\n %%s \\n\\n\", json);\n"
                   + "    }\n"
                   + "  }\n";


      String methodText = String.format(fmt, targetClass.getName());

      TypeGenerationUtil.addMethod(targetClass, null, methodText, true, psiElementFactory);
    }

    private void makeToJsonMethod(PsiClass targetClass, PsiElementFactory psiElementFactory) {

      String fmt = "public java.lang.String toJson(com.fasterxml.jackson.databind.ObjectMapper mapper) {\n"
                   + "    try {\n"
                   + "      return mapper.writeValueAsString(this);\n"
                   + "    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {\n"
                   + "      throw new com.geoffgranum.uttu.core.exception.FormattedException(e, \"Could not write %1$s as Json\");\n"
                   + "    }\n"
                   + "  }\n";


      String methodText = String.format(fmt, targetClass.getName());

      TypeGenerationUtil.addMethod(targetClass, null, methodText, true, psiElementFactory);
    }

    @NotNull
    private List<BuilderFieldGenerator> createBuilderFieldGenerators() {
      List<BuilderFieldGenerator> bFields = Lists.newArrayListWithCapacity(fieldMembers.size());
      InterestingTypes interestingTypes = new InterestingTypes(psiElementFactory);
      for (PsiFieldMember member : fieldMembers) {
        bFields.add(new BuilderFieldGenerator(FieldInfo.from(member.getElement(), interestingTypes)));
      }
      return bFields;
    }
  }
}

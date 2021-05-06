package com.geoffgranum.plugin.builder.action;

import com.geoffgranum.plugin.builder.domain.BuilderManager;
import com.geoffgranum.plugin.builder.domain.PreferencesState;
import com.geoffgranum.plugin.builder.generate.CopyMethodCreator;
import com.geoffgranum.plugin.builder.info.BuilderInfo;
import com.geoffgranum.plugin.builder.ui.ValueKeys;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import org.jetbrains.annotations.NotNull;

public class AddCopyMethodHandler implements LanguageCodeInsightActionHandler {


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
    // List<PsiFieldMember> fieldMembers = chooseFields(file, editor, project, propertiesComponent);
    // if (fieldMembers.isEmpty()) {
    //   return;
    // }

    // Cast is safe, as we tested for same in the 'isValid()'
    ApplicationManager.getApplication()
      .runWriteAction(new MakeCopyMethodRunnable(project, (PsiJavaFileImpl) file, editor, propertiesComponent));
  }

  @Override
  public boolean isValidFor(Editor editor, PsiFile file) {
    return file instanceof PsiJavaFile
           && OverrideImplementUtil.getContextClass(editor.getProject(), editor, file, false) != null
           && new BuilderManager(editor.getProject(), (PsiJavaFile) file).hasBuilder();
  }


  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private static class MakeCopyMethodRunnable implements Runnable {

    private final Project project;

    private final PsiJavaFileImpl file;

    private final Editor editor;

    private final PropertiesComponent propertiesComponent;

    CodeStyleManager codeStyleManager;

    PsiElementFactory psiElementFactory;

    MakeCopyMethodRunnable(Project project,
                           PsiJavaFileImpl file,
                           Editor editor,
                           PropertiesComponent propertiesComponent) {
      this.project = project;
      this.file = file;
      this.editor = editor;
      this.propertiesComponent = propertiesComponent;

      psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    @Override
    public void run() {
      // Pull values out of state to run - the dialog box sets state on 'ok' click.
      String stateJson = propertiesComponent.getValue(ValueKeys.USER_PREFERENCES_KEY);
      PreferencesState state = PreferencesState.fromJson(stateJson).build();

      BuilderManager manager = new BuilderManager(project, file);
      BuilderInfo builderInfo = manager.extractBuilderInfo();

      new CopyMethodCreator().create(psiElementFactory, manager.builderClass(), builderInfo.fields);
    }


  }
}

package com.geoffgranum.plugin.builder.action;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class AddCopyMethodAction extends BaseCodeInsightAction {

  private final AddCopyMethodHandler handler = new AddCopyMethodHandler();

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return handler;
  }

  @Override
  protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull final PsiFile file) {
    return handler.isValidFor(editor, file);
  }
}

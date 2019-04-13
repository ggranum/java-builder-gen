package com.geoffgranum.plugin.builder.ui;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.NonFocusableCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author ggranum
 */
public class ChooserCreation {


  public static List<PsiFieldMember> getSelectedFieldsFromDialog(Project project,
                                                                 final PropertiesComponent propertiesComponent,
                                                                 List<PsiFieldMember> members) {
    PsiFieldMember[] memberArray = members.toArray(new PsiFieldMember[0]);
    JCheckBox[] checkOptions = new JCheckBox[ChooserOption.options.length];
    for (int i = 0; i < ChooserOption.options.length; i++) {
      ChooserOption option = ChooserOption.options[i];
      checkOptions[i] = createCheckbox(propertiesComponent, option);
    }

    MemberChooser<PsiFieldMember> chooser = new MemberChooser<>(memberArray, false, true, project, null, checkOptions);

    chooser.setTitle("Select Fields to Include in Builder");
    chooser.selectElements(memberArray);
    chooser.show();

    List<PsiFieldMember> selectedElements;

    if (chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      selectedElements = Collections.emptyList();
    } else {
      selectedElements = chooser.getSelectedElements();
    }
    return selectedElements;
  }

  @NotNull
  private static JCheckBox createCheckbox(PropertiesComponent propertiesComponent, ChooserOption option) {
    final JCheckBox withImplementValidated = new NonFocusableCheckBox(option.title);
    withImplementValidated.setMnemonic(option.mnemonic);
    withImplementValidated.setToolTipText(option.description);
    withImplementValidated.setSelected(propertiesComponent.isTrueValue(option.key));
    withImplementValidated.addItemListener(e -> propertiesComponent.setValue(option.key,
                                                                             Boolean.toString(withImplementValidated.isSelected())));
    return withImplementValidated;
  }
}

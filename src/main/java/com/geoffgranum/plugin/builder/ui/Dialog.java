package com.geoffgranum.plugin.builder.ui;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.NonFocusableCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author ggranum
 */
public class Dialog {

  public static final DialogOption GENERATE_JSON_ANNOTATIONS = new DialogOption("Enable Jackson marshaling for class.",
    "Annotate the class and Builder fields with the Jackson Annotations required for marshaling/unmarshalling the class.",
    'm',
    ValueKeys.GENERATE_JSON_ANNOTATIONS);

  public static final DialogOption GENERATE_TO_FROM_JSON_METHOD = new DialogOption("Generate toJson and fromJson methods",
    "Implies Enable Jackson marshaling. Generates a static fromJson(ObjectMapper mapper, String json) and a toJson(ObjectMapper mapper) method.",
    'f',
    ValueKeys.GENERATE_TO_FROM_JSON_METHOD);

  public static final DialogOption COPY_FIELD_ANNOTATIONS = new DialogOption("Copy annotations into builder.",
    "Copy all annotations off of model fields and onto builder fields.",
    'a',
    ValueKeys.COPY_FIELD_ANNOTATIONS);

  public static final DialogOption IMPLEMENT_VALIDATED = new DialogOption("Implement Validated",
    "Add Hibernate validations stubs to builder fields and implement the Validated class. Requires JavaSpork, see Plugin readme.",
    'v',
    ValueKeys.IMPLEMENT_VALIDATED);

  public static final DialogOption GENERATE_COPY_METHOD = new DialogOption("Generate Copy Method",
    "Add a copy method: 'public static ClassNameBuilder ClassName.from(ClassName source)",
    'c',
    ValueKeys.ADD_COPY_METHOD);

  public static final DialogOption GENERATE_EXAMPLE_CODE_COMMENT = new DialogOption("Provide Example Comment",
    "Adds an example use of the builder to the constructor comment.",
    'e',
    ValueKeys.ADD_EXAMPLE_CODE_COMMENT);

  public static final DialogOption USE_WITH_PREFIX = new DialogOption("Use annoying 'with' prefix on builder methods.",
    "Prefix every single builder method with the completely redundant and annoyingly verbose 'with' syntax.",
    'w',
    ValueKeys.USE_WITH_PREFIX);

  private final PropertiesComponent propertiesComponent;

  private JCheckBox cbcopyFieldAnnotations;

  private JCheckBox cbGenerateJsonAnnotations;

  private JCheckBox cbGenerateFromJsonMethod;

  private JCheckBox cbImplementValidated;

  private JCheckBox cbGenerateCopyMethod;

  private JCheckBox cbUseWith;

  private JCheckBox cbGenerateExampleCodeComment;


  public Dialog(PropertiesComponent propertiesComponent) {
    this.propertiesComponent = propertiesComponent;
  }

  public List<PsiFieldMember> getSelectedFieldsFromDialog(Project project, List<PsiFieldMember> members) {
    PsiFieldMember[] memberArray = members.toArray(new PsiFieldMember[0]);
    List<JComponent> components = new ArrayList<>();

    this.cbcopyFieldAnnotations = createCheckbox(COPY_FIELD_ANNOTATIONS);
    this.cbGenerateJsonAnnotations = createCheckbox(GENERATE_JSON_ANNOTATIONS);
    this.cbGenerateFromJsonMethod = createCheckbox(GENERATE_TO_FROM_JSON_METHOD);
    this.cbImplementValidated = createCheckbox(IMPLEMENT_VALIDATED);
    this.cbGenerateCopyMethod = createCheckbox(GENERATE_COPY_METHOD);
    this.cbGenerateExampleCodeComment = createCheckbox(GENERATE_EXAMPLE_CODE_COMMENT);
    this.cbUseWith = createCheckbox(USE_WITH_PREFIX);

    components.add(cbcopyFieldAnnotations);
    components.add(cbGenerateJsonAnnotations);
    components.add(cbGenerateFromJsonMethod);
    components.add(cbImplementValidated);
    components.add(cbGenerateCopyMethod);
    components.add(cbGenerateExampleCodeComment);
    components.add(cbUseWith);

    MemberChooser<PsiFieldMember> chooser =
      new MemberChooser<>(memberArray, false, true, project, null, components.toArray(new JComponent[0]));

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
  private JCheckBox createCheckbox(DialogOption option) {
    final JCheckBox check = new NonFocusableCheckBox(option.title);
    check.setMnemonic(option.mnemonic);
    check.setToolTipText(option.description);
    check.setSelected(propertiesComponent.isTrueValue(option.key));
    check.addItemListener(e -> propertiesComponent.setValue(option.key, Boolean.toString(check.isSelected())));
    return check;
  }
}

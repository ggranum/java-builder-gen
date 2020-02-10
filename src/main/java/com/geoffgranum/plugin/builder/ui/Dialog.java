package com.geoffgranum.plugin.builder.ui;

import com.geoffgranum.plugin.builder.domain.PreferencesState;
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

  public static final DialogOption GENERATE_JSON_ANNOTATIONS = new DialogOption("Enable Jackson marshaling for class",
    "Annotate the class and Builder fields with the Jackson Annotations required for marshaling/unmarshalling the class.",
    'm');

  public static final DialogOption GENERATE_TO_JSON_METHOD = new DialogOption("Generate toJson method",
    "Implies Enable Jackson marshaling. Generates a static fromJson(ObjectMapper mapper, String json) and a toJson(ObjectMapper mapper) method.",
    't');

  public static final DialogOption GENERATE_FROM_JSON_METHOD = new DialogOption("Generate fromJson method",
    "Implies Enable Jackson marshaling. Generates a static fromJson(ObjectMapper mapper, String json) and a toJson(ObjectMapper mapper) method.",
    'f');

  public static final DialogOption COPY_FIELD_ANNOTATIONS = new DialogOption("Copy annotations into builder",
    "Copy all annotations off of model fields and onto builder fields.",
    'a');

  public static final DialogOption IMPLEMENT_VALIDATED = new DialogOption("Implement Validated",
    "Add Hibernate validations stubs to builder fields and implement the Validated class. Requires Uttu, see Plugin readme.",
    'v');

  public static final DialogOption GENERATE_COPY_METHOD = new DialogOption("Generate Copy Method",
    "Add a copy method: 'public static ClassNameBuilder ClassName.from(ClassName source)",
    'c');

  public static final DialogOption GENERATE_EXAMPLE_CODE_COMMENT =
    new DialogOption("Provide Example Comment", "Adds an example use of the builder to the constructor comment.", 'e');

  public static final DialogOption USE_WITH_PREFIX = new DialogOption("Use annoying 'with' prefix on builder methods.",
    "Prefix every single builder method with the completely redundant and annoyingly verbose 'with' syntax.",
    'w');

  public static final DialogOption USE_SPORK = new DialogOption("[Deprecated] Use Spork Library (instead of Uttu).",
    "For to/From json and Validated, use the older Spork imports instead of the newer Uttu project.",
    'x');

  private final PropertiesComponent propertiesComponent;

  private final PreferencesState.Builder newState;

  private final PreferencesState previousState;

  private JCheckBox copyFieldAnnotationsCb;

  private JCheckBox generateJsonAnnotationsCb;

  private JCheckBox generateToJsonMethodCb;

  private JCheckBox generateFromJsonMethodCb;

  private JCheckBox implementValidatedCb;

  private JCheckBox generateCopyMethodCb;

  private JCheckBox useWithPrefixCb;

  private JCheckBox useSporkCb;

  private JCheckBox generateExampleCodeCommentCb;

  private MemberChooser<PsiFieldMember> chooser;

  public Dialog(PreferencesState state, PropertiesComponent propertiesComponent) {
    this.previousState = state;
    this.newState = new PreferencesState.Builder().from(state);
    this.propertiesComponent = propertiesComponent;
  }

  public List<PsiFieldMember> getSelectedFieldsFromDialog(Project project, List<PsiFieldMember> members) {
    PsiFieldMember[] memberArray = members.toArray(new PsiFieldMember[0]);
    List<JComponent> components = new ArrayList<>();

    copyFieldAnnotationsCb = createCopyFieldAnnotations();
    components.add(copyFieldAnnotationsCb);

    generateJsonAnnotationsCb = createGenerateJsonAnnotationsCb();
    components.add(generateJsonAnnotationsCb);


    generateToJsonMethodCb = createGenerateToJsonMethodCb();
    components.add(generateToJsonMethodCb);

    generateFromJsonMethodCb = createGenerateFromJsonMethodCb();
    components.add(generateFromJsonMethodCb);

    implementValidatedCb = createImplementValidatedCb();
    components.add(implementValidatedCb);

    generateCopyMethodCb = createGenerateCopyMethodCb();
    components.add(generateCopyMethodCb);

    generateExampleCodeCommentCb = createGenerateExampleCodeCommentCb();
    components.add(generateExampleCodeCommentCb);

    useWithPrefixCb = createUseWithPrefixCb();
    components.add(useWithPrefixCb);

    useSporkCb = createUseSporkCb();
    components.add(useSporkCb);

    chooser = new MemberChooser<>(memberArray, false, true, project, null, components.toArray(new JComponent[0]));

    chooser.setTitle("Select Fields to Include in Builder");
    chooser.selectElements(memberArray);
    chooser.show();

    List<PsiFieldMember> selectedElements;

    if (chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      selectedElements = Collections.emptyList();
    } else {
      // Save values to JetBrains preferences
      String json = newState.build().toJson();
      propertiesComponent.setValue(ValueKeys.USER_PREFERENCES_KEY, json);
      selectedElements = chooser.getSelectedElements();
    }
    return selectedElements;
  }

  @NotNull
  private JCheckBox createCopyFieldAnnotations() {
    JCheckBox cb = createCheckbox(COPY_FIELD_ANNOTATIONS, previousState.copyFieldAnnotations);
    cb.addItemListener((e -> newState.copyFieldAnnotations(cb.isSelected())));
    return cb;
  }

  @NotNull
  private JCheckBox createCheckbox(DialogOption option, boolean value) {
    final JCheckBox check = new NonFocusableCheckBox(option.title);
    check.setMnemonic(option.mnemonic);
    check.setToolTipText(option.description);
    check.setSelected(value);
    return check;
  }

  @NotNull
  private JCheckBox createGenerateJsonAnnotationsCb() {
    JCheckBox cb = createCheckbox(GENERATE_JSON_ANNOTATIONS, previousState.generateJsonAnnotations);
    cb.addItemListener((e -> {
      boolean checked = cb.isSelected();
      newState.generateJsonAnnotations(cb.isSelected());
      generateToJsonMethodCb.setEnabled(checked);
      generateFromJsonMethodCb.setEnabled(checked);

      if (!checked) {
        // Disable to/From Json methods if annotations are disabled.
        newState.generateToJsonMethod(false);
        newState.generateFromJsonMethod(false);
        generateToJsonMethodCb.setSelected(false);
        generateFromJsonMethodCb.setSelected(false);
      }
    }));
    return cb;
  }

  @NotNull
  private JCheckBox createGenerateToJsonMethodCb() {
    JCheckBox cb = createCheckbox(GENERATE_TO_JSON_METHOD, previousState.generateToJsonMethod);
    cb.setEnabled(previousState.generateJsonAnnotations);
    cb.addItemListener((e -> {
      boolean checked = cb.isSelected();
      newState.generateToJsonMethod(checked);
    }));
    return cb;
  }

  @NotNull
  private JCheckBox createGenerateFromJsonMethodCb() {
    JCheckBox cb = createCheckbox(GENERATE_FROM_JSON_METHOD, previousState.generateFromJsonMethod);
    cb.setEnabled(previousState.generateJsonAnnotations);
    cb.addItemListener((e -> {
      boolean checked = cb.isSelected();
      newState.generateFromJsonMethod(checked);
    }));
    return cb;
  }

  @NotNull
  private JCheckBox createImplementValidatedCb() {
    JCheckBox cb = createCheckbox(IMPLEMENT_VALIDATED, previousState.implementValidated);
    cb.addItemListener((e -> newState.implementValidated(cb.isSelected())));
    return cb;
  }

  @NotNull
  private JCheckBox createGenerateCopyMethodCb() {
    JCheckBox cb = createCheckbox(GENERATE_COPY_METHOD, previousState.generateCopyMethod);
    cb.addItemListener((e -> newState.generateCopyMethod(cb.isSelected())));
    return cb;
  }

  @NotNull
  private JCheckBox createGenerateExampleCodeCommentCb() {
    JCheckBox cb = createCheckbox(GENERATE_EXAMPLE_CODE_COMMENT, previousState.generateExampleCodeComment);
    cb.addItemListener((e -> newState.generateExampleCodeComment(cb.isSelected())));
    return cb;
  }

  @NotNull
  private JCheckBox createUseWithPrefixCb() {
    JCheckBox cb = createCheckbox(USE_WITH_PREFIX, previousState.useWithPrefix);
    cb.addItemListener((e -> newState.useWithPrefix(cb.isSelected())));
    return cb;
  }

  @NotNull
  private JCheckBox createUseSporkCb() {
    JCheckBox cb = createCheckbox(USE_SPORK, previousState.useSpork);
    cb.addItemListener((e -> newState.useSpork(cb.isSelected())));
    return cb;
  }
}

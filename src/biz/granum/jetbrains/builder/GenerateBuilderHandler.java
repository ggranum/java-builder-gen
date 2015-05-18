package biz.granum.jetbrains.builder;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiResolveHelper;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.ui.NonFocusableCheckBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.List;
import javax.swing.JCheckBox;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class GenerateBuilderHandler implements LanguageCodeInsightActionHandler {

  @NonNls
  private static final String PROP_NEW_BUILDER_METHOD = "biz.granum.jetbrains.BuilderGen.jsonAnnotations";

  @NonNls
  private static final String IMPLEMENT_VALIDATED = "biz.granum.jetbrains.BuilderGen.implementValidated";

  @NonNls
  private static final String GENERATE_JSON_ANNOTATIONS = "biz.granum.jetbrains.BuilderGen.jsonAnnotations";

  @Override
  public boolean isValidFor(Editor editor, PsiFile file) {
    return file instanceof PsiJavaFile
           && OverrideImplementUtil.getContextClass(editor.getProject(), editor, file, false) != null
           && isApplicable(file, editor);
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if(!CodeInsightUtilBase.prepareEditorForWrite(editor)) {
      return;
    }
    if(!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
      return;
    }
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
    List<PsiFieldMember> fieldMembers = chooseFields(file, editor, project, propertiesComponent);
    if(fieldMembers.isEmpty()) {
      return;
    }

    ApplicationManager.getApplication().runWriteAction(new MakeBuilderRunnable(project,
                                                                               file,
                                                                               editor,
                                                                               fieldMembers,
                                                                               propertiesComponent));
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  /**
   * Check the current Intellij scope for any fields, regardless of access level, scope, etc.
   */
  public static boolean isApplicable(PsiFile file, Editor editor) {
    List<PsiFieldMember> targetElements = getFields(file, editor);
    return !targetElements.isEmpty();
  }

  /**

   */
  @NotNull
  private static List<PsiFieldMember> chooseFields(
      PsiFile file,
      Editor editor,
      Project project,
      final PropertiesComponent propertiesComponent) {
    List<PsiFieldMember> members = getFields(file, editor);
    List<PsiFieldMember> selectedFields = Lists.newArrayList();
    if(!members.isEmpty() && !ApplicationManager.getApplication().isUnitTestMode()) {
      selectedFields.addAll(getSelectedFieldsFromDialog(project, propertiesComponent, members));
    }
    return selectedFields;
  }

  private static List<PsiFieldMember> getSelectedFieldsFromDialog(
      Project project,
      final PropertiesComponent propertiesComponent,
      List<PsiFieldMember> members) {

    final JCheckBox withImplementValidated = new NonFocusableCheckBox("Implement Validated");
    withImplementValidated.setMnemonic('v');
    withImplementValidated.setToolTipText("Add Hibernate validations stubs to builder fields and implement the Validated class.");
    withImplementValidated.setSelected(propertiesComponent.isTrueValue(IMPLEMENT_VALIDATED));
    withImplementValidated.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        propertiesComponent.setValue(IMPLEMENT_VALIDATED, Boolean.toString(withImplementValidated.isSelected()));
      }
    });

    final JCheckBox withJackson = new NonFocusableCheckBox("Enable Jackson marshaling for class.");
    withJackson.setMnemonic('w');
    withJackson.setToolTipText(
        "Annotate the class and Builder fields with the Jackson Annotations required for marshaling/unmarshalling the class.");
    withJackson.setSelected(propertiesComponent.isTrueValue(GENERATE_JSON_ANNOTATIONS));
    withJackson.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        propertiesComponent.setValue(GENERATE_JSON_ANNOTATIONS, Boolean.toString(withJackson.isSelected()));
      }
    });

    PsiFieldMember[] memberArray = members.toArray(new PsiFieldMember[members.size()]);

    MemberChooser<PsiFieldMember> chooser = new MemberChooser<PsiFieldMember>(memberArray, false, true, project, null,
                                                                              new JCheckBox[]{
                                                                                  withImplementValidated,
                                                                                  withJackson
                                                                              });

    chooser.setTitle("Select Fields to Include in Builder");
    chooser.selectElements(memberArray);
    chooser.show();

    List<PsiFieldMember> selectedElements;

    if(chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      selectedElements = Collections.emptyList();
    } else {
      selectedElements = chooser.getSelectedElements();
    }
    return selectedElements;
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
    if(generateBuilderFor != null) {
      PsiClass clazz = PsiTreeUtil.getParentOfType(generateBuilderFor, PsiClass.class);
      PsiClass classToExtractFieldsFrom = clazz;
      while (classToExtractFieldsFrom != null) {
        result.addAll(0, collectFieldsInClass(generateBuilderFor, clazz, classToExtractFieldsFrom));
        if(classToExtractFieldsFrom.hasModifierProperty(PsiModifier.STATIC)) {
          break;
        }
        classToExtractFieldsFrom = classToExtractFieldsFrom.getSuperClass();
      }
    }

    return result;
  }

  private static List<PsiFieldMember> collectFieldsInClass(
      PsiElement element,
      PsiClass accessObjectClass,
      PsiClass clazz) {
    List<PsiFieldMember> classFieldMembers = Lists.newArrayList();
    for (PsiField field : clazz.getFields()) {
      // check access to the field from the builder container class (eg. private superclass fields)
      if(fieldIsAccessibleFromBuilder(element, accessObjectClass, clazz, field)
         && !ignoringField(accessObjectClass, clazz, field)) {
        PsiClass containingClass = field.getContainingClass();
        if(containingClass != null) {
          classFieldMembers.add(
              new PsiFieldMember(field,
                                 TypeConversionUtil.getSuperClassSubstitutor(containingClass,
                                                                             clazz,
                                                                             PsiSubstitutor.EMPTY)));
        }
      }
    }

    return classFieldMembers;
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
           || isAllUpperCase(field.getName())
           || isLoggingField(field)
           || isIgnoredFinalField(accessObjectClass, clazz, field);
  }

  private static boolean isIgnoredFinalField(PsiClass accessObjectClass, PsiElement clazz, PsiField field) {
    boolean ignored = false;
    if(field.hasModifierProperty(PsiModifier.FINAL)) {
      if(field.getInitializer() != null) {
        ignored = true; // remove final fields that are assigned in the declaration
      }
      if(!accessObjectClass.isEquivalentTo(clazz) && !accessObjectClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
        ignored = true; // remove final superclass fields
      }
    }
    return ignored;
  }

  private static boolean fieldIsAccessibleFromBuilder(
      PsiElement element,
      PsiClass accessObjectClass,
      PsiClass clazz, PsiField field) {
    PsiResolveHelper helper = JavaPsiFacade.getInstance(clazz.getProject()).getResolveHelper();

    return helper.isAccessible(field, accessObjectClass, clazz) && !PsiTreeUtil.isAncestor(field, element, false);
  }

  /**
   * Does the string have a lowercase character?
   *
   * @param s the string to test.
   *
   * @return true if the string has a lowercase character, false if not.
   */
  private static boolean isAllUpperCase(String s) {
    for (int i = 0; i < s.length(); i++) {
      if(Character.isLowerCase(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static class MakeBuilderRunnable implements Runnable {

    private final PsiFile file;
    private final Editor editor;
    private final List<PsiFieldMember> fieldMembers;
    private final PropertiesComponent propertiesComponent;
    CodeStyleManager codeStyleManager;
    PsiElementFactory psiElementFactory;

    public MakeBuilderRunnable(
        Project project,
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
      boolean implementValidated = propertiesComponent.getBoolean(IMPLEMENT_VALIDATED, false);
      boolean implementJackson = propertiesComponent.getBoolean(GENERATE_JSON_ANNOTATIONS, false);

      PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
      PsiClass clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);

      BuilderClassGenerator builderMaker = new BuilderClassGenerator.Builder()
          .containerClass(clazz)
          .fields(fieldMembers)
          .implementJackson(implementJackson)
          .implementValidated(implementValidated)
          .build();
      builderMaker.makeSelf(psiElementFactory);



    }


  }
}

package com.geoffgranum.plugin.builder;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;

/**
 * @author Geoff M. Granum
 */
public class TypeGenerationUtil {

  // From PsiJavaParserFacadeImpl#createClassFromText, which only allows you to specify the *body*.
  private static final String DUMMY_FILE_NAME = "_Dummy_." + JavaFileType.INSTANCE.getDefaultExtension();

  @NonNls
  private static final String JAVA_DOT_LANG = "java.lang.";

  @NonNls
  public static final String BUILDER_CLASS_NAME = "Builder";

  private static PsiClass createClassFromText(String text, Project project) {
    final FileType type = JavaFileType.INSTANCE;
    PsiJavaFile aFile = (PsiJavaFile)PsiFileFactory.getInstance(project).createFileFromText(DUMMY_FILE_NAME, type, text);
    final PsiClass[] classes = aFile.getClasses();
    if(classes.length != 1) {
      throw new IncorrectOperationException("Incorrect class \"" + text + "\".");
    }
    return classes[0];
  }

  public static PsiClass createBuilderClass(PsiClass clazz, boolean implementValidated) {
    PsiClass builderClass;
    String builderClassBody;
    if(implementValidated) {
      builderClassBody = "public static class %s extends com.geoffgranum.uttu.core.validation.Validated {}";
    } else {
      builderClassBody = "public static class %s {}";
    }
    String body = String.format(builderClassBody, BUILDER_CLASS_NAME);
    builderClass = createClassFromText(body, clazz.getProject());

    // builder classes are static and final
    PsiModifierList modifierList = builderClass.getModifierList();
    if(modifierList == null) {
      throw new IllegalStateException("No modifierList on new freshly minted class.");
    }
    modifierList.setModifierProperty(PsiModifier.STATIC, true);
    modifierList.setModifierProperty(PsiModifier.FINAL, true);
    return builderClass;
  }

  public static PsiElement addAnnotation(PsiClass target, String annotationText, PsiElementFactory psiElementFactory) {

    PsiAnnotation annotation = psiElementFactory.createAnnotationFromText(annotationText, target);

    PsiModifierList modifierList = target.getModifierList();
    assert modifierList != null;
    PsiElement firstChild = modifierList.getFirstChild();
    return modifierList.addBefore(annotation, firstChild);
  }

  public static PsiField addField(PsiClass target,
                                  PsiElement after,
                                  String name,
                                  PsiType type,
                                  PsiElementFactory psiElementFactory,
                                  String... annotationsForField) {
    PsiField theField = target.findFieldByName(name, false);
    if(theField != null && !areTypesPresentableEqual(theField.getType(), type)) {
      theField.delete();
      theField = null;
    }
    if(theField == null) {

      PsiField newField = psiElementFactory.createField(name, type);

      if(after != null) {
        target.addAfter(newField, after);
      } else {
        target.add(newField);
      }
      theField = target.findFieldByName(name, false);
    }
    if(theField == null || theField.getModifierList() == null) {
      throw new IllegalStateException("Field does not exist or does not have a modifier list.");
    }

    PsiModifierList modifiers = theField.getModifierList();
    PsiElement firstChild = modifiers.getFirstChild();
    for (String annotationText : annotationsForField) {
      PsiAnnotation annotation = psiElementFactory.createAnnotationFromText(annotationText, theField);
      modifiers.addBefore(annotation, firstChild);
    }

    return theField;
  }

  public static PsiElement addMethod(PsiClass target,
                                     PsiElement after,
                                     String methodText,
                                     PsiElementFactory psiElementFactory) {
    return addMethod(target, after, methodText, false, psiElementFactory);
  }

  public static PsiElement addMethod(PsiClass target,
                                     PsiElement after,
                                     String methodText,
                                     boolean replace,
                                     PsiElementFactory psiElementFactory) {
    PsiMethod newMethod = psiElementFactory.createMethodFromText(methodText, null);
    PsiMethod theMethod = target.findMethodBySignature(newMethod, false);

    if(theMethod == null && newMethod.isConstructor()) {
      for (PsiMethod constructor : target.getConstructors()) {
        if(areParameterListsEqual(constructor.getParameterList(), newMethod.getParameterList())) {
          theMethod = constructor;
          break;
        }
      }
    }

    if(theMethod == null) {
      if(after != null) {
        target.addAfter(newMethod, after);
      } else {
        target.add(newMethod);
      }
    } else if(replace) {
      theMethod.replace(newMethod);
    }
    theMethod = target.findMethodBySignature(newMethod, false);
    return theMethod;
  }

  private static boolean areParameterListsEqual(PsiParameterList paramList1, PsiParameterList paramList2) {
    if(paramList1.getParametersCount() != paramList2.getParametersCount()) {
      return false;
    }

    PsiParameter[] param1Params = paramList1.getParameters();
    PsiParameter[] param2Params = paramList2.getParameters();
    for (int i = 0; i < param1Params.length; i++) {
      PsiParameter param1Param = param1Params[i];
      PsiParameter param2Param = param2Params[i];

      if(!areTypesPresentableEqual(param1Param.getType(), param2Param.getType())) {
        return false;
      }
    }
    return true;
  }

  private static boolean areTypesPresentableEqual(PsiType type1, PsiType type2) {
    if(type1 != null && type2 != null) {
      String type1Canonical = stripJavaLang(type1.getPresentableText());
      String type2Canonical = stripJavaLang(type2.getPresentableText());
      return type1Canonical.equals(type2Canonical);
    }
    return false;
  }

  private static String stripJavaLang(String typeString) {
    return typeString.startsWith(JAVA_DOT_LANG) ? typeString.substring(
                                                                          JAVA_DOT_LANG.length()) : typeString;
  }
}
 

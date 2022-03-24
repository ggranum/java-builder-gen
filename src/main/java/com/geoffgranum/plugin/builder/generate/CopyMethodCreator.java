package com.geoffgranum.plugin.builder.generate;

import com.geoffgranum.plugin.builder.TypeGenerationUtil;
import com.geoffgranum.plugin.builder.info.FieldInfo;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;

import java.util.List;

/**
 * @author ggranum
 */
public class CopyMethodCreator {

  public void create(PsiElementFactory psiElementFactory, PsiClass builderClass, List<FieldInfo> fields) {

    String fmt = "public %1$s copy(){\n" + "  %2$s" + "  return copy;" + "}";

    StringBuilder body = new StringBuilder("Builder copy = new Builder();\n");

    for (FieldInfo field : fields) {
      body.append(copyFieldStatement(field)).append('\n');
    }

    String methodText = String.format(fmt, builderClass.getName(), body.toString());

    PsiMethod[] buildMethods = builderClass.findMethodsByName("build", false);
    if (buildMethods.length > 0) {
      TypeGenerationUtil.addMethod(psiElementFactory, builderClass, buildMethods[0], methodText, true, true);
    } else {
      TypeGenerationUtil.addMethod(psiElementFactory, builderClass, methodText, true);
    }
  }


  /**
   * Returns a copy statement of the form "targetVariableName.fieldName(this.fieldName);"
   */
  private String copyFieldStatement(FieldInfo info) {
    String fmt = "%1$s.%2$s(this.%2$s);";
    return String.format(fmt, "copy", info.field.getName());
  }

}

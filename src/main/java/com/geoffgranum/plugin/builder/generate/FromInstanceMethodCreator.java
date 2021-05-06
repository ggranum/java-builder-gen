package com.geoffgranum.plugin.builder.generate;

import com.geoffgranum.plugin.builder.TypeGenerationUtil;
import com.geoffgranum.plugin.builder.info.FieldInfo;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;

import java.util.List;

/**
 * @author ggranum
 */
public class FromInstanceMethodCreator {

  public void create(PsiElementFactory psiElementFactory,
                     PsiClass containerClass,
                     PsiClass builderClass,
                     List<FieldInfo> fields) {

    String fmt = "public %1$s from(%2$s copy){\n" + "  %3$s" + "  return this;" + "}";
    String fieldInitFmt = "%1$s = copy.%1$s;\n";

    StringBuilder body = new StringBuilder();

    for (FieldInfo info : fields) {
      body.append(String.format(fieldInitFmt, info.field.getName()));
    }

    String methodText = String.format(fmt, builderClass.getName(), containerClass.getQualifiedName(), body.toString());

    TypeGenerationUtil.addMethod(psiElementFactory, builderClass, null, methodText, true);
  }
}

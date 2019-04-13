package com.geoffgranum.plugin.builder.info;

import com.intellij.psi.PsiAnnotation;

/**
 * @author ggranum
 */
public final class FieldAnnotationsInfo {

  public final boolean hasNotNull;

  public final boolean hasNullable;

  private FieldAnnotationsInfo(Builder builder) {
    hasNotNull = builder.hasNotNull;
    hasNullable = builder.hasNullable;
  }


  public static FieldAnnotationsInfo from(PsiAnnotation[] annotations) {
    Builder builder = new Builder().hasNotNull(checkForAnnotationByName(annotations, "NotNull"))
                                   .hasNullable(checkForAnnotationByName(annotations, "Nullable"));
    return builder.build();
  }

  private static boolean checkForAnnotationByName(PsiAnnotation[] annotations, String name) {
    boolean result = false;
    for (PsiAnnotation annotation : annotations) {
      String qName = annotation.getQualifiedName();
      if (qName != null && qName.contains(name)) {
        result = true;
        break;
      }
    }
    return result;

  }

  public static final class Builder {
    private Boolean hasNotNull;

    private Boolean hasNullable;

    /*
      BuilderFieldAnnotationsInfo builderFieldAnnotationsInfo = new BuilderFieldAnnotationsInfo.Builder()
      .hasNotNull( input.getHasNotNull() )
      .hasNullable( input.getHasNullable() )
      .build();
    */
    public Builder() {
    }

    public Builder from(FieldAnnotationsInfo copy) {
      hasNotNull = copy.hasNotNull;
      hasNullable = copy.hasNullable;
      return this;
    }

    public Builder hasNotNull(boolean hasNotNull) {
      this.hasNotNull = hasNotNull;
      return this;
    }

    public Builder hasNullable(boolean hasNullable) {
      this.hasNullable = hasNullable;
      return this;
    }

    public FieldAnnotationsInfo build() {
      return new FieldAnnotationsInfo(this);
    }
  }
}

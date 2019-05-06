package com.geoffgranum.plugin.builder.info;

import com.geoffgranum.plugin.builder.InterestingTypes;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;

/**
 * @author ggranum
 */
public final class FieldInfo {

  public final FieldAnnotationsInfo annotationsInfo;

  /**
   * If this field represents, for example, a {@code "List<Foo>"} or {@code Optional<Bar>}, then this value
   * represents the generic type (Foo, Bar, etc).
   */
  public final PsiType[] typeParameters;

  public final PsiType actualType;

  public final String typeClassName;

  public final boolean isList;

  public final boolean isMap;

  public final boolean isPrimitiveType;

  /**
   * Literally "java.util.Optional"
   */
  public final boolean isAnOptional;

  public final PsiField field;

  private FieldInfo(Builder builder) {
    annotationsInfo = builder.annotationsInfo;
    typeParameters = builder.typeParameters;
    actualType = builder.actualType;
    typeClassName = builder.typeClassName;
    isList = builder.isCollection;
    isMap = builder.isMap;
    isAnOptional = builder.isAnOptional;
    field = builder.field;

    isPrimitiveType = field.getType() instanceof PsiPrimitiveType;
  }


  public static FieldInfo from(PsiField field, InterestingTypes interestingTypes) {
    Builder builder = new Builder();
    PsiType type = field.getType();
    if (type instanceof PsiClassReferenceType) {
      PsiClassReferenceType pType = (PsiClassReferenceType) type;
      builder.typeParameters(pType.getParameters())
             .typeClassName(pType.getClassName())
             .isAnOptional(interestingTypes.isAnOptional(pType))
             .isMap(interestingTypes.isJavaUtilMap(pType))
             .isCollection(interestingTypes.isJavaUtilList(pType));
    } else {

      builder.typeClassName(type.getPresentableText());
    }
    return builder.field(field)
                  .actualType(type)
                  .annotationsInfo(new FieldAnnotationsInfoParser().parse(field.getAnnotations()))
                  .build();
  }

  public static final class Builder {
    private FieldAnnotationsInfo annotationsInfo;

    private PsiType[] typeParameters;

    private PsiType actualType;

    private String typeClassName;

    private boolean isCollection;

    private boolean isMap;

    private boolean isAnOptional;

    private PsiField field;

    /*
      FieldInfo fieldInfo = new FieldInfo.Builder()
      .annotationsInfo( input.getAnnotationsInfo() )
      .genericType( input.getGenericType() )
      .actualType( input.getActualType() )
      .typeClassName( input.getTypeClassName() )
      .isCollection( input.getIsCollection() )
      .isMap( input.getIsMap() )
      .isAnOptional( input.getIsAnOptional() )
      .field( input.getField() )
      .build();
    */
    public Builder() {
    }

    public Builder actualType(PsiType actualType) {
      this.actualType = actualType;
      return this;
    }

    public Builder annotationsInfo(FieldAnnotationsInfo annotationsInfo) {
      this.annotationsInfo = annotationsInfo;
      return this;
    }

    public Builder field(PsiField field) {
      this.field = field;
      return this;
    }

    public Builder isAnOptional(boolean isAnOptional) {
      this.isAnOptional = isAnOptional;
      return this;
    }

    public Builder isCollection(boolean isCollection) {
      this.isCollection = isCollection;
      return this;
    }

    public Builder isMap(boolean isMap) {
      this.isMap = isMap;
      return this;
    }

    public Builder typeClassName(String typeClassName) {
      this.typeClassName = typeClassName;
      return this;
    }

    public Builder typeParameters(PsiType[] typeParameters) {
      this.typeParameters = typeParameters;
      return this;
    }

    public FieldInfo build() {
      return new FieldInfo(this);
    }
  }
}

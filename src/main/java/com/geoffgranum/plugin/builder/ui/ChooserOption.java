package com.geoffgranum.plugin.builder.ui;

/**
 * @author ggranum
 */
public class ChooserOption {


  public static final ChooserOption[] options =
    { new ChooserOption("Create Copy Method", "Add a copy method.", 'c', ValueKeys.ADD_COPY_METHOD),
      new ChooserOption("Implement Validated",
                        "Add Hibernate validations stubs to builder fields and implement the Validated class.",
                        'v',
                        ValueKeys.IMPLEMENT_VALIDATED),
      new ChooserOption("Enable Jackson marshaling for class.",
                        "Annotate the class and Builder fields with the Jackson Annotations required for marshaling/unmarshalling the class.",
                        'm',
                        ValueKeys.GENERATE_JSON_ANNOTATIONS),
      new ChooserOption("Create Example Comment",
                        "Adds an example use of the builder to the constructor comment.",
                        'e',
                        ValueKeys.ADD_EXAMPLE_CODE_COMMENT), };

  public final String title;

  public final String description;

  public final char mnemonic;

  public final String key;

  public ChooserOption(String title, String description, char mnemonic, String key) {
    this.title = title;
    this.description = description;
    this.mnemonic = mnemonic;
    this.key = key;
  }
}



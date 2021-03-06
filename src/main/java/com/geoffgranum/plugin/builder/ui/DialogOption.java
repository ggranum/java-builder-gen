package com.geoffgranum.plugin.builder.ui;

/**
 * @author ggranum
 */
public class DialogOption {

  public final String title;

  public final String description;

  public final char mnemonic;

  public DialogOption(String title, String description, char mnemonic) {
    this.title = title;
    this.description = description;
    this.mnemonic = mnemonic;
  }
}



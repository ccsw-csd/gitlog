/**
 *
 */
package com.ccsw.gitlog.common;

import java.util.ArrayList;

/**
 * @author rroigped
 *
 */
public class FileDiff {
  private String path;
  private ArrayList<String> code;

  public FileDiff(String path) {
    this.path = path;
    code = new ArrayList<String>();
  }

  public String getId() {
    return this.path.substring(1).replaceAll("[- _ . /]", "");
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public ArrayList<String> getCode() {
    return code;
  }

  public void addLine(String line) {
    this.code.add(line);
  }

}

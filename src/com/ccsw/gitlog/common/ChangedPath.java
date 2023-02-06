/**
 *
 */
package com.ccsw.gitlog.common;

/**
 * @author rroigped
 *
 */
public class ChangedPath {
  private char type;
  private String path;
  private String copyPath;
  private long copyRevision;
  private FileDiff file;

  public ChangedPath(char type, String path, String copyPath, long copyRevision) {
    this.type = type;
    this.path = path;
    this.copyPath = copyPath;
    this.copyRevision = copyRevision;
    this.file = null;
  }

  public char getType() {
    return type;
  }

  public void setType(char type) {
    this.type = type;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getCopyPath() {
    return copyPath;
  }

  public void setCopyPath(String copyPath) {
    this.copyPath = copyPath;
  }

  public long getCopyRevision() {
    return copyRevision;
  }

  public void setCopyRevision(long copyRevision) {
    this.copyRevision = copyRevision;
  }

  public FileDiff getFile() {
    return file;
  }

  public void setFile(FileDiff file) {
    this.file = file;
  }

  @Override
  public String toString() {
    return this.path
        + ((this.copyPath != null) ? " (from " + this.copyPath + " revision " + this.copyRevision + ")" : "");
  }
}

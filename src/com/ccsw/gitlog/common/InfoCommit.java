/**
 *
 */
package com.ccsw.gitlog.common;

import java.util.ArrayList;
import java.util.Date;

/**
 * @author rroigped
 *
 */
public class InfoCommit {
  private long revNumber;
  private String author;
  private Date date;
  private String commit;
  private ArrayList<ChangedPath> changedPaths;

  public InfoCommit(long revNumber, String author, Date date, String commit) {
    this.revNumber = revNumber;
    this.author = author;
    this.date = date;
    this.commit = commit;
    this.changedPaths = new ArrayList<ChangedPath>();
  }

  public long getRevNumber() {
    return revNumber;
  }

  public void setRevNumber(long revNumber) {
    this.revNumber = revNumber;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getCommit() {
    return commit;
  }

  public void setCommit(String commit) {
    this.commit = commit;
  }

  public ArrayList<ChangedPath> getChangedPaths() {
    return changedPaths;
  }

  public void setChangedPaths(ArrayList<ChangedPath> changedPaths) {
    this.changedPaths = changedPaths;
  }

  public void addChangedPath(ChangedPath c) {
    this.changedPaths.add(c);
  }
}

package com.ccsw.gitlog;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ccsw.gitlog.common.RepoUtils;
import com.ccsw.gitlog.processgit.GitScanner;

/**
 * @author pajimene
 *
 */
public class Main {

  /**
   * @param args
   */
  public static void main(String[] args) {

    System.out.println();

    if (args.length < 5) {
      System.out.println(
          "** Error: Invalid arguments\n\nusage: java -jar gitlog.jar <reposFile> <user> <pass> <fromDate*> <toDate*> <blackListFile**> <extensionsFile**>\n\n"
              + "  (*)format date: DD/MM/YYYY\n" //
              + "  (**)Optional file");

      return;
    }

    List<String> blacklistWords = new ArrayList<String>();

    if (args.length >= 6) {
      File blacklistFile = new File(args[5]);
      if (!blacklistFile.exists()) {
        System.out.println("** Error: blacklistFile not exists");
        return;
      }

      try {
        System.out.println("Reading blacklistFile");
        blacklistWords = RepoUtils.readFileToArrayList(blacklistFile);
        System.out.println("  Readed " + blacklistWords.size() + " words");
      } catch (Exception e) {
        System.out.println("** Error: blacklistFile with invalid format");
        return;
      }
    }

    Map<String, Boolean> validExtensions = RepoUtils.validExtensions;

    if (args.length >= 7) {
      File extensionsFile = new File(args[6]);
      if (!extensionsFile.exists()) {
        System.out.println("** Error: ExtensionsFile not exists");
        return;
      }

      try {
        System.out.println("Reading ExtensionsFile");
        validExtensions = RepoUtils.readFileToHashMap(extensionsFile);
        System.out.println("  Readed " + validExtensions.size() + " extensions");
      } catch (Exception e) {
        System.out.println("** Error: ExtensionsFile with invalid format");
        return;
      }
    }

    File file = new File(args[0]);
    if (!file.exists()) {
      System.out.println("** Error: GitFile not exists");
      return;
    }

    List<String> gitRepos;
    try {
      System.out.println("Reading GitFile");
      gitRepos = RepoUtils.readFileToArrayList(file);
      System.out.println("  Parsed " + gitRepos.size() + " repos");
    } catch (Exception e) {
      System.out.println("** Error: GitFile with invalid format");
      return;
    }

    String user = args[1];
    String pass = args[2];

    DateFormat commitDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    Date from, to;
    try {
      System.out.println("Parse dates");
      from = RepoUtils.parseDate(args[3], true);
      to = RepoUtils.parseDate(args[4], false);

      System.out.println("  Date parsed from " + commitDateFormat.format(from) + " to " + commitDateFormat.format(to));

    } catch (Exception e) {
      System.out.println("** Error: Invalid date format");
      return;
    }

    System.out.println();
    System.out.println("Start GitLog scan");
    System.out.println();

    GitScanner gScanner = new GitScanner(validExtensions);
    gScanner.generateGitLog(gitRepos, user, pass, from, to, blacklistWords);

  }

}

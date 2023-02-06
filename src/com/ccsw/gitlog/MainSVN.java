package com.ccsw.gitlog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import com.ccsw.gitlog.common.RepoUtils;
import com.ccsw.gitlog.processsvn.SVNScanner;

public class MainSVN {
  public static void main(String[] args) {

    System.out.println();

    ArrayList<SVNURL> urls = new ArrayList();

    if (args.length < 5) {
      System.out.println(
          "** Error: Invalid arguments\n\nusage: java -jar svnlog.jar <reposFile> <user> <pass> <fromDate*> <toDate*> <blackListFile**> <extensionsFile**>\n\n"
              + "  (*)format date: DD/MM/YYYY\n" //
              + "  (**)Optional file");

      return;
    }

    List<String> blacklistWords = new ArrayList();
    File blacklistFile = null;
    if (args.length >= 6) {
      blacklistFile = new File(args[5]);
      if (!blacklistFile.exists()) {
        System.out.println("** Error: file not exists");
        return;
      }

      try {
        System.out.println("Reading blacklistFile");
        blacklistWords = RepoUtils.readFileToArrayList(blacklistFile);
        System.out.println("  Readed " + blacklistWords.size() + " words");
      } catch (Exception e) {
        System.out.println("** Error: file with invalid format");
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

    String username = args[1];
    String password = args[2];

    try {
      List<String> links = Files.readAllLines(Paths.get(args[0], new String[0]));
      for (String link : links) {
        urls.add(SVNURL.parseURIEncoded(link));
      }

      System.out.println();
      System.out.println("Start SVNLog scan");
      System.out.println();

      SVNScanner svnScanner = new SVNScanner(validExtensions);
      svnScanner.generateSVNLog(username, password, urls, from, to, blacklistWords);

    } catch (IOException e) {
      e.printStackTrace();
    } catch (SVNException e) {
      System.out.println("Error parsing " + args[0] + " links");
      System.exit(1);
    }
    System.out.println("\nDone.");
  }

}

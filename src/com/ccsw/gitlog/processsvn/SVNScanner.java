package com.ccsw.gitlog.processsvn;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import com.ccsw.gitlog.common.ChangedPath;
import com.ccsw.gitlog.common.FileDiff;
import com.ccsw.gitlog.common.InfoCommit;
import com.ccsw.gitlog.common.RepoUtils;

import rst.pdfbox.layout.elements.ControlElement;
import rst.pdfbox.layout.elements.Document;
import rst.pdfbox.layout.elements.Frame;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.shape.Rect;
import rst.pdfbox.layout.text.BaseFont;

public class SVNScanner {
  private static final String CODE_FONT_PATH = RepoUtils.codeFont;
  private static final String TITLE_FONT_PATH = RepoUtils.titleFont;

  ArrayList<SVNURL> urls;

  SVNRepository repository;

  SVNClientManager clientManager;

  List<String> blacklistWords;

  public SVNScanner(Map<String, Boolean> validExtensions) {
    RepoUtils.validExtensions = validExtensions;
  }

  private void accessSVNRepo(String username, String password, SVNURL url) throws SVNException {

    // Connection to the svn repo
    // Setup repository connection
    DAVRepositoryFactory.setup();
    SVNRepositoryFactoryImpl.setup();
    FSRepositoryFactory.setup();

    // Connect to the repository: if succeeds get root and UUID
    System.out.print("Connecting to " + url + "... ");
    this.repository = SVNRepositoryFactory.create(url);
    ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username,
        password.toCharArray());
    this.repository.setAuthenticationManager(authManager);

    // Check if the URL corresponds to a directory
    SVNNodeKind nodeKind = this.repository.checkPath("", -1);
    if (nodeKind == SVNNodeKind.NONE) {
      System.err.println("There is no entry at '" + url + "'.");
      System.exit(1);
    } else if (nodeKind == SVNNodeKind.FILE) {
      System.err.println("The entry at '" + url + "' is a file while a directory was expected.");
      System.exit(1);
    }

    // Configure client manager
    ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
    this.clientManager = SVNClientManager.newInstance(options, authManager);
    System.out.println("OK!");

  }

  private Map<String, ArrayList<InfoCommit>> getData(String username, String password, SVNURL url, Date startDate,
      Date endDate) throws SVNException {

    Map<String, ArrayList<InfoCommit>> data = new HashMap<String, ArrayList<InfoCommit>>();

    try {
      accessSVNRepo(username, password, url);
    } catch (SVNException e) {
      System.out.println("Connection failed...");
      e.printStackTrace();
      System.exit(1);
    }

    System.out.print("Getting data from " + url + "... ");

    long startRevision = this.repository.getDatedRevision(startDate);
    long endRevision = this.repository.getDatedRevision(endDate);

    ArrayList<FileDiff> diff = new ArrayList<FileDiff>(); // Storing all the code between startRevision and
                                                          // endRevision
    ByteArrayOutputStream baos = new ByteArrayOutputStream(); // Raw input from doDiff in bytes
    SVNDiffClient diffClient = this.clientManager.getDiffClient();
    diffClient.getDiffGenerator().setForcedBinaryDiff(false);
    diffClient.doDiff(url, SVNRevision.create(startRevision), url, SVNRevision.create(endRevision), SVNDepth.UNKNOWN,
        true, baos);

    // Get differences between two revisions and store them into a FileDiff class
    try {
      String diffRaw = baos.toString("UTF-8"); // Raw input from doDiff in string

      String lines[] = diffRaw.split("\\r?\\n"); // Raw input split into lines

      for (String line : lines) {
        if (line != null) {
//          String lineCode = line.replaceAll("\u0009", "    ").replaceAll("\n", "").replaceAll("\uFFFD", "")
//              .replaceAll("\u2B24", "");
          String lineCode = line;
          if (lineCode.contains("Index: ") && lineCode.indexOf('I') == 0) {
            diff.add(new FileDiff("/" + lineCode.replaceAll("Index: ", "")));
          } else {
            if (!diff.isEmpty())
              diff.get(diff.size() - 1).addLine(lineCode);
          }
        }
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    diff.removeIf(f -> !RepoUtils.isValidExtension(f.getPath()));

    // Getting log data from the repository
    Collection logEntries = this.repository.log(new String[] { "" }, null, startRevision, endRevision, true, true);

    // Iterate through logEntries to store data from revisions into InfoCommit class
    for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
      ArrayList<InfoCommit> info = new ArrayList<InfoCommit>();
      SVNLogEntry logEntry = (SVNLogEntry) entries.next();

      if (logEntry.getAuthor() != null) {
        InfoCommit rev = new InfoCommit(logEntry.getRevision(), logEntry.getAuthor(), logEntry.getDate(),
            logEntry.getMessage());
        if (data.get(logEntry.getAuthor()) == null)
          data.put(logEntry.getAuthor(), new ArrayList<InfoCommit>());

        if (logEntry.getChangedPaths().size() > 0) {
          Set changedPathsSet = logEntry.getChangedPaths().keySet();
          for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();) {
            SVNLogEntryPath entryPath = logEntry.getChangedPaths().get(changedPaths.next());

            ChangedPath cp = new ChangedPath(entryPath.getType(), entryPath.getPath(), entryPath.getCopyPath(),
                entryPath.getCopyRevision());

            for (FileDiff file : diff) {
              if (entryPath.getPath().contains(file.getPath())) {
                cp.setFile(file);
              }
            }
            rev.addChangedPath(cp);
          }
        }
        data.get(logEntry.getAuthor()).add(rev);
      }
    }

    System.out.println("OK!");

    return data;
  }

  public void generateSVNLog(String username, String password, ArrayList<SVNURL> urls, Date startDate, Date endDate,
      List<String> blacklistWords) throws IOException, SVNException {

    this.blacklistWords = blacklistWords;

    Set<String> users = new HashSet<String>(); // To keep how many users need PDF
    LinkedList<Map<String, ArrayList<InfoCommit>>> commits = new LinkedList<Map<String, ArrayList<InfoCommit>>>();
    // For keeping commits ordered like the txt

    for (SVNURL url : urls) {
      Map<String, ArrayList<InfoCommit>> repoData = getData(username, password, url, startDate, endDate);
      Iterator it = repoData.keySet().iterator();
      while (it.hasNext())
        users.add((String) it.next());
      commits.add(repoData);
    }

    System.out.println("\nGenerating PDF files. \nDestination files will be at ./pdf/*");

    for (String user : users) {
      Set<FileDiff> diff = new HashSet<FileDiff>(); // For adding all the code without repeating
      Document document = new Document(40, 50, 40, 60);

//      InputStream fontStream = new FileInputStream("c:/Windows/Fonts/ARIALUNI.TTF");
//      PDType0Font titleFont = PDType0Font.load(document.getPDDocument(), fontStream);

      boolean hasEntries = false;
      for (Map<String, ArrayList<InfoCommit>> repo : commits) {
        Iterator it = repo.keySet().iterator();
        // iterate through every author
        while (it.hasNext()) {
          String author = (String) it.next();
          if (author.equals(user)) {
            hasEntries = true;
            Paragraph p = new Paragraph();

            // First page of every repo that shows info
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            p.addMarkup(
                "Commits of *" + user + "* (" + format.format(startDate) + ") - (" + format.format(endDate) + ")\n", 20,
                BaseFont.Helvetica);
            String repoUrls = RepoUtils.removeInvalidCharacters(urls.get(commits.indexOf(repo)).toString(),
                PDType1Font.HELVETICA_OBLIQUE);
            p.addText(repoUrls + "\n", 18, PDType1Font.HELVETICA_OBLIQUE);
            document.add(p);
            document.add(ControlElement.NEWPAGE);
            // Adding revisions and changed paths for the repo
            for (InfoCommit i : repo.get(author)) {
              p = new Paragraph();
              p.addText("Revision " + i.getRevNumber() + " por " + i.getAuthor() + "\n", 18,
                  PDType1Font.HELVETICA_BOLD);
              p.addText(i.getDate() + "\n", 14, PDType1Font.HELVETICA_OBLIQUE);

              p.addText("Commit: '"
                  + RepoUtils.removeInvalidCharacters(i.getCommit(), blacklistWords, PDType1Font.HELVETICA) + "'\n\n",
                  12, PDType1Font.HELVETICA);
              p.addText("Changed paths and files\n", 13, PDType1Font.HELVETICA);
              document.add(p);

              if (i.getChangedPaths().size() > 0) {
                Paragraph p1 = new Paragraph();
                for (ChangedPath c : i.getChangedPaths()) {
                  if (RepoUtils.isValidExtension(c.getPath())) {
                    String col;
                    switch (c.getType()) {
                      case 'A':
                        col = "{color:#009900}";
                        break;
                      case 'M':
                        col = "{color:#ffa500}";
                        break;
                      case 'D':
                        col = "{color:#ff0000}";
                        break;
                      default:
                        col = "{color:#7d7d7d}";
                        break;
                    }

                    if (c.getFile() != null) {
                      diff.add(c.getFile());
                      // String markup = col + "{link[#" + c.getFile().getId() + "]}" + c + "{link}
                      // \n";
                    }
                    String markup = col + c;
                    p1.addMarkup(
                        RepoUtils.removeInvalidCharacters(markup, this.blacklistWords, PDType1Font.HELVETICA) + "\n",
                        10.0F, BaseFont.Helvetica);
                  }
                }
                document.add(p1);
              }
              Paragraph spacing = new Paragraph();
              spacing.addText("\n\n", 12, PDType1Font.HELVETICA);
              document.add(spacing);
            }
          }
        } // while
      } // for 2
      if (hasEntries) {
        // Show differences between startRevision and endRevision
        // Adding console font to the document
        PDFont font = PDType0Font.load(document.getPDDocument(), new File(CODE_FONT_PATH));

        // Add diff code
        for (FileDiff code : diff) {
          // Iterate through all the files that matches between changed paths and diff to
          // be sure that we can link the
          // hyperlinks
          document.add(ControlElement.NEWPAGE);
          Paragraph title = new Paragraph();
          // String markup = "{anchor:" + code.getId() + "}" + code.getPath() + "
          // {anchor}" + "\n";
          String markup = code.getPath();
          title.addMarkup(RepoUtils.removeInvalidCharacters(markup, this.blacklistWords, PDType1Font.HELVETICA) + "\n",
              18, BaseFont.Helvetica);
          document.add(title);

          for (String line : code.getCode()) {
            Paragraph pCode = new Paragraph();
            Frame frame = new Frame(pCode, document.getPageWidth(), null);
            frame.setShape(new Rect());
            if (line.length() > 0) {
              if (line.charAt(0) == '+')
                frame.setBackgroundColor(new Color(152, 251, 152));
              else if (line.charAt(0) == '-')
                frame.setBackgroundColor(new Color(255, 160, 122));
              else
                frame.setBackgroundColor(new Color(240, 240, 240));
            }
            pCode.addText(RepoUtils.removeInvalidCharacters(line, font) + "\n", 8, font);
            document.add(frame);
          }
        }
      } // if(hasEntries)

      DateFormat dateFormat = new SimpleDateFormat("MM-YYYY");
      String fileName = user + "-" + dateFormat.format(startDate) + "-SVN" + ".pdf";

      System.out.println("  Write file " + fileName);

      final OutputStream outputStream = new FileOutputStream("pdf/" + fileName);
      document.save(outputStream);
    } // for 1
  } // generateSVNLog(...)

} // Class

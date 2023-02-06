package com.ccsw.gitlog.processgit;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;

import com.ccsw.gitlog.common.FileDiff;
import com.ccsw.gitlog.common.RepoUtils;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import rst.pdfbox.layout.elements.ControlElement;
import rst.pdfbox.layout.elements.Document;
import rst.pdfbox.layout.elements.Frame;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.shape.Rect;
import rst.pdfbox.layout.shape.Stroke;
import rst.pdfbox.layout.text.Alignment;
import rst.pdfbox.layout.text.BaseFont;
import rst.pdfbox.layout.text.Indent;
import rst.pdfbox.layout.text.SpaceUnit;

class CommitObj {
    public CommitObj(RevCommit data) {

        this.data = data;
        this.changes = null;
    }

    public RevCommit data;

    public FileDiff changes;
}

public class GitScanner {
    private static final String FONT_PATH = RepoUtils.codeFont;

    Map<String, Boolean> validExtensions = new HashMap<>();

    public GitScanner(Map<String, Boolean> validExtensions) {

        this.validExtensions = validExtensions;
    }

    /**
    * {@inheritDoc}
    */
    public void generateGitLog(List<String> gitRepos, String user, String pass, Date from, Date to, List<String> blacklistWords) {

        File root = new File("pdf");
        root.mkdirs();

        HashMap<String, Map<String, ArrayList<CommitObj>>> gits = obtainCommits(gitRepos, from, to, user, pass, blacklistWords);
        System.out.println();

        try {
            for (Map.Entry person : gits.entrySet()) {

                System.out.println("Creating PDF for " + person.getKey());

                String personName = (String) person.getKey();
                Map<String, ArrayList<CommitObj>> projects = (Map<String, ArrayList<CommitObj>>) person.getValue();

                Document document = new Document(40, 50, 40, 60);

                DateFormat monthDateFormat = new SimpleDateFormat("MMMM yyyy");
                DateFormat commitDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

                Paragraph title = new Paragraph();
                String text = RepoUtils.removeInvalidCharacters("Commits de " + personName + ": " + monthDateFormat.format(from), blacklistWords, PDType1Font.HELVETICA_BOLD);
                title.addText(text, 20, PDType1Font.HELVETICA_BOLD);

                Frame titleFrame = new Frame(title);
                titleFrame.setShape(new Rect());
                titleFrame.setMargin(0, 0, 0, 10);

                document.add(titleFrame);

                for (Map.Entry git : projects.entrySet()) {

                    System.out.println("  Add summary for " + git.getKey());

                    String name = (String) git.getKey();
                    ArrayList<CommitObj> commits = (ArrayList<CommitObj>) git.getValue();

                    Paragraph subtitle = new Paragraph();
                    subtitle.addText("�?ndice " + name, 16, PDType1Font.HELVETICA_BOLD);

                    Frame subtitleFrame = new Frame(subtitle);
                    subtitleFrame.setShape(new Rect());
                    subtitleFrame.setMargin(0, 0, 5, 15);

                    document.add(subtitleFrame);

                    Paragraph paragraph;
                    for (CommitObj commit : commits) {
                        RevCommit commit_data = commit.data;

                        paragraph = new Paragraph();

                        // Fecha
                        Date commitDate = new Date(Long.valueOf(String.valueOf(commit_data.getCommitTime())) * 1000);
                        String commitDateString = commitDateFormat.format(commitDate);
                        paragraph.addMarkup("{color:#777777}Autor: " + commit_data.getAuthorIdent().getName() + " " + commitDateString + ":", 12F, BaseFont.Helvetica);

                        Frame commitInfoFrame = new Frame(paragraph);
                        commitInfoFrame.setShape(new Rect());

                        document.add(commitInfoFrame);

                        // Mensaje commit
                        paragraph = new Paragraph();
                        paragraph.add(new Indent("  ", 6, SpaceUnit.em, 10, PDType1Font.HELVETICA_BOLD, Alignment.Right));
                        paragraph.addText(RepoUtils.removeInvalidCharacters(commit_data.getShortMessage(), blacklistWords, PDType1Font.HELVETICA), 12, PDType1Font.HELVETICA);
                        paragraph.addText("  - " + commit_data.getName() + "\n", 9, PDType1Font.HELVETICA);

                        Frame frame = new Frame(paragraph);
                        frame.setShape(new Rect());
                        frame.setMargin(0, 0, 10, 30);

                        document.add(frame);
                    }

                    document.add(ControlElement.NEWPAGE);
                }

                for (Map.Entry git : projects.entrySet()) {
                    System.out.println("  Add detail for " + git.getKey());

                    String name = (String) git.getKey();
                    ArrayList<CommitObj> commits = (ArrayList<CommitObj>) git.getValue();

                    Paragraph subtitle = new Paragraph();
                    subtitle = new Paragraph();
                    subtitle.addText("Detalles " + name, 16, PDType1Font.HELVETICA_BOLD);

                    Frame subtitleFrame = new Frame(subtitle);
                    subtitleFrame = new Frame(subtitle);
                    subtitleFrame.setShape(new Rect());
                    subtitleFrame.setMargin(0, 0, 0, 5);

                    document.add(subtitleFrame);

                    Paragraph paragraph;
                    for (CommitObj commit : commits) {
                        RevCommit commit_data = commit.data;

                        // System.out.println(" " + commit_data.getName());

                        Frame divider = new Frame(new Paragraph(), 515f, 1f);
                        divider.setShape(new Rect());
                        divider.setBorder(Color.LIGHT_GRAY, new Stroke(1));
                        divider.setMargin(0, 0, 15, 15);

                        document.add(divider);

                        paragraph = new Paragraph();

                        // Fecha
                        Date commitDate = new Date(Long.valueOf(String.valueOf(commit_data.getCommitTime())) * 1000);
                        String commitDateString = commitDateFormat.format(commitDate);
                        paragraph.addMarkup("{color:#777777}Autor: " + commit_data.getAuthorIdent().getName() + " " + commitDateString + ":", 12F, BaseFont.Helvetica);

                        Frame commitInfoFrame = new Frame(paragraph);
                        commitInfoFrame.setShape(new Rect());

                        document.add(commitInfoFrame);

                        // Mensaje commit
                        paragraph = new Paragraph();
                        paragraph.add(new Indent("  ", 6, SpaceUnit.em, 10, PDType1Font.HELVETICA_BOLD, Alignment.Right));
                        paragraph.addText(RepoUtils.removeInvalidCharacters(commit_data.getShortMessage(), blacklistWords, PDType1Font.HELVETICA), 12, PDType1Font.HELVETICA);
                        paragraph.addText("  - " + commit_data.getName(), 9, PDType1Font.HELVETICA);

                        Frame frame = new Frame(paragraph);
                        frame.setShape(new Rect());
                        frame.setMargin(0, 0, 10, 30);

                        document.add(frame);

                        // CÃ³digo
                        if (commit.changes != null) {
                            PDFont font = PDType0Font.load(document.getPDDocument(), new File(FONT_PATH));
                            // Iterate through all the files that matches between changed paths and diff to
                            // be sure that we can link the
                            // hyperlinks

                            for (String line : commit.changes.getCode()) {
                                if (line.length() > 0) {

                                    line = RepoUtils.removeInvalidCharacters(line, blacklistWords, font);
                                    if (line.charAt(0) != 'd') {
                                        Paragraph pCode = new Paragraph();
                                        frame = new Frame(pCode, document.getPageWidth(), null);
                                        frame.setShape(new Rect());

                                        if (line.charAt(0) == '+')
                                            frame.setBackgroundColor(new Color(152, 251, 152));
                                        else if (line.charAt(0) == '-')
                                            frame.setBackgroundColor(new Color(255, 160, 122));
                                        else
                                            frame.setBackgroundColor(new Color(240, 240, 240));
                                        pCode.addText(line + "\n", 8, font);
                                        document.add(frame);
                                    } else {
                                        title = new Paragraph();
                                        line = line.replace("diff --git ", "").replace(" b/", "\nb/");
                                        title.addText("\n" + line, 8, PDType1Font.HELVETICA_BOLD);
                                        document.add(title);
                                    }
                                }
                            }
                        }
                    }

                    document.add(ControlElement.NEWPAGE);
                }

                DateFormat dateFormat = new SimpleDateFormat("MM-YYYY");
                String fileName = personName + "-" + dateFormat.format(from) + "-Git" + ".pdf";
                System.out.println("  Write file " + fileName);

                final OutputStream outputStream = new FileOutputStream("pdf/" + fileName);
                document.save(outputStream);

                System.out.println("GENERATED!!");
                System.out.println();
                System.out.println();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * @param line
    * @return
    */
    private boolean isValidExtension(DiffEntry entry) {

        String oldPath = entry.getOldPath();
        String newPath = entry.getNewPath();

        for (String validExtension : this.validExtensions.keySet()) {

            if (oldPath.endsWith(validExtension))
                return true;

            if (newPath.endsWith(validExtension))
                return true;

        }

        return false;
    }

    private Git getGitObjectUserPass(String url, String user, String pass) {

        Git git = null;

        System.out.println("Cloning " + url);

        File localPath;
        try {

            localPath = File.createTempFile("TestGitRepository", "");

            if (!localPath.delete()) {
                throw new IOException("Could not delete temporary file " + localPath);
            }

            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(user, pass);

            git = Git.cloneRepository().setURI(url).setCredentialsProvider(cp).setDirectory(localPath).call();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        return git;
    }

    private Git getGitObject(String url, String user, String pass) {

        Git git = null;

        System.out.println("Cloning " + url);

        File localPath;
        try {

            localPath = File.createTempFile("TestGitRepository", "");

            if (!localPath.delete()) {
                throw new IOException("Could not delete temporary file " + localPath);
            }

            TransportConfigCallback transportConfigCallback = new SshTransportConfigCallback();

            git = Git.cloneRepository().setTransportConfigCallback(transportConfigCallback).setURI(url).setDirectory(localPath).call();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        return git;
    }

    private static class SshPathTransportConfigCallback implements TransportConfigCallback {

        private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(Host host, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);
                defaultJSch.addIdentity(sshPrivateFilePath);
                return defaultJSch;
            }

        };

        @Override
        public void configure(Transport transport) {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        }

    }

    private static final String sshPrivateFilePath = "C:\\Users\\pajimene\\.ssh\\git-ssh.pub";
    private static final String sshPassword = "prueba";

    private static class SshTransportConfigCallback implements TransportConfigCallback {

        private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch jSch = super.createDefaultJSch(fs);
                jSch.addIdentity(sshPrivateFilePath, sshPassword.getBytes());
                return jSch;
            }
        };

        @Override
        public void configure(Transport transport) {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        }

    }

    /**
    * Normalizar nombre del git
    *
    * @param urlGit Enlace del git
    * @return
    */
    private String normalizeGitName(String urlGit) {

        return urlGit.substring(urlGit.lastIndexOf("/"));
    }

    /**
    * Obtener commits de un git entre las 2 fechas pasadas
    *
    * @param since Fecha inicio
    * @param until Fecha fin
    * @return Array con los commits
    */
    private void obtainGitCommits(Date since, Date until, String repoUrlCombined, HashMap<String, Map<String, ArrayList<CommitObj>>> map, String user, String pass, List<String> blacklistWords) {

        try {

            String splitRepo[] = repoUrlCombined.split(" ");
            String repoUrl = splitRepo[0];
            String branch = null;

            if (splitRepo.length == 2) {
                branch = splitRepo[1];
            }

            Git git = getGitObject(repoUrl, user, pass);

            Repository repo = git.getRepository();

            if (branch != null) {
                repo = git.checkout().setName(branch).getRepository();
            }

            System.out.println("  Parse commits...");

            Collection<Ref> allRefs = repo.getAllRefs().values();

            RevWalk walk = new RevWalk(repo);
            for (Ref ref : allRefs) {
                walk.markStart(walk.parseCommit(ref.getObjectId()));
            }

            RevFilter between = CommitTimeRevFilter.between(since, until);

            walk.setRevFilter(between);

            int commitCount = 0;
            for (Iterator<RevCommit> iterator = walk.iterator(); iterator.hasNext();) {
                commitCount++;
                RevCommit commitData = iterator.next();

                CommitObj c = new CommitObj(commitData);

                if (commitData.getParents().length > 0) {
                    ObjectReader reader = git.getRepository().newObjectReader();

                    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                    oldTreeIter.reset(reader, commitData.getTree());

                    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                    newTreeIter.reset(reader, commitData.getParents()[0].getTree());

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DiffFormatter diffFormatter = new DiffFormatter(out);
                    diffFormatter.setBinaryFileThreshold(500000);
                    diffFormatter.setRepository(git.getRepository());
                    List<DiffEntry> entries = diffFormatter.scan(newTreeIter, oldTreeIter);

                    for (DiffEntry entry : entries) {
                        if (isValidExtension(entry)) {
                            out.reset();

                            diffFormatter.format(entry);

                            c.changes = new FileDiff("");

                            String lines[] = out.toString(StandardCharsets.UTF_8.name()).split("\\r?\\n"); // Raw input split into
                            for (String line : lines) {

                                if (line != null) {
                                    c.changes.addLine(line);
                                }
                            }
                        }

                    }

                    diffFormatter.close();
                }

                String personName = c.data.getAuthorIdent().getName();
                Map<String, ArrayList<CommitObj>> commitsPerson = map.get(personName);
                if (commitsPerson == null)
                    commitsPerson = new HashMap<String, ArrayList<CommitObj>>();

                String repoNormalize = normalizeGitName(repoUrl);

                ArrayList<CommitObj> commitsGit = commitsPerson.get(repoNormalize);
                if (commitsGit == null)
                    commitsGit = new ArrayList<CommitObj>();

                commitsGit.add(c);

                commitsPerson.put(repoNormalize, commitsGit);

                map.put(personName, commitsPerson);
            }

            // walk.release();
            walk.reset();

            System.out.println("  Parsed " + commitCount + " commits");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    *
    * @param from
    * @param to
    * @return
    */
    private HashMap<String, Map<String, ArrayList<CommitObj>>> obtainCommits(List<String> gitRepos, Date from, Date to, String user, String pass, List<String> blacklistWords) {

        HashMap<String, Map<String, ArrayList<CommitObj>>> gitcommits = new HashMap();

        for (String git : gitRepos) {
            obtainGitCommits(from, to, git, gitcommits, user, pass, blacklistWords);
        }

        return gitcommits;

    }

}

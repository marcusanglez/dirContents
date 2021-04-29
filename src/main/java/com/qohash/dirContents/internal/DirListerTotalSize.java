package com.qohash.dirContents.internal;

import org.springframework.stereotype.Service;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 This class implements the functionality to list a directory.
 User java -jar DirListerTotalSize to access the CLI.
 the jar o the class can be added to a given machine and an sh, eg. dirLister.sh
 and this dirLister could be added to a util dir  that is referenced by the PATH
 environment var, the sh should have the following call:
/////
 java -jar DirListerTotalSize $1
////
then executing:

 $ dirLister.sh [someDir]

 would immediately produce the dir listing. Also

 $dirLister.sh -ui

 would call a UI file chooser component

 $dirLister.sh -cli

 will list all the files and request to type the file that we want to access at this level

 */
@Service
public class DirListerTotalSize {

    public static final Runtime RUNTIME = Runtime.getRuntime();
    private final static String LIST_SORTED_CMD_FORMAT = "ls %s -lS";
    private static final String UI = "-ui";
    private static final String CLI = "-cli";
    private static final String API = "-api";
    public static final boolean ONLY_DIRECTORIES = false;
    public static final boolean DIRS_AND_FILES = true;
    private static String OS = System.getProperty("os.name").toLowerCase();


    /**
     * If no parameters are added then
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (!isUnix()) {
            System.out.println("ERROR: This program only runs on the Unix platform");
            return;
        }

        String firstArg = (args == null || args.length == 0) ?
                ""
                : args[0];

        if ("".equals(firstArg)
                || "--help".equals(firstArg)
                || "-h".equals(firstArg)){
            System.out.println("The first argument will denote the source Directory, in case there is no specified this are the\n"
            + "following options:\n");
            System.out.println("\t"+UI+"\t\t will display a GUI component to choose a directory\n");
            System.out.println("\t"+CLI+"\t\t will inquired for the directory to be selected\n");
            System.out.println("\t" + API + "\t\t ");

            return;
        }

        DirListerTotalSize dirListerTotalSize = new DirListerTotalSize();

        String sourceDir = null;
        if (UI.equalsIgnoreCase(firstArg)){
            sourceDir = uiDirChooser();
        }else if (CLI.equalsIgnoreCase(firstArg) || "".equalsIgnoreCase(firstArg)){
            sourceDir = args.length > 1 ? args[1] : ".";
            sourceDir = dirListerTotalSize.cliDirChooser(sourceDir);
        }else{
            sourceDir = firstArg;
        }
        dirListerTotalSize.printDirectory(sourceDir, DIRS_AND_FILES);
    }

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0
                || OS.indexOf("nux") >= 0
                || OS.indexOf("aix") > 0);
    }


    private String cliDirChooser(String sourceDir) {
        boolean wantToContinue = true;
        List<String> directories = null;
        do {
            try {
                directories = getDirElements(sourceDir, ONLY_DIRECTORIES);
                directories
                        .stream()
                        .forEach(System.out::println);

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (directories == null || directories.isEmpty() ) {
                System.out.println("No more nested directories, listing current");
                wantToContinue = false;
            }else {
                sourceDir += "/" + askWhichDirectory(directories);
                wantToContinue = askWantToContinue();
            }
        } while (wantToContinue);
        return sourceDir;
    }

    private static boolean askWantToContinue() {
        System.out.println("Do you wish to continue(Y), or finish and print(n) [Y/n]?");
        Scanner console = new Scanner(System.in);
        String answer = console.next();
        if (answer != null && "N".equalsIgnoreCase(answer)) return false;
        return true;
    }

    private static String askWhichDirectory(List<String> directories){

        // Create a Scanner object to read input.
        System.out.println("Which directory?");
        Scanner console = new Scanner(System.in);
        String dir = console.next();
        System.out.print("chosen: '"+ dir + "'. ");

        if (directories.contains(dir)) return dir;
        else{
            System.out.println("Invalid directory. Please enter a valid directory.");
            return askWhichDirectory(directories);
        }
    }

    private static String uiDirChooser() {

        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("Choose Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        return (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) ?
                chooser.getSelectedFile().toString()
                : null;
    }

    /**
     * Prints the directory contents
     * @param sourceDir
     * @param onlyDirsOrAll
     * @throws Exception
     */
    private void printDirectory(String sourceDir, boolean onlyDirsOrAll) throws Exception {

        getDirElements(sourceDir, onlyDirsOrAll)
                .stream()
                .forEach(System.out::println);
    }

    /**
     * Gets the list of the startingDir, the directory size is the size of all contained files plus directory meta file
     * @param startingDir the firstLevel dir to be displayed
     * @return
     */
    public List<String> getAllDirElements(String startingDir) throws Exception {
        return getDirElements(startingDir, DirListerTotalSize.DIRS_AND_FILES);
    }

    /**
     * Performs the OS list command
     *
     * @param startingDir
     * @param onlyDirsOrAll
     * @return
     */
    private List<String> getDirElements(String startingDir, boolean onlyDirsOrAll) throws Exception {

        //TODO: verify operating system is Linux,
        Path path = null;
        try {
            path = Path.of(startingDir);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        String listSorted = String.format(DirListerTotalSize.LIST_SORTED_CMD_FORMAT, path.toString());

        Process process = executeCommand(listSorted);

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<String> lines = new ArrayList<>();
        String line = null;
        try {
            // we read and ignore the first line that contains the total
            line = reader.readLine();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        int fileCount = 0;
        Long totalSize = 0L;
        if (onlyDirsOrAll == DIRS_AND_FILES){
            lines.add("t size\t\tdate\t\t name");}
        try {
            while ((line = reader.readLine()) != null) {

                boolean dir = "d".equals(line.substring(0,1));
                if (!dir) {
                    if (onlyDirsOrAll == ONLY_DIRECTORIES){
                        continue;
                    }
                    fileCount++;
                }
                // preserve the first character to distinguish files and directories
                // remove permissions and groups
                line = line.substring(0, 1) + line.substring(24);

                if (onlyDirsOrAll == ONLY_DIRECTORIES){
                    line = getDirName(line);
                }

                Long size = 0l;
                if (onlyDirsOrAll == DIRS_AND_FILES){
                    size = dir ? getDirSize(startingDir+ "/"+ getDirName(line)) : getSize(line);
                    line = dir ? replaceImmediateSize(line, size) : line;
                }
                lines.add(line);

                totalSize += size;
            }
        } catch (IOException ignoreEx) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        if (onlyDirsOrAll == DIRS_AND_FILES) {
            lines.add(String.format("%s files", fileCount));
            lines.add(String.format("Total size: " + totalSize));
        }
        return lines;
    }

    private String replaceImmediateSize(String line, Long size) {

        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        int tokenCount = 0;
        final int SIZE_POS = 2;
        StringBuilder builder = new StringBuilder();

        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            tokenCount++;
            // replace the token with the size

            if (tokenCount == SIZE_POS) {
                try {
                    token = String.valueOf(size);
                } catch (Exception ignoreEx) {
                }
            }
            builder.append(token).append(' ');
        }
        return builder.toString();
    }

    private Long getDirSize(String dirName)  {

        Path of = Path.of(dirName);
        try {
            DirVisitor<Path> visitor = new DirVisitor();
            Files.walkFileTree(of, visitor);
            return visitor.getTotalSize();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return 0l;
        }
    }

    private static Long getSize(String line){
        try {
            return Long.valueOf(getTokenPos(line, 2));
        }catch (NumberFormatException numberFormatException){
            return 0L;
        }
    }

    private static String getDirName(String line){
        return getTokenPos(line, 6);
    }

    private static String getTokenPos(String line, int tokenPosTarget) {
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        int tokenCount = 0;

        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            tokenCount++;
            // the second token is the one with the size
            if (tokenCount == tokenPosTarget) {
                try {
                    return token;
                } catch (Exception ignoreEx) {
                }
            }
        }
        return "0";
    }

    public static Process executeCommand(String cmd) {
        try {
            return RUNTIME.exec(cmd);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }
}

/**
 *
 */
package com.ccsw.gitlog.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * @author rroigped
 *
 */
public class RepoUtils {
  public static String codeFont = "assets/consola.ttf";
  public static String titleFont = "assets/arialuni-titles.ttf";

  public static Map<String, Boolean> validExtensions = new HashMap<>();
  static {
    validExtensions.put(".java", Boolean.TRUE);
    validExtensions.put(".properties", Boolean.TRUE);
    validExtensions.put(".sql", Boolean.TRUE);
    validExtensions.put(".xml", Boolean.TRUE);
    validExtensions.put(".txt", Boolean.TRUE);
    validExtensions.put(".js", Boolean.TRUE);
    validExtensions.put(".jsp", Boolean.TRUE);
    validExtensions.put(".css", Boolean.TRUE);
    validExtensions.put(".scss", Boolean.TRUE);
    validExtensions.put(".ts", Boolean.TRUE);
    validExtensions.put(".html", Boolean.TRUE);
  }

  /**
   * @param extensionsFile
   * @return
   */
  public static Map<String, Boolean> readFileToHashMap(File file) throws Exception {

    Map<String, Boolean> result = new HashMap<>();
    BufferedReader buffer = new BufferedReader(new FileReader(file));

    String line;
    while ((line = buffer.readLine()) != null) {
      line = line.trim();

      if (line != null && line.length() > 0)
        result.put(line, Boolean.TRUE);
    }

    return result;
  }

  /**
   * @param string
   * @return
   */
  public static Date parseDate(String stringDate, boolean firstHour) {

    String splitDate[] = stringDate.split("/");
    Calendar calendar = Calendar.getInstance();

    calendar.set(Calendar.DATE, Integer.parseInt(splitDate[0]));
    calendar.set(Calendar.MONTH, Integer.parseInt(splitDate[1]) - 1);
    calendar.set(Calendar.YEAR, Integer.parseInt(splitDate[2]));

    if (firstHour) {
      calendar.set(Calendar.HOUR_OF_DAY, 0);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
    }

    else {
      calendar.set(Calendar.HOUR_OF_DAY, 23);
      calendar.set(Calendar.MINUTE, 59);
      calendar.set(Calendar.SECOND, 59);
    }

    return calendar.getTime();
  }

  public static List<String> readFileToArrayList(File file) throws Exception {

    List<String> repos = new ArrayList<String>();
    BufferedReader buffer = new BufferedReader(new FileReader(file));

    String line;
    while ((line = buffer.readLine()) != null) {
      line = line.trim();

      if (line != null && line.length() > 0)
        repos.add(line);
    }

    return repos;
  }

  public static String removeBlacklistWords(String text, List<String> blacklistWords) {

    if ((blacklistWords == null) || (blacklistWords.size() == 0)) {
      return text;
    }
    for (String word : blacklistWords) {
      if ((word != null) && (word.trim().length() > 0)) {
        text = text.replaceAll("(?i)" + word, "**CENSORED**");
      }
    }
    return text;
  }

  public static String removeInvalidCharacters(String text, List<String> blacklistWords, PDFont font) {

    text = removeBlacklistWords(text, blacklistWords);
    text = removeInvalidCharacters(text, font);

    return text;
  }

  public static String removeInvalidCharacters(String text, PDFont font) {

    StringBuilder nonSymbolBuffer = new StringBuilder();
    for (char character : text.toCharArray()) {
      if (isCharacterEncodeable(character, font)) {
        nonSymbolBuffer.append(character);
      } else {
        // handle writing line with symbols...
      }
    }

    return nonSymbolBuffer.toString();
  }

  private static boolean isCharacterEncodeable(char character, PDFont font) {

    try {
      font.encode(Character.toString(character));
      return true;
    } catch (Exception iae) {
      return false;
    }
  }

  public static boolean isValidExtension(String file) {
    String ext = "." + FilenameUtils.getExtension(file);

    for (String validExtension : validExtensions.keySet()) {
      if (validExtension.equals(ext))
        return true;
    }
    return false;

  }
}

package de.zeus.upcam.rest.format;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for formatting and parsing data related to HTTP requests and responses.
 */
public class Format {

    /**
     * Gets the current date in the format "yyyyMMdd".
     *
     * @return The current date as a formatted string.
     */
    public static String getCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return LocalDate.now().format(formatter);
    }

    /**
     * Prepares the URL by replacing "${day}" with the current date.
     *
     * @param url The URL containing "${day}" as a placeholder for the current date.
     * @return The URL with "${day}" replaced by the current date.
     */
    public static String prepareUrl(String url) {
        return url.replace("${day}", getCurrentDate());
    }

    /**
     * Parses the HTML content using the provided pattern and returns a list of matching text elements.
     *
     * @param html    The HTML content to parse.
     * @param pattern The CSS query pattern to select specific elements.
     * @return A list of text elements that match the given pattern.
     */
    public static List<String> parseHtml(String html, String pattern) {
        Document doc = Jsoup.parse(html);
        Elements links = doc.select(pattern);
        return links.stream().map(Element::text).collect(Collectors.toList());
    }
}

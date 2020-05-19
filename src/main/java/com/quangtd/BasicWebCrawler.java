package com.quangtd;

import jdk.nashorn.internal.runtime.JSONFunctions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;

public class BasicWebCrawler {
    private HashSet<String> links;
    private final int MAX_DEPTH = 5;

    public BasicWebCrawler() {
        this.links = new HashSet<String>();
    }

    public void getPageLinks(String URL, int depth) {
        //4. Check if you have already crawled the URLs
        //(we are intentionally not checking for duplicate content in this example)
        if (!links.contains(URL)) {
            try {
                //4. (i) If not add it to the index
                if (links.add(URL)) {
                    System.out.println(URL);
                }
                //2. Fetch the HTML code
                Document document = Jsoup.connect(URL).get();
                //3. Parse the HTML to extract links to other URLs
                Elements linksOnPage = document.select("a[href]");
                //5. For each extracted URL... go back to Step 4.
                depth++;
                for (Element page : linksOnPage) {
                    String url = page.attr("abs:href");
                    if (depth < MAX_DEPTH && !url.contains("facebook") && !url.contains("yout")) {
                        getPageLinks(url, depth);
                    }
                }
            } catch (Exception e) {
                System.out.println("For URL = " + URL + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new BasicWebCrawler().getPageLinks("https://hcharge.tk/", 0);
    }
}

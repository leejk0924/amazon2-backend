package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.exception.ParsingException;
import com.jk.amazon2.posting.exception.ScrapingException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NaverBlogScraper {

    private static final String NAVER_BLOG_BASE_URL = "https://blog.naver.com/PostList.naver";
    private static final int TIMEOUT_SECONDS = 10;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    private final HttpClient httpClient;

    public NaverBlogScraper() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();
    }

    public Integer scrapePostingCount(String blogId, LocalDate date) {
        try {
            String url = buildUrl(blogId, date);
            Document doc = fetchAndParse(url);
            return extractPostingCount(doc);
        } catch (ParsingException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new ScrapingException("Failed to scrape blog for " + blogId, e);
        }
    }

    private String buildUrl(String blogId, LocalDate date) {
        return String.format("%s?blogId=%s&viewdate=%s",
            NAVER_BLOG_BASE_URL, blogId, date);
    }

    private Document fetchAndParse(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new ScrapingException("HTTP " + response.statusCode() + " for " + url);
        }

        return Jsoup.parse(response.body());
    }

    private Integer extractPostingCount(Document doc) {
        Elements categoryTitlePcol2 = doc.getElementsByClass("category_title pcol2");

        if (categoryTitlePcol2.isEmpty()) {
            throw new ParsingException("Element 'category_title pcol2' not found");
        }

        Element element = categoryTitlePcol2.get(0);
        String text = element.text();

        Pattern pattern = Pattern.compile("(\\d+)개의 글");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }
}

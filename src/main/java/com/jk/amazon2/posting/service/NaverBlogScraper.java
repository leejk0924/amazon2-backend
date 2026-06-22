package com.jk.amazon2.posting.service;

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

    public ScrapingResult<Integer> scrapePostingCount(String blogId, LocalDate date) {
        try {
            String url = buildUrl(blogId, date);
            ScrapingResult<Document> fetchResult = fetchAndParse(url);
            if (fetchResult instanceof ScrapingResult.Failure<Document> failure) {
                return new ScrapingResult.Failure<>(failure.type(), failure.message(), failure.cause());
            }
            Document doc = ((ScrapingResult.Success<Document>) fetchResult).value();
            return extractPostingCount(doc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ScrapingResult.Failure<>(ScrapingResult.FailureType.NETWORK_ERROR,
                    "요청 중단: " + blogId, e);
        }
    }

    private String buildUrl(String blogId, LocalDate date) {
        return String.format("%s?blogId=%s&viewdate=%s",
            NAVER_BLOG_BASE_URL, blogId, date);
    }

    private ScrapingResult<Document> fetchAndParse(String url) throws InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
            .GET()
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            return new ScrapingResult.Failure<>(ScrapingResult.FailureType.NETWORK_ERROR,
                    "네트워크 오류: " + url, e);
        }

        if (response.statusCode() != 200) {
            return new ScrapingResult.Failure<>(ScrapingResult.FailureType.HTTP_ERROR,
                    "HTTP " + response.statusCode() + ": " + url, null);
        }

        return new ScrapingResult.Success<>(Jsoup.parse(response.body()));
    }

    private ScrapingResult<Integer> extractPostingCount(Document doc) {
        Elements categoryTitlePcol2 = doc.getElementsByClass("category_title pcol2");

        if (categoryTitlePcol2.isEmpty()) {
            return new ScrapingResult.Success<>(0);
        }

        Element element = categoryTitlePcol2.get(0);
        String text = element.text();

        Pattern pattern = Pattern.compile("(\\d+)개의 글");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return new ScrapingResult.Success<>(Integer.parseInt(matcher.group(1)));
        }

        return new ScrapingResult.Success<>(0);
    }
}

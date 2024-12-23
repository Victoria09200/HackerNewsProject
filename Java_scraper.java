/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.java_scraper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 *
 * @author A.R TRADERS
 */ 



@SpringBootApplication
public class Java_scraper implements CommandLineRunner {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/hackernews";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "password";

    public static void Java_scraper (String[] args) {
        SpringApplication.run(Java_scraper.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        scrapeStories();
    }

    private void scrapeStories() {
        String url = "https://news.ycombinator.com/";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Document doc = Jsoup.connect(url).get();
            Elements storyRows = doc.select("tr.athing");

            for (Element storyRow : storyRows) {
                String storyId = storyRow.attr("id");
                String title = storyRow.select("span.titleline > a").text();
                String storyUrl = storyRow.select("span.titleline > a").attr("href");
                String pointsText = storyRow.nextElementSibling().select("span.score").text();
                int points = pointsText.isEmpty() ? 0 : Integer.parseInt(pointsText.split(" ")[0]);
                String submitter = storyRow.nextElementSibling().select("a.hnuser").text();
                String submissionTime = storyRow.nextElementSibling().select("span.age").attr("title");
                int commentCount = extractCommentCount(storyRow.nextElementSibling().select("a").last().text());

                saveStory(connection, storyId, title, storyUrl, points, submitter, submissionTime, commentCount);
            }
        } catch (IOException | SQLException e) {
        }
    }

    private int extractCommentCount(String commentText) {
        if (commentText.endsWith("comments")) {
            return Integer.parseInt(commentText.split(" ")[0]);
        }
        return 0;
    }

    private void saveStory(Connection connection, String storyId, String title, String url, int points, String submitter, String submissionTime, int commentCount) throws SQLException {
        String sql = "INSERT INTO stories (story_id, title, url, points, submitter, submission_time, comment_count) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (story_id) DO NOTHING;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, storyId);
            pstmt.setString(2, title);
            pstmt.setString(3, url);
            pstmt.setInt(4, points);
            pstmt.setString(5, submitter);
            pstmt.setObject(6, LocalDateTime.parse(submissionTime).toInstant(ZoneOffset.UTC));
            pstmt.setInt(7, commentCount);
            pstmt.executeUpdate();
        }
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.Post;
import util.DBConnection;

public class PostDAO {
    public static void createPost(Post post) {
        String sql = "INSERT INTO posts (title, content) VALUES (?, ?)";

        try (
                Connection conn = DBConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, post.getTitle());
            stmt.setString(2, post.getContent());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static List<Post> getAllPosts() {
        List<Post> posts = new ArrayList();
        String sql = "SELECT * FROM posts";

        Post post;
        try (
                Connection conn = DBConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
        ) {
            for(; rs.next(); posts.add(post)) {
                post = new Post();
                post.setId(rs.getInt("id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                if (rs.getTimestamp("created_at") != null) {
                    post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return posts;
    }

    public static void updatePost(int id, String title, String content) {
        String sql = "UPDATE posts SET title=?, content=? WHERE id=?";

        try (
                Connection conn = DBConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, title);
            stmt.setString(2, content);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void deletePost(int id) {
        String sql = "DELETE FROM posts WHERE id=?";

        try (
                Connection conn = DBConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

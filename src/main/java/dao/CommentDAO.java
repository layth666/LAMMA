//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.Comment;
import util.DBConnection;

public class CommentDAO {
    public static void createComment(Comment comment) {
        String sql = "INSERT INTO comments (post_id, content) VALUES (?, ?)";

        try (
                Connection conn = DBConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, comment.getPostId());
            stmt.setString(2, comment.getContent());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static List<Comment> getCommentsByPostId(int postId) {
        List<Comment> comments = new ArrayList();
        String sql = "SELECT * FROM comments WHERE post_id=?";

        try (
                Connection conn = DBConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, postId);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                comments.add(new Comment(rs.getInt("id"), rs.getString("content"), rs.getInt("post_id")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return comments;
    }

    public static void updateComment(int id, String content) {
        String sql = "UPDATE comments SET content=? WHERE id=?";

        try (
                Connection conn = DBConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, content);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void deleteComment(int id) {
        String sql = "DELETE FROM comments WHERE id=?";

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

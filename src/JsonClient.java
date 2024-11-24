import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class JsonClient {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/users";

    public static String createUser(String userJson) throws IOException {
        URL url = new URL(BASE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = userJson.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return readResponse(connection);
    }

    public static String updateUser(int id, String userJson) throws IOException {
        URL url = new URL(BASE_URL + "/" + id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = userJson.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return readResponse(connection);
    }

    public static int deleteUser(int id) throws IOException {
        URL url = new URL(BASE_URL + "/" + id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");

        return connection.getResponseCode();
    }

    public static String getAllUsers() throws IOException {
        URL url = new URL(BASE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        return readResponse(connection);
    }

    public static String getUserById(int id) throws IOException {
        URL url = new URL(BASE_URL + "/" + id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        return readResponse(connection);
    }

    public static String getUserByUsername(String username) throws IOException {
        URL url = new URL(BASE_URL + "?username=" + username);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        return readResponse(connection);
    }

    public static String getOpenTasksForUser(int userId) throws IOException {
        URL url = new URL(BASE_URL + "/" + userId + "/todos");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        String response = readResponse(connection);

        String[] todos = response.split("},");
        StringBuilder openTasks = new StringBuilder("[");
        for (String todo : todos) {
            if (todo.contains("\"completed\":false")) {
                if (openTasks.length() > 1) {
                    openTasks.append(",");
                }
                openTasks.append(todo).append("}");
            }
        }
        openTasks.append("]");
        return openTasks.toString();
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        BufferedReader reader;
        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        }
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line.trim());
        }
        reader.close();
        return response.toString();
    }

    public static void saveCommentsForLastPost(int userId) throws IOException {
        URL postsUrl = new URL(BASE_URL + "/" + userId + "/posts");
        HttpURLConnection postsConnection = (HttpURLConnection) postsUrl.openConnection();
        postsConnection.setRequestMethod("GET");
        String postsResponse = readResponse(postsConnection);

        int lastPostId = -1;
        String[] posts = postsResponse.split("},");
        for (String post : posts) {
            int idIndex = post.indexOf("\"id\":");
            if (idIndex != -1) {
                int idStart = idIndex + 5;
                int idEnd = post.indexOf(',', idStart);
                int postId = Integer.parseInt(post.substring(idStart, idEnd).trim());
                lastPostId = Math.max(lastPostId, postId);
            }
        }

        if (lastPostId == -1) {
            throw new IOException("No posts found for user " + userId);
        }

        URL commentsUrl = new URL("https://jsonplaceholder.typicode.com/posts/" + lastPostId + "/comments");
        HttpURLConnection commentsConnection = (HttpURLConnection) commentsUrl.openConnection();
        commentsConnection.setRequestMethod("GET");
        String commentsResponse = readResponse(commentsConnection);

        String fileName = "user-" + userId + "-post-" + lastPostId + "-comments.json";
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(commentsResponse);
            System.out.println("Comments saved to " + fileName);
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("Open tasks for user with ID 1:");
            System.out.println(getOpenTasksForUser(1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.movieexplorer;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class BackendServer {
    private static final int PORT = 8081;
    private static HttpServer server;
    private static Connection connection;

    // FRONTEND URL - Update this to match your frontend port
    private static final String FRONTEND_ORIGIN = "http://localhost:8000";

    // In-memory session storage
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("🚀 Starting Movie Explorer Backend Server...");
        System.out.println("📌 Using DBConnection class for database connections");

        // Initialize database using your DBConnection class
        initDatabase();

        // Create HTTP server
        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Setup routes
        setupRoutes();

        server.start();
        System.out.println("✅ Server running on http://localhost:" + PORT);
        System.out.println("📊 API Endpoints:");
        System.out.println("  POST /api/register    - Register new user");
        System.out.println("  POST /api/login       - Login user");
        System.out.println("  GET  /api/profile     - Get user profile");
        System.out.println("  POST /api/logout      - Logout user");
        System.out.println("  GET  /api/movies      - Get all movies");
        System.out.println("  GET  /api/search?q=   - Search movies");
        System.out.println("  POST /api/favorite    - Add/remove favorite");
        System.out.println("  GET  /api/favorites   - Get user favorites");
    }

    // ============ DATABASE SETUP ============
    private static void initDatabase() {
        try {
            // Use your existing DBConnection class
            connection = DBConnection.getConnection();

            if (connection == null || connection.isClosed()) {
                throw new RuntimeException("Failed to get database connection from DBConnection class");
            }

            System.out.println("✅ Database connected via DBConnection class!");

            createTables();
            seedInitialData();

        } catch (Exception e) {
            System.err.println("❌ Database initialization error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void createTables() throws SQLException {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();

            // Users table - matches your schema.sql
            String usersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(150) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;

            // Movies table
            String moviesTable = """
                CREATE TABLE IF NOT EXISTS movies (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    year INT,
                    genre VARCHAR(255),
                    rating DECIMAL(3,1) DEFAULT 0.0,
                    description TEXT,
                    poster_url VARCHAR(500),
                    duration VARCHAR(50),
                    director VARCHAR(255),
                    cast TEXT,
                    added_by INT,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (added_by) REFERENCES users(id) ON DELETE SET NULL
                )
            """;

            // Favorites table
            String favoritesTable = """
                CREATE TABLE IF NOT EXISTS favorites (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    movie_id INT NOT NULL,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(user_id, movie_id),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
                )
            """;

            stmt.execute(usersTable);
            stmt.execute(moviesTable);
            stmt.execute(favoritesTable);
            System.out.println("✅ Database tables created/verified!");
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private static void seedInitialData() throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM movies");
            rs.next();
            int count = rs.getInt("count");

            if (count == 0) {
                System.out.println("📥 Seeding initial movie data...");

                // Insert sample movies
                String[] movieInserts = {
                        "INSERT INTO movies (title, year, genre, rating, description, poster_url, duration, director, cast) VALUES " +
                                "('Inception', 2010, 'Sci-Fi, Action', 8.8, 'A thief who steals corporate secrets through dream-sharing technology.', 'https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg', '148 min', 'Christopher Nolan', 'Leonardo DiCaprio, Joseph Gordon-Levitt')",

                        "INSERT INTO movies (title, year, genre, rating, description, poster_url, duration, director, cast) VALUES " +
                                "('The Dark Knight', 2008, 'Action, Crime, Drama', 9.0, 'Batman faces the Joker, a criminal mastermind.', 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg', '152 min', 'Christopher Nolan', 'Christian Bale, Heath Ledger')",

                        "INSERT INTO movies (title, year, genre, rating, description, poster_url, duration, director, cast) VALUES " +
                                "('Parasite', 2019, 'Comedy, Drama, Thriller', 8.6, 'A poor family schemes to become employed by a wealthy family.', 'https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg', '132 min', 'Bong Joon Ho', 'Song Kang-ho, Lee Sun-kyun')",

                        "INSERT INTO movies (title, year, genre, rating, description, poster_url, duration, director, cast) VALUES " +
                                "('Interstellar', 2014, 'Adventure, Drama, Sci-Fi', 8.6, 'A team of explorers travel through a wormhole in space.', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', '169 min', 'Christopher Nolan', 'Matthew McConaughey, Anne Hathaway')",

                        "INSERT INTO movies (title, year, genre, rating, description, poster_url, duration, director, cast) VALUES " +
                                "('The Shawshank Redemption', 1994, 'Drama', 9.3, 'Two imprisoned men bond over a number of years.', 'https://image.tmdb.org/t/p/w500/q6y0Go1tsGEsmtFryDOJo3dEmqu.jpg', '142 min', 'Frank Darabont', 'Tim Robbins, Morgan Freeman')"
                };

                for (String insert : movieInserts) {
                    stmt.executeUpdate(insert);
                }

                System.out.println("✅ 5 sample movies added!");
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    // ============ ROUTE SETUP ============
    private static void setupRoutes() {
        // Authentication
        server.createContext("/api/register", BackendServer::handleRegister);
        server.createContext("/api/login", BackendServer::handleLogin);
        server.createContext("/api/logout", BackendServer::handleLogout);
        server.createContext("/api/profile", BackendServer::handleProfile);

        // Movies
        server.createContext("/api/movies", BackendServer::handleMovies);
        server.createContext("/api/search", BackendServer::handleSearch);
        server.createContext("/api/favorites", BackendServer::handleFavorites);

        // Options handler for CORS preflight
        server.createContext("/api/", BackendServer::handleOptions);

        // Static files (for HTML/CSS/JS)
        server.createContext("/", BackendServer::handleStaticFiles);
    }

    // ============ AUTHENTICATION HANDLERS ============
    private static void handleRegister(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }

        try {
            String body = readRequestBody(exchange);
            Map<String, String> data = parseJson(body);

            String name = data.get("name");
            String email = data.get("email");
            String password = data.get("password");

            // Validation
            if (name == null || email == null || password == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing required fields\"}");
                return;
            }

            if (!isValidEmail(email)) {
                sendResponse(exchange, 400, "{\"error\": \"Invalid email format\"}");
                return;
            }

            if (password.length() < 6) {
                sendResponse(exchange, 400, "{\"error\": \"Password must be at least 6 characters\"}");
                return;
            }

            // Check if user exists
            PreparedStatement checkStmt = connection.prepareStatement(
                    "SELECT id FROM users WHERE email = ?"
            );
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                sendResponse(exchange, 409, "{\"error\": \"Email already registered\"}");
                checkStmt.close();
                return;
            }
            checkStmt.close();

            // Hash password with salt
            String salt = generateSalt();
            String passwordHash = hashPassword(password + salt);

            // Create user
            PreparedStatement insertStmt = connection.prepareStatement(
                    "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            insertStmt.setString(1, name);
            insertStmt.setString(2, email);
            insertStmt.setString(3, passwordHash + ":" + salt); // Store hash:salt

            int affectedRows = insertStmt.executeUpdate();

            if (affectedRows > 0) {
                // Get the new user ID
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                generatedKeys.next();
                int userId = generatedKeys.getInt(1);

                // Create session
                String sessionToken = generateSessionToken();
                sessions.put(sessionToken, new Session(userId, email, name));

                // Set session cookie
                exchange.getResponseHeaders().add("Set-Cookie",
                        "session=" + sessionToken + "; HttpOnly; Path=/; Max-Age=86400; SameSite=Lax");

                String response = String.format(
                        "{\"success\": true, \"message\": \"Registration successful\", " +
                                "\"user\": {\"id\": %d, \"name\": \"%s\", \"email\": \"%s\"}, " +
                                "\"session\": \"%s\"}",
                        userId, escapeJson(name), escapeJson(email), sessionToken
                );
                sendResponse(exchange, 201, response);
            } else {
                sendResponse(exchange, 500, "{\"error\": \"Failed to create user\"}");
            }

            insertStmt.close();

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                sendResponse(exchange, 409, "{\"error\": \"Email already registered\"}");
            } else {
                sendResponse(exchange, 500, "{\"error\": \"Database error: " + e.getMessage() + "\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\": \"Server error: " + e.getMessage() + "\"}");
        }
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }

        try {
            String body = readRequestBody(exchange);
            Map<String, String> data = parseJson(body);

            String email = data.get("email");
            String password = data.get("password");

            if (email == null || password == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing credentials\"}");
                return;
            }

            // Get user from database
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT id, name, email, password_hash FROM users WHERE email = ?"
            );
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHashWithSalt = rs.getString("password_hash");
                String[] parts = storedHashWithSalt.split(":");

                if (parts.length != 2) {
                    sendResponse(exchange, 500, "{\"error\": \"Invalid password format\"}");
                    stmt.close();
                    return;
                }

                String storedHash = parts[0];
                String salt = parts[1];
                String inputHash = hashPassword(password + salt);

                if (inputHash.equals(storedHash)) {
                    int userId = rs.getInt("id");
                    String userName = rs.getString("name");
                    String userEmail = rs.getString("email");

                    // Update last login
                    PreparedStatement updateStmt = connection.prepareStatement(
                            "UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE id = ?"
                    );
                    updateStmt.setInt(1, userId);
                    updateStmt.executeUpdate();
                    updateStmt.close();

                    // Create session
                    String sessionToken = generateSessionToken();
                    sessions.put(sessionToken, new Session(userId, userEmail, userName));

                    // Set session cookie
                    exchange.getResponseHeaders().add("Set-Cookie",
                            "session=" + sessionToken + "; HttpOnly; Path=/; Max-Age=86400; SameSite=Lax");

                    String response = String.format(
                            "{\"success\": true, \"message\": \"Login successful\", " +
                                    "\"user\": {\"id\": %d, \"name\": \"%s\", \"email\": \"%s\"}, " +
                                    "\"session\": \"%s\"}",
                            userId, escapeJson(userName), escapeJson(userEmail), sessionToken
                    );
                    sendResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 401, "{\"error\": \"Invalid email or password\"}");
                }
            } else {
                sendResponse(exchange, 401, "{\"error\": \"Invalid email or password\"}");
            }

            stmt.close();

        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\": \"Database error: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\": \"Server error: " + e.getMessage() + "\"}");
        }
    }

    private static void handleLogout(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }

        // Get session from cookie
        String sessionToken = getSessionTokenFromCookie(exchange);
        if (sessionToken != null) {
            sessions.remove(sessionToken);
        }

        // Clear cookie
        exchange.getResponseHeaders().add("Set-Cookie",
                "session=; HttpOnly; Path=/; Max-Age=0; SameSite=Lax");

        sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Logged out\"}");
    }

    private static void handleProfile(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }

        try {
            Session session = getSessionFromRequest(exchange);
            if (session == null) {
                sendResponse(exchange, 401, "{\"error\": \"Not authenticated\"}");
                return;
            }

            // Get user details
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT id, name, email, created_at FROM users WHERE id = ?"
            );
            stmt.setInt(1, session.userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String response = String.format(
                        "{\"success\": true, \"user\": {" +
                                "\"id\": %d, \"name\": \"%s\", \"email\": \"%s\", " +
                                "\"createdAt\": \"%s\"}}",
                        rs.getInt("id"),
                        escapeJson(rs.getString("name")),
                        escapeJson(rs.getString("email")),
                        rs.getString("created_at")
                );
                sendResponse(exchange, 200, response);
            } else {
                sendResponse(exchange, 404, "{\"error\": \"User not found\"}");
            }

            stmt.close();

        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\": \"Database error: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\": \"Server error\"}");
        }
    }

    // ============ MOVIES HANDLERS ============
    private static void handleMovies(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT m.*, u.name as added_by_name FROM movies m " +
                                "LEFT JOIN users u ON m.added_by = u.id " +
                                "ORDER BY m.rating DESC LIMIT 50"
                );

                List<Map<String, Object>> movies = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> movie = new HashMap<>();
                    movie.put("id", rs.getInt("id"));
                    movie.put("title", rs.getString("title"));
                    movie.put("year", rs.getInt("year"));
                    movie.put("genre", rs.getString("genre"));
                    movie.put("rating", rs.getDouble("rating"));
                    movie.put("description", rs.getString("description"));
                    movie.put("posterUrl", rs.getString("poster_url"));
                    movie.put("duration", rs.getString("duration"));
                    movie.put("director", rs.getString("director"));
                    movie.put("cast", rs.getString("cast"));
                    movie.put("addedBy", rs.getString("added_by_name"));
                    movies.add(movie);
                }
                stmt.close();

                String response = String.format(
                        "{\"success\": true, \"movies\": %s, \"count\": %d}",
                        convertToJson(movies), movies.size()
                );
                sendResponse(exchange, 200, response);

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\": \"Failed to fetch movies: " + e.getMessage() + "\"}");
            }
        } else {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
        }
    }

    private static void handleSearch(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }

        try {
            String query = exchange.getRequestURI().getQuery();
            String searchTerm = "";

            if (query != null && query.contains("q=")) {
                searchTerm = query.split("q=")[1].split("&")[0];
                searchTerm = java.net.URLDecoder.decode(searchTerm, StandardCharsets.UTF_8);
            }

            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM movies WHERE title LIKE ? OR genre LIKE ? OR director LIKE ? OR cast LIKE ? LIMIT 20"
            );
            String likeTerm = "%" + searchTerm + "%";
            stmt.setString(1, likeTerm);
            stmt.setString(2, likeTerm);
            stmt.setString(3, likeTerm);
            stmt.setString(4, likeTerm);

            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> movies = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> movie = new HashMap<>();
                movie.put("id", rs.getInt("id"));
                movie.put("title", rs.getString("title"));
                movie.put("year", rs.getInt("year"));
                movie.put("genre", rs.getString("genre"));
                movie.put("rating", rs.getDouble("rating"));
                movie.put("posterUrl", rs.getString("poster_url"));
                movie.put("description", rs.getString("description"));
                movies.add(movie);
            }
            stmt.close();

            String response = String.format(
                    "{\"success\": true, \"movies\": %s, \"count\": %d, \"query\": \"%s\"}",
                    convertToJson(movies), movies.size(), escapeJson(searchTerm)
            );
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\": \"Search failed: " + e.getMessage() + "\"}");
        }
    }

    private static void handleFavorites(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        Session session = getSessionFromRequest(exchange);
        if (session == null) {
            sendResponse(exchange, 401, "{\"error\": \"Not authenticated\"}");
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())) {
            try {
                PreparedStatement stmt = connection.prepareStatement(
                        "SELECT m.* FROM movies m " +
                                "JOIN favorites f ON m.id = f.movie_id " +
                                "WHERE f.user_id = ? ORDER BY f.added_at DESC"
                );
                stmt.setInt(1, session.userId);
                ResultSet rs = stmt.executeQuery();

                List<Map<String, Object>> favorites = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> movie = new HashMap<>();
                    movie.put("id", rs.getInt("id"));
                    movie.put("title", rs.getString("title"));
                    movie.put("year", rs.getInt("year"));
                    movie.put("genre", rs.getString("genre"));
                    movie.put("rating", rs.getDouble("rating"));
                    movie.put("posterUrl", rs.getString("poster_url"));
                    favorites.add(movie);
                }
                stmt.close();

                String response = String.format(
                        "{\"success\": true, \"favorites\": %s, \"count\": %d}",
                        convertToJson(favorites), favorites.size()
                );
                sendResponse(exchange, 200, response);

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\": \"Failed to get favorites\"}");
            }
        } else if ("POST".equals(exchange.getRequestMethod())) {
            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseJson(body);
                String movieIdStr = data.get("movieId");

                if (movieIdStr == null) {
                    sendResponse(exchange, 400, "{\"error\": \"Missing movieId\"}");
                    return;
                }

                int movieId = Integer.parseInt(movieIdStr);

                // Check if movie exists
                PreparedStatement checkMovie = connection.prepareStatement(
                        "SELECT id FROM movies WHERE id = ?"
                );
                checkMovie.setInt(1, movieId);
                ResultSet rs = checkMovie.executeQuery();

                if (!rs.next()) {
                    checkMovie.close();
                    sendResponse(exchange, 404, "{\"error\": \"Movie not found\"}");
                    return;
                }
                checkMovie.close();

                // Check if already favorited
                PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT id FROM favorites WHERE user_id = ? AND movie_id = ?"
                );
                checkStmt.setInt(1, session.userId);
                checkStmt.setInt(2, movieId);
                rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Already favorited, so remove it
                    PreparedStatement deleteStmt = connection.prepareStatement(
                            "DELETE FROM favorites WHERE user_id = ? AND movie_id = ?"
                    );
                    deleteStmt.setInt(1, session.userId);
                    deleteStmt.setInt(2, movieId);
                    deleteStmt.executeUpdate();
                    deleteStmt.close();
                    sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Removed from favorites\"}");
                } else {
                    // Add to favorites
                    PreparedStatement insertStmt = connection.prepareStatement(
                            "INSERT INTO favorites (user_id, movie_id) VALUES (?, ?)"
                    );
                    insertStmt.setInt(1, session.userId);
                    insertStmt.setInt(2, movieId);
                    insertStmt.executeUpdate();
                    insertStmt.close();
                    sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Added to favorites\"}");
                }
                checkStmt.close();

            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\": \"Invalid movieId\"}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\": \"Failed to update favorites: " + e.getMessage() + "\"}");
            }
        } else {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
        }
    }

    // ============ STATIC FILE HANDLER ============
    private static void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        // Try to serve file from current directory
        File file = new File("." + path);

        if (file.exists() && !file.isDirectory()) {
            String mimeType = getMimeType(path);
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } else {
            // Return index.html for SPA routing
            if (path.endsWith(".html") || !path.contains(".")) {
                File indexFile = new File("./index.html");
                if (indexFile.exists()) {
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, indexFile.length());
                    try (OutputStream os = exchange.getResponseBody();
                         FileInputStream fis = new FileInputStream(indexFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    return;
                }
            }

            // Return 404 for missing files
            String response = "{\"error\": \"File not found: " + path + "\"}";
            sendResponse(exchange, 404, response);
        }
    }

    // ============ OPTIONS HANDLER (CORS Preflight) ============
    private static void handleOptions(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
    }

    // ============ FIXED CORS HEADERS METHOD ============
    private static void setCorsHeaders(HttpExchange exchange) {
        // Get the Origin header from the request
        List<String> originHeaders = exchange.getRequestHeaders().get("Origin");
        String origin = FRONTEND_ORIGIN; // Default to your frontend

        // Check if the origin is in the allowed list (you can add more origins here)
        if (originHeaders != null && !originHeaders.isEmpty()) {
            String requestOrigin = originHeaders.get(0);
            // You can add more allowed origins here
            if (requestOrigin.equals("http://localhost:8000") ||
                    requestOrigin.equals("http://localhost:3000")) {
                origin = requestOrigin;
            }
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }

    // ============ HELPER METHODS ============
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            // Simple JSON parsing
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                    String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    // ============ FIXED JSON CONVERSION METHOD ============
    private static String convertToJson(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            sb.append("{");

            List<String> keys = new ArrayList<>(map.keySet());
            for (int j = 0; j < keys.size(); j++) {
                String key = keys.get(j);
                Object value = map.get(key);

                sb.append("\"").append(key).append("\":");

                if (value == null) {
                    sb.append("null");
                } else if (value instanceof String) {
                    sb.append("\"").append(escapeJson(value.toString())).append("\"");
                } else if (value instanceof Integer || value instanceof Long ||
                        value instanceof Double || value instanceof Float ||
                        value instanceof Short || value instanceof Byte) {
                    sb.append(value);
                } else if (value instanceof Boolean) {
                    sb.append(value.toString());
                } else {
                    // For other types (like Timestamp), convert to string
                    sb.append("\"").append(escapeJson(value.toString())).append("\"");
                }

                if (j < keys.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append("}");
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ============ FIXED SEND RESPONSE METHOD ============
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        // Set CORS headers before sending response
        setCorsHeaders(exchange);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String getSessionTokenFromCookie(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                String[] pairs = cookie.split(";");
                for (String pair : pairs) {
                    String[] keyValue = pair.trim().split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("session")) {
                        return keyValue[1];
                    }
                }
            }
        }
        return null;
    }

    private static Session getSessionFromRequest(HttpExchange exchange) {
        String token = getSessionTokenFromCookie(exchange);
        if (token == null) {
            // Also check Authorization header
            List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
        }

        if (token != null) {
            Session session = sessions.get(token);
            // Check if session expired (24 hours)
            if (session != null && System.currentTimeMillis() - session.createdAt < 24 * 60 * 60 * 1000) {
                return session;
            } else {
                sessions.remove(token); // Clean up expired session
            }
        }
        return null;
    }

    private static String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // ============ INNER CLASSES ============
    static class Session {
        int userId;
        String email;
        String name;
        long createdAt;

        Session(int userId, String email, String name) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
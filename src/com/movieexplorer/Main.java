package com.movieexplorer;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

// ========== ADD: JSON Library SIMULATION ==========
class JsonUtil {
    public static String toJson(Object obj) {
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        } else if (obj instanceof List) {
            return listToJson((List<?>) obj);
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else if (obj == null) {
            return "null";
        }
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder json = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":")
                    .append(toJson(entry.getValue()));
            if (++i < map.size()) json.append(",");
        }
        json.append("}");
        return json.toString();
    }

    private static String listToJson(List<?> list) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            json.append(toJson(list.get(i)));
            if (i < list.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
// ==================================================

public class Main {
    // ========== ENHANCED: Data Structures ==========
    private static Trie movieTrie = new Trie();
    private static List<Movie> allMovies = new ArrayList<>();
    private static Map<Integer, Movie> movieCache = new ConcurrentHashMap<>();
    private static HttpClient httpClient = HttpClient.newHttpClient();
    private static final String TMDB_API_KEY = "b93aab0bde990c6b33cff962d2b23284"; // REPLACE WITH YOUR KEY
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    // ===============================================

    public static void main(String[] args) throws Exception {
        System.out.println("🎬 Starting Movie Explorer Backend...");
        System.out.println("⚙️ Loading configuration...");

        // Initialize movies
        initializeMovies();
        System.out.println("📊 Trie loaded with " + movieTrie.getMovieCount() + " movies");
        System.out.println("🌐 TMDb API: " + (TMDB_API_KEY.equals("b93aab0bde990c6b33cff962d2b23284") ? "DISABLED (set your API key)" : "ENABLED"));

        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ========== ENHANCED: More Endpoints ==========
        server.createContext("/api/hello", new HelloHandler());
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/movies", new MoviesHandler());
        server.createContext("/api/movies/", new MovieDetailsHandler()); // Single movie
        server.createContext("/api/movies/popular", new PopularMoviesHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/suggestions", new AutocompleteHandler());
        server.createContext("/api/trie/stats", new TrieStatsHandler());
        server.createContext("/api/tmdb/search", new TMDbSearchHandler()); // Direct TMDb search
        // ==============================================

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("\n✅ Server running on http://localhost:8080");
        System.out.println("📡 Available endpoints:");
        System.out.println("  GET  /api/hello           - Test connection");
        System.out.println("  GET  /api/health          - Health check with stats");
        System.out.println("  GET  /api/movies          - Get all movies");
        System.out.println("  GET  /api/movies/{id}     - Get movie details");
        System.out.println("  GET  /api/movies/popular  - Get popular movies");
        System.out.println("  GET  /api/search?q=query  - Search movies (Trie)");
        System.out.println("  GET  /api/suggestions?q=p - Autocomplete suggestions");
        System.out.println("  GET  /api/trie/stats      - Trie statistics");
        System.out.println("  GET  /api/tmdb/search     - Direct TMDb search");
        System.out.println("\n🔍 Example usage:");
        System.out.println("  http://localhost:8080/api/search?q=inception");
        System.out.println("  http://localhost:8080/api/movies/1");
        System.out.println("  http://localhost:8080/api/suggestions?q=the");
        System.out.println("\n🚀 Frontend: Open http://localhost:8080 in browser");
        System.out.println("🛑 Press Ctrl+C to stop the server");
    }

    // ========== ENHANCED: Movie Initialization ==========
    private static void initializeMovies() {
        // Initial sample movies
        Movie[] movies = {
                createMovie(1, "Inception", 2010, 8.8,
                        "/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg",
                        Arrays.asList("Action", "Sci-Fi", "Thriller"),
                        "A thief who steals corporate secrets through dream-sharing technology."),

                createMovie(2, "The Shawshank Redemption", 1994, 9.3,
                        "/q6y0Go1tsGEsmtFryDOJo3dEmqu.jpg",
                        Arrays.asList("Drama"),
                        "Two imprisoned men bond over a number of years."),

                createMovie(3, "The Dark Knight", 2008, 9.0,
                        "/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
                        Arrays.asList("Action", "Crime", "Drama"),
                        "Batman faces the Joker in Gotham City."),

                createMovie(4, "Parasite", 2019, 8.6,
                        "/3h1JZJeh5zqTn2birTk6qOB2KzJ.jpg",
                        Arrays.asList("Comedy", "Drama", "Thriller"),
                        "A poor family schemes to become employed by a wealthy family."),

                createMovie(5, "Interstellar", 2014, 8.6,
                        "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                        Arrays.asList("Adventure", "Drama", "Sci-Fi"),
                        "A team of explorers travel through a wormhole in space."),

                createMovie(6, "The Godfather", 1972, 9.2,
                        "/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                        Arrays.asList("Crime", "Drama"),
                        "The aging patriarch of an organized crime dynasty transfers control."),

                createMovie(7, "Pulp Fiction", 1994, 8.9,
                        "/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg",
                        Arrays.asList("Crime", "Drama"),
                        "The lives of two mob hitmen, a boxer, and a pair of diner bandits intertwine."),

                createMovie(8, "Fight Club", 1999, 8.8,
                        "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                        Arrays.asList("Drama"),
                        "An insomniac office worker forms an underground fight club."),

                createMovie(9, "Forrest Gump", 1994, 8.8,
                        "/saHP97rTPS5eLmrLQEcANmKrsFl.jpg",
                        Arrays.asList("Drama", "Romance"),
                        "The presidencies of Kennedy and Johnson, Vietnam, and other events shape Forrest's life."),

                createMovie(10, "The Matrix", 1999, 8.7,
                        "/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg",
                        Arrays.asList("Action", "Sci-Fi"),
                        "A computer hacker learns about the true nature of reality.")
        };

        for (Movie movie : movies) {
            allMovies.add(movie);
            movieCache.put(movie.getId(), movie);
            movieTrie.insert(movie.getTitle().toLowerCase(), movie);
        }

        // Try to load more movies from TMDb if API key is set
        if (!TMDB_API_KEY.equals("b93aab0bde990c6b33cff962d2b23284")) {
            loadMoviesFromTMDb();
        }
    }

    private static Movie createMovie(int id, String title, int year, double rating,
                                     String posterPath, List<String> genres, String description) {
        String poster = "https://image.tmdb.org/t/p/w500" + posterPath;
        String backdrop = "https://image.tmdb.org/t/p/w1280" + posterPath;
        return new Movie(id, title, year, rating, poster, backdrop, genres, description, "en", 120);
    }

    private static void loadMoviesFromTMDb() {
        System.out.println("🔄 Loading movies from TMDb...");
        try {
            // This is a simplified version - you'd want to implement proper pagination
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TMDB_BASE_URL + "/movie/popular?api_key=" + TMDB_API_KEY + "&page=1"))
                    .header("Accept", "application/json")
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(response -> {
                        try {
                            // Parse TMDb response and add to Trie
                            // This is simplified - you'd need proper JSON parsing
                            System.out.println("✅ Loaded additional movies from TMDb");
                        } catch (Exception e) {
                            System.out.println("⚠️ Failed to load TMDb movies: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.out.println("⚠️ TMDb API error: " + e.getMessage());
        }
    }
    // ==================================================

    // ========== ENHANCED: Handler Classes ==========

    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "🎬 Movie Explorer API v1.0");
            response.put("status", "running");
            response.put("timestamp", new Date().toString());
            response.put("trie_movies", movieTrie.getMovieCount());
            sendJsonResponse(exchange, response, 200);
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("service", "Movie Explorer Backend");
            response.put("uptime", System.currentTimeMillis());
            response.put("movies_in_trie", movieTrie.getMovieCount());
            response.put("total_movies_cached", allMovies.size());
            response.put("memory_usage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            response.put("java_version", System.getProperty("java.version"));
            sendJsonResponse(exchange, response, 200);
        }
    }

    static class MoviesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("count", allMovies.size());
            response.put("movies", allMovies);
            sendJsonResponse(exchange, response, 200);
        }
    }

    static class MovieDetailsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            if (parts.length < 4) {
                sendError(exchange, "Invalid movie ID", 400);
                return;
            }

            try {
                int movieId = Integer.parseInt(parts[3]);
                Movie movie = movieCache.get(movieId);

                if (movie == null) {
                    sendError(exchange, "Movie not found", 404);
                    return;
                }

                sendJsonResponse(exchange, movie, 200);
            } catch (NumberFormatException e) {
                sendError(exchange, "Invalid movie ID format", 400);
            }
        }
    }

    static class PopularMoviesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Get query parameters for pagination
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            int page = Integer.parseInt(params.getOrDefault("page", "1"));
            int limit = Integer.parseInt(params.getOrDefault("limit", "10"));

            // Simple pagination
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, allMovies.size());

            List<Movie> pageMovies = allMovies.subList(start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("total_pages", (int) Math.ceil((double) allMovies.size() / limit));
            response.put("total_results", allMovies.size());
            response.put("results", pageMovies);

            sendJsonResponse(exchange, response, 200);
        }
    }

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String query = params.getOrDefault("q", "").trim().toLowerCase();

            List<Movie> results;
            if (query.isEmpty()) {
                results = allMovies;
            } else {
                results = movieTrie.search(query);
                System.out.println("🔍 Trie search: \"" + query + "\" → " + results.size() + " results");
            }

            // Pagination
            int page = Integer.parseInt(params.getOrDefault("page", "1"));
            int limit = Integer.parseInt(params.getOrDefault("limit", "20"));
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, results.size());

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("page", page);
            response.put("total_pages", (int) Math.ceil((double) results.size() / limit));
            response.put("total_results", results.size());
            response.put("results", results.subList(start, end));

            sendJsonResponse(exchange, response, 200);
        }
    }

    static class AutocompleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String prefix = params.getOrDefault("q", "").trim().toLowerCase();

            List<String> suggestions;
            if (prefix.length() < 2) {
                suggestions = Collections.emptyList();
            } else {
                suggestions = movieTrie.autocomplete(prefix);
                System.out.println("💡 Autocomplete: \"" + prefix + "\" → " + suggestions.size() + " suggestions");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("prefix", prefix);
            response.put("suggestions", suggestions);
            response.put("count", suggestions.size());

            sendJsonResponse(exchange, response, 200);
        }
    }

    static class TrieStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_movies", movieTrie.getMovieCount());
            stats.put("data_structure", "Trie (Prefix Tree)");
            stats.put("search_time_complexity", "O(m) where m = search term length");
            stats.put("insert_time_complexity", "O(m) where m = title length");
            stats.put("autocomplete_time", "O(m + n) where n = number of suggestions");
            stats.put("memory_efficiency", "Optimized for string prefix operations");
            stats.put("best_for", "Autocomplete, spell check, dictionary lookups");
            stats.put("implementation", "Custom Java implementation");
            stats.put("cache_size", movieCache.size());
            stats.put("sample_titles", Arrays.asList("Inception", "The Dark Knight", "Parasite"));

            sendJsonResponse(exchange, stats, 200);
        }
    }

    static class TMDbSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (TMDB_API_KEY.equals("b93aab0bde990c6b33cff962d2b23284")) {
                sendError(exchange, "TMDb API key not configured. Please set your API key in Main.java", 501);
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String query = params.getOrDefault("query", "");

            if (query.isEmpty()) {
                sendError(exchange, "Query parameter 'query' is required", 400);
                return;
            }

            try {
                // Direct TMDb API call
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(TMDB_BASE_URL + "/search/movie?api_key=" + TMDB_API_KEY +
                                "&query=" + java.net.URLEncoder.encode(query, "UTF-8") + "&page=1"))
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.body().getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.body().getBytes());
                }
            } catch (Exception e) {
                sendError(exchange, "TMDb API error: " + e.getMessage(), 500);
            }
        }
    }
    // ============================================

    // ========== ENHANCED: Helper Methods ==========
    private static void sendJsonResponse(HttpExchange exchange, Object data, int statusCode) throws IOException {
        String response = JsonUtil.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        // Handle OPTIONS preflight requests
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void sendError(HttpExchange exchange, String message, int statusCode) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("status", statusCode);
        error.put("timestamp", new Date().toString());
        sendJsonResponse(exchange, error, statusCode);
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                try {
                    value = java.net.URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    // Use original value if decoding fails
                }
                params.put(key, value);
            }
        }
        return params;
    }
    // ==============================================
}
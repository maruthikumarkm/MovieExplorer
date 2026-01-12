package com.movieexplorer;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Movie {
    private int id;
    private String title;
    private int year;
    private double rating;
    private String poster;
    private String backdrop; // Added for movie details
    private List<String> genres;
    private String description;
    private String overview; // Full description from TMDb
    private int runtime; // in minutes
    private String language; // original language
    private double popularity; // TMDb popularity score
    private Map<String, Object> additionalInfo; // For extra data

    // ========== CONSTRUCTORS ==========

    // Original constructor (for backward compatibility)
    public Movie(int id, String title, int year, double rating, String poster, List<String> genres, String description) {
        this(id, title, year, rating, poster, null, genres, description, "en", 0);
    }

    // Enhanced constructor
    public Movie(int id, String title, int year, double rating, String poster, String backdrop,
                 List<String> genres, String description, String language, int runtime) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.rating = rating;
        this.poster = poster;
        this.backdrop = backdrop;
        this.genres = genres;
        this.description = description;
        this.overview = description; // Default to description
        this.language = language;
        this.runtime = runtime;
        this.popularity = rating * 100; // Simple popularity calculation
        this.additionalInfo = new HashMap<>();
    }

    // Constructor from TMDb API data
    public static Movie fromTMDbData(Map<String, Object> tmdbData) {
        int id = ((Number) tmdbData.getOrDefault("id", 0)).intValue();
        String title = (String) tmdbData.getOrDefault("title", "Unknown");

        // Parse release year from release_date
        int year = 0;
        String releaseDate = (String) tmdbData.get("release_date");
        if (releaseDate != null && releaseDate.length() >= 4) {
            try {
                year = Integer.parseInt(releaseDate.substring(0, 4));
            } catch (NumberFormatException e) {
                year = 0;
            }
        }

        double rating = ((Number) tmdbData.getOrDefault("vote_average", 0.0)).doubleValue();
        String posterPath = (String) tmdbData.get("poster_path");
        String backdropPath = (String) tmdbData.get("backdrop_path");

        String poster = posterPath != null
                ? "https://image.tmdb.org/t/p/w500" + posterPath
                : "https://via.placeholder.com/500x750?text=No+Poster";

        String backdrop = backdropPath != null
                ? "https://image.tmdb.org/t/p/w1280" + backdropPath
                : poster; // Fallback to poster if no backdrop

        // Parse genres
        List<String> genres = parseGenres(tmdbData);

        String overview = (String) tmdbData.getOrDefault("overview", "No description available.");
        String language = (String) tmdbData.getOrDefault("original_language", "en");
        int runtime = ((Number) tmdbData.getOrDefault("runtime", 0)).intValue();
        double popularity = ((Number) tmdbData.getOrDefault("popularity", 0.0)).doubleValue();

        Movie movie = new Movie(id, title, year, rating, poster, backdrop, genres, overview, language, runtime);
        movie.setOverview(overview);
        movie.setPopularity(popularity);

        // Store original TMDb data in additionalInfo
        movie.getAdditionalInfo().put("tmdb_id", id);
        movie.getAdditionalInfo().put("tmdb_data", tmdbData);

        return movie;
    }

    private static List<String> parseGenres(Map<String, Object> tmdbData) {
        List<String> genres = new java.util.ArrayList<>();

        try {
            List<Map<String, Object>> genreList = (List<Map<String, Object>>) tmdbData.get("genres");
            if (genreList != null) {
                for (Map<String, Object> genre : genreList) {
                    String genreName = (String) genre.get("name");
                    if (genreName != null) {
                        genres.add(genreName);
                    }
                }
            }
        } catch (Exception e) {
            // If genres parsing fails, use empty list
        }

        // Fallback to hardcoded genres if none found
        if (genres.isEmpty()) {
            genres.add("Movie");
        }

        return genres;
    }

    // ========== GETTERS & SETTERS ==========

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getRating() { return rating; }
    public void setRating(double rating) {
        this.rating = rating;
        // Update popularity when rating changes
        this.popularity = rating * 100;
    }

    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }

    public String getBackdrop() { return backdrop; }
    public void setBackdrop(String backdrop) { this.backdrop = backdrop; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        if (this.overview == null || this.overview.isEmpty()) {
            this.overview = description;
        }
    }

    public String getOverview() { return overview; }
    public void setOverview(String overview) {
        this.overview = overview;
        if (this.description == null || this.description.isEmpty()) {
            this.description = overview.length() > 150
                    ? overview.substring(0, 147) + "..."
                    : overview;
        }
    }

    public int getRuntime() { return runtime; }
    public void setRuntime(int runtime) { this.runtime = runtime; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public double getPopularity() { return popularity; }
    public void setPopularity(double popularity) { this.popularity = popularity; }

    public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    // ========== UTILITY METHODS ==========

    // Convert to JSON-compatible Map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("year", year);
        map.put("rating", rating);
        map.put("poster", poster);
        map.put("backdrop", backdrop);
        map.put("genres", genres);
        map.put("description", description);
        map.put("overview", overview);
        map.put("runtime", runtime);
        map.put("language", language);
        map.put("popularity", popularity);
        map.put("additionalInfo", additionalInfo);
        return map;
    }

    // Convert to JSON string
    public String toJson() {
        return mapToJson(toMap());
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();

            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof List) {
                json.append(listToJson((List<?>) value));
            } else if (value instanceof Map) {
                json.append(mapToJson((Map<String, Object>) value));
            } else if (value == null) {
                json.append("null");
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }

            if (++i < map.size()) json.append(",");
        }
        json.append("}");
        return json.toString();
    }

    private String listToJson(List<?> list) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof String) {
                json.append("\"").append(escapeJson((String) item)).append("\"");
            } else if (item instanceof Number || item instanceof Boolean) {
                json.append(item);
            } else if (item instanceof Map) {
                json.append(mapToJson((Map<String, Object>) item));
            } else {
                json.append("\"").append(escapeJson(item.toString())).append("\"");
            }
            if (i < list.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Check if movie is valid (has required fields)
    public boolean isValid() {
        return id > 0 && title != null && !title.trim().isEmpty() && year > 1900;
    }

    // Get formatted runtime (e.g., "2h 15m")
    public String getFormattedRuntime() {
        if (runtime <= 0) return "N/A";
        int hours = runtime / 60;
        int minutes = runtime % 60;

        if (hours > 0 && minutes > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh", hours);
        } else {
            return String.format("%dm", minutes);
        }
    }

    // Get star rating (e.g., "★★★★☆")
    public String getStarRating() {
        int fullStars = (int) Math.floor(rating / 2); // Convert 0-10 to 0-5 stars
        int halfStar = (rating % 2) >= 1 ? 1 : 0;
        int emptyStars = 5 - fullStars - halfStar;

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < fullStars; i++) stars.append("★");
        if (halfStar > 0) stars.append("½");
        for (int i = 0; i < emptyStars; i++) stars.append("☆");

        return stars.toString();
    }

    // Get first genre (for UI display)
    public String getPrimaryGenre() {
        return (genres != null && !genres.isEmpty()) ? genres.get(0) : "Movie";
    }

    // Get genres as comma-separated string
    public String getGenresString() {
        if (genres == null || genres.isEmpty()) return "Movie";
        return String.join(", ", genres);
    }

    // Get short description (truncated)
    public String getShortDescription(int maxLength) {
        if (overview == null || overview.isEmpty()) {
            return description != null && !description.isEmpty()
                    ? (description.length() > maxLength
                    ? description.substring(0, maxLength - 3) + "..."
                    : description)
                    : "No description available.";
        }

        return overview.length() > maxLength
                ? overview.substring(0, maxLength - 3) + "..."
                : overview;
    }

    // Clone movie
    public Movie clone() {
        Movie clone = new Movie(id, title, year, rating, poster, backdrop,
                new java.util.ArrayList<>(genres), description, language, runtime);
        clone.setOverview(overview);
        clone.setPopularity(popularity);
        clone.setAdditionalInfo(new HashMap<>(additionalInfo));
        return clone;
    }

    @Override
    public String toString() {
        return String.format("Movie{id=%d, title='%s', year=%d, rating=%.1f, genres=%s}",
                id, title, year, rating, genres);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Movie movie = (Movie) obj;
        return id == movie.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
package com.movieexplorer;

import java.util.*;

public class Trie {
    private TrieNode root;
    private int totalMovieCount; // Cache for faster getMovieCount()
    private Map<String, Movie> movieMap; // For quick lookups by ID

    private class TrieNode {
        Map<Character, TrieNode> children;
        boolean isEndOfWord;
        List<Integer> movieIds; // Store movie IDs instead of full objects
        int wordCount; // Count of words passing through this node

        TrieNode() {
            children = new HashMap<>();
            isEndOfWord = false;
            movieIds = new ArrayList<>();
            wordCount = 0;
        }
    }

    public Trie() {
        root = new TrieNode();
        totalMovieCount = 0;
        movieMap = new HashMap<>();
    }

    // ========== ENHANCED INSERTION ==========
    // Insert a movie title into Trie
    public void insert(String title, Movie movie) {
        insert(title.toLowerCase(), movie, 1);
    }

    // Insert with weight (for ranking)
    public void insert(String title, Movie movie, int weight) {
        TrieNode current = root;
        title = title.toLowerCase().trim();

        // Store movie in map for quick lookup
        movieMap.put(String.valueOf(movie.getId()), movie);

        for (char ch : title.toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode());
            current.wordCount += weight;
        }

        if (!current.movieIds.contains(movie.getId())) {
            current.movieIds.add(movie.getId());
            totalMovieCount++;
        }
        current.isEndOfWord = true;
    }

    // Batch insert multiple movies
    public void insertAll(List<Movie> movies) {
        for (Movie movie : movies) {
            insert(movie.getTitle(), movie);
        }
        System.out.println("✅ Inserted " + movies.size() + " movies into Trie");
    }
    // ========================================

    // ========== ENHANCED SEARCH ==========
    // Search for movies with given prefix - returns sorted by relevance
    public List<Movie> search(String prefix) {
        return search(prefix, 50); // Default limit 50
    }

    public List<Movie> search(String prefix, int limit) {
        long startTime = System.nanoTime();
        prefix = prefix.toLowerCase().trim();

        if (prefix.isEmpty()) {
            return getPopularMovies(limit);
        }

        TrieNode current = root;

        // Navigate to the prefix node
        for (char ch : prefix.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) {
                return Collections.emptyList();
            }
        }

        // Collect all movie IDs from this node and its children
        Set<Integer> movieIds = new HashSet<>();
        collectMovieIds(current, movieIds);

        // Convert IDs to Movie objects
        List<Movie> results = new ArrayList<>();
        for (int id : movieIds) {
            Movie movie = movieMap.get(String.valueOf(id));
            if (movie != null) {
                results.add(movie);
            }
        }
// Make a final copy of prefix for the lambda
        final String finalPrefix = prefix;

        results.sort((m1, m2) -> {
            // First by exact match at beginning
            boolean m1Starts = m1.getTitle().toLowerCase().startsWith(finalPrefix);
            boolean m2Starts = m2.getTitle().toLowerCase().startsWith(finalPrefix);
            if (m1Starts != m2Starts) {
                return m1Starts ? -1 : 1;
            }

            // Then by rating
            int ratingCompare = Double.compare(m2.getRating(), m1.getRating());
            if (ratingCompare != 0) return ratingCompare;

            // Then by year (newer first)
            return Integer.compare(m2.getYear(), m1.getYear());
        });

        // Limit results
        if (results.size() > limit) {
            results = results.subList(0, limit);
        }

        long endTime = System.nanoTime();
        System.out.println(String.format("🔍 Trie search: \"%s\" → %d results (%.2f ms)",
                prefix, results.size(), (endTime - startTime) / 1_000_000.0));

        return results;
    }

    private void collectMovieIds(TrieNode node, Set<Integer> movieIds) {
        if (node == null) return;

        if (node.isEndOfWord) {
            movieIds.addAll(node.movieIds);
        }

        // Prioritize nodes with higher word count (more common paths)
        List<Map.Entry<Character, TrieNode>> sortedChildren = new ArrayList<>(node.children.entrySet());
        sortedChildren.sort((a, b) -> Integer.compare(b.getValue().wordCount, a.getValue().wordCount));

        for (Map.Entry<Character, TrieNode> entry : sortedChildren) {
            collectMovieIds(entry.getValue(), movieIds);
        }
    }
    // =====================================

    // ========== ENHANCED AUTOCOMPLETE ==========
    // Get autocomplete suggestions with ranking
    public List<String> autocomplete(String prefix) {
        return autocomplete(prefix, 10); // Default 10 suggestions
    }

    public List<String> autocomplete(String prefix, int limit) {
        prefix = prefix.toLowerCase().trim();

        if (prefix.length() < 2) {
            return getPopularTitles(limit);
        }

        TrieNode current = root;

        // Navigate to the prefix node
        for (char ch : prefix.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) {
                return getPopularTitles(limit);
            }
        }

        // Collect suggestions with their popularity scores
        List<Suggestion> suggestions = new ArrayList<>();
        collectSuggestions(current, new StringBuilder(prefix), suggestions);

        // Sort by popularity (wordCount) and then alphabetically
        suggestions.sort((a, b) -> {
            int scoreCompare = Integer.compare(b.score, a.score);
            if (scoreCompare != 0) return scoreCompare;
            return a.word.compareTo(b.word);
        });

        // Convert to list of strings
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, suggestions.size()); i++) {
            result.add(suggestions.get(i).word);
        }

        return result;
    }

    private static class Suggestion {
        String word;
        int score;

        Suggestion(String word, int score) {
            this.word = word;
            this.score = score;
        }
    }

    private void collectSuggestions(TrieNode node, StringBuilder prefix, List<Suggestion> suggestions) {
        if (node == null) return;

        if (node.isEndOfWord && node.wordCount > 0) {
            suggestions.add(new Suggestion(prefix.toString(), node.wordCount));
        }

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            prefix.append(entry.getKey());
            collectSuggestions(entry.getValue(), prefix, suggestions);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    // Get popular movie titles for suggestions
    private List<String> getPopularTitles(int limit) {
        List<Movie> popular = getPopularMovies(limit);
        List<String> titles = new ArrayList<>();
        for (Movie movie : popular) {
            titles.add(movie.getTitle());
        }
        return titles;
    }

    // Get popular movies based on rating
    private List<Movie> getPopularMovies(int limit) {
        List<Movie> allMovies = new ArrayList<>(movieMap.values());
        allMovies.sort((m1, m2) -> {
            // Sort by rating (descending), then by year (descending)
            int ratingCompare = Double.compare(m2.getRating(), m1.getRating());
            if (ratingCompare != 0) return ratingCompare;
            return Integer.compare(m2.getYear(), m1.getYear());
        });

        if (allMovies.size() > limit) {
            return allMovies.subList(0, limit);
        }
        return allMovies;
    }
    // ===========================================

    // ========== UTILITY METHODS ==========
    // Check if prefix exists
    public boolean startsWith(String prefix) {
        TrieNode current = root;
        prefix = prefix.toLowerCase();

        for (char ch : prefix.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) {
                return false;
            }
        }
        return true;
    }

    // Get movie by ID
    public Movie getMovieById(int id) {
        return movieMap.get(String.valueOf(id));
    }

    // Get all movies in Trie (cached)
    public List<Movie> getAllMovies() {
        return new ArrayList<>(movieMap.values());
    }

    // Get movie count (cached)
    public int getMovieCount() {
        return totalMovieCount;
    }

    // Get Trie statistics
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMovies", totalMovieCount);
        stats.put("uniqueTitles", countUniqueTitles(root));
        stats.put("totalNodes", countNodes(root));
        stats.put("averageDepth", calculateAverageDepth());
        stats.put("memoryUsage", estimateMemoryUsage());
        return stats;
    }

    private int countUniqueTitles(TrieNode node) {
        if (node == null) return 0;

        int count = node.isEndOfWord ? 1 : 0;
        for (TrieNode child : node.children.values()) {
            count += countUniqueTitles(child);
        }
        return count;
    }

    private int countNodes(TrieNode node) {
        if (node == null) return 0;

        int count = 1;
        for (TrieNode child : node.children.values()) {
            count += countNodes(child);
        }
        return count;
    }

    private double calculateAverageDepth() {
        List<Integer> depths = new ArrayList<>();
        calculateDepths(root, 0, depths);

        if (depths.isEmpty()) return 0.0;

        int sum = 0;
        for (int depth : depths) {
            sum += depth;
        }
        return sum / (double) depths.size();
    }

    private void calculateDepths(TrieNode node, int currentDepth, List<Integer> depths) {
        if (node == null) return;

        if (node.isEndOfWord) {
            depths.add(currentDepth);
        }

        for (TrieNode child : node.children.values()) {
            calculateDepths(child, currentDepth + 1, depths);
        }
    }

    private String estimateMemoryUsage() {
        // Rough estimation
        int estimatedBytes = totalMovieCount * 1000; // Approx 1KB per movie
        if (estimatedBytes < 1024) {
            return estimatedBytes + " bytes";
        } else if (estimatedBytes < 1024 * 1024) {
            return String.format("%.1f KB", estimatedBytes / 1024.0);
        } else {
            return String.format("%.1f MB", estimatedBytes / (1024.0 * 1024.0));
        }
    }

    // Clear Trie
    public void clear() {
        root = new TrieNode();
        movieMap.clear();
        totalMovieCount = 0;
    }

    // Remove a movie from Trie
    public boolean remove(String title, int movieId) {
        return remove(root, title.toLowerCase(), 0, movieId);
    }

    private boolean remove(TrieNode node, String title, int index, int movieId) {
        if (node == null) return false;

        if (index == title.length()) {
            if (node.isEndOfWord) {
                boolean removed = node.movieIds.remove(Integer.valueOf(movieId));
                if (removed) {
                    movieMap.remove(String.valueOf(movieId));
                    totalMovieCount--;

                    if (node.movieIds.isEmpty()) {
                        node.isEndOfWord = false;
                    }
                    return true;
                }
            }
            return false;
        }

        char ch = title.charAt(index);
        TrieNode child = node.children.get(ch);
        if (child == null) return false;

        boolean shouldDeleteChild = remove(child, title, index + 1, movieId);

        if (shouldDeleteChild && !child.isEndOfWord && child.children.isEmpty()) {
            node.children.remove(ch);
            return node.children.isEmpty() && !node.isEndOfWord;
        }

        return false;
    }
    // =====================================

    // ========== DEBUG/ANALYSIS METHODS ==========
    // Print Trie structure (for debugging)
    public void printTrie() {
        System.out.println("📊 Trie Structure:");
        printTrie(root, "", 0);
    }

    private void printTrie(TrieNode node, String prefix, int level) {
        if (node == null) return;

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("  ");
        }

        System.out.println(indent + "[" + prefix + "] " +
                (node.isEndOfWord ? "★ " + node.movieIds.size() + " movies" : "") +
                " (words: " + node.wordCount + ")");

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            printTrie(entry.getValue(), prefix + entry.getKey(), level + 1);
        }
    }

    // Get top N most common prefixes
    public List<String> getMostCommonPrefixes(int n) {
        List<Map.Entry<String, Integer>> prefixes = new ArrayList<>();
        collectPrefixes(root, new StringBuilder(), prefixes);

        prefixes.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(n, prefixes.size()); i++) {
            result.add(prefixes.get(i).getKey() + " (" + prefixes.get(i).getValue() + " words)");
        }
        return result;
    }

    private void collectPrefixes(TrieNode node, StringBuilder prefix, List<Map.Entry<String, Integer>> prefixes) {
        if (node == null || prefix.length() == 0) return;

        if (prefix.length() > 1 && node.wordCount > 5) { // Only prefixes with >5 words
            prefixes.add(new AbstractMap.SimpleEntry<>(prefix.toString(), node.wordCount));
        }

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            prefix.append(entry.getKey());
            collectPrefixes(entry.getValue(), prefix, prefixes);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }
    // ============================================
}
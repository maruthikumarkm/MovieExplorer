#!/bin/bash

echo "======================================"
echo "   🎬 MOVIE EXPLORER SETUP"
echo "======================================"
echo ""

# Create necessary folders
echo "📁 Creating directories..."
mkdir -p frontend
mkdir -p target/classes

echo "✅ Directory structure:"
echo "   frontend/          - Frontend HTML/CSS/JS"
echo "   src/              - Java source code"
echo "   target/classes/   - Compiled classes"
echo ""

# Check for required files
echo "🔍 Checking source files..."

if [ -f "src/com/movieexplorer/Main.java" ]; then
    echo "✅ Main.java found"
else
    echo "❌ Main.java missing! Expected at: src/com/movieexplorer/Main.java"
    exit 1
fi

if [ -f "src/com/movieexplorer/Trie.java" ]; then
    echo "✅ Trie.java found"
else
    echo "❌ Trie.java missing!"
    exit 1
fi

if [ -f "src/com/movieexplorer/Movie.java" ]; then
    echo "✅ Movie.java found"
else
    echo "❌ Movie.java missing!"
    exit 1
fi

# Check for frontend
if [ -f "frontend/index.html" ]; then
    echo "✅ index.html found in frontend/"
else
    echo "⚠️  index.html not in frontend/. You'll need to add it."
    echo "   Copy your HTML file to: frontend/index.html"
fi

echo ""
echo "======================================"
echo "   ✅ SETUP COMPLETE!"
echo "======================================"
echo ""
echo "To run the project:"
echo "1. Make launch script executable:"
echo "   chmod +x launch.sh"
echo "2. Run the project:"
echo "   ./launch.sh"
echo ""
echo "Or run manually:"
echo "   cd src"
echo "   javac com/movieexplorer/*.java"
echo "   java com.movieexplorer.Main"
echo ""

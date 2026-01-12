#!/bin/bash

echo "======================================"
echo "   🎬 MOVIE EXPLORER LAUNCHER"
echo "======================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java not found!${NC}"
    echo "Please install Java 11 or later:"
    echo "  Mac:     brew install openjdk@17"
    echo "  Ubuntu:  sudo apt install openjdk-17-jdk"
    echo "  Windows: Download from https://adoptium.net/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
echo -e "${GREEN}✅ Java ${JAVA_VERSION} found${NC}"
echo ""

# Compile
echo "🔧 Compiling Java files..."
cd src
javac -d ../target/classes com/movieexplorer/*.java

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Compilation successful!${NC}"
    echo ""

    echo "======================================"
    echo "   🚀 STARTING MOVIE EXPLORER"
    echo "======================================"
    echo ""
    echo -e "${YELLOW}📡 Backend Server:${NC}"
    echo "   URL:    http://localhost:8080"
    echo "   Status: Running"
    echo ""
    echo -e "${YELLOW}🌐 Frontend:${NC}"
    echo "   Open in browser:"
    echo "   file://$(pwd)/../frontend/index.html"
    echo ""
    echo -e "${YELLOW}📊 Test Endpoints:${NC}"
    echo "   Health:    curl http://localhost:8080/api/hello"
    echo "   Movies:    curl http://localhost:8080/api/movies"
    echo "   Search:    curl 'http://localhost:8080/api/search?q=inception'"
    echo ""
    echo -e "${YELLOW}🛑 To stop:${NC} Press Ctrl+C"
    echo "======================================"
    echo ""

    # Run the server
    cd ../target/classes
    java com.movieexplorer.Main

else
    echo -e "${RED}❌ Compilation failed!${NC}"
    echo "Check for errors above."
    exit 1
fi

#!/bin/bash
# Script to clean Maven cache and rebuild project

echo "🧹 Cleaning Maven project..."

# Clean all modules
mvn clean

# Remove Maven local repository cache for Spring Cloud (optional - uncomment if needed)
# rm -rf ~/.m2/repository/org/springframework/cloud

echo "✅ Maven clean completed!"
echo ""
echo "Next steps in IntelliJ:"
echo "1. Right-click on root pom.xml → Maven → Reload Project"
echo "2. Or: File → Invalidate Caches → Invalidate and Restart"
echo "3. Or: Maven tool window → Reload All Maven Projects (circular arrow icon)"

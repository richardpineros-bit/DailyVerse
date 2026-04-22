#!/bin/bash
# DailyVerse GitHub Setup Script
# Usage: ./setup-github.sh <your-github-username> [repo-name]

set -e

USERNAME=${1:-""}
REPO_NAME=${2:-"DailyVerse"}

if [ -z "$USERNAME" ]; then
    echo "Usage: ./setup-github.sh <your-github-username> [repo-name]"
    echo "Example: ./setup-github.sh richardpineros-bit"
    echo ""
    echo "Make sure you have a GitHub Personal Access Token with 'repo' scope"
    echo "Get one at: https://github.com/settings/tokens"
    exit 1
fi

if [ -z "$GITHUB_TOKEN" ]; then
    echo "Error: GITHUB_TOKEN environment variable not set"
    echo "Please set it: export GITHUB_TOKEN=your_token_here"
    echo "Get a token at: https://github.com/settings/tokens (repo scope)"
    exit 1
fi

echo "Creating GitHub repo: $USERNAME/$REPO_NAME"

# Create the repo on GitHub
curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/user/repos \
  -d "{\"name\":\"$REPO_NAME\",\"private\":false,\"description\":\"DailyVerse - Bible verse lock screen wallpaper app\",\"auto_init\":false}" | grep -q "201" || {
    echo "Repo may already exist or token is invalid. Continuing..."
}

# Add remote and push
echo "Adding remote and pushing code..."
git remote add origin "https://$GITHUB_TOKEN@github.com/$USERNAME/$REPO_NAME.git" 2>/dev/null || git remote set-url origin "https://$GITHUB_TOKEN@github.com/$USERNAME/$REPO_NAME.git"

git branch -M main 2>/dev/null || true
git push -u origin main

echo ""
echo "Done! Repository created at: https://github.com/$USERNAME/$REPO_NAME"
echo ""
echo "Next steps:"
echo "1. Go to https://github.com/$USERNAME/$REPO_NAME/settings/secrets/actions"
echo "2. Add your API keys as repository secrets (optional for CI):"
echo "   - UNSPLASH_ACCESS_KEY"
echo "3. Go to Actions tab and run 'Build APK' workflow"
echo "4. Download your APK from the artifacts!"

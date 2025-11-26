#!/usr/bin/env python3
"""
Generate translation contributor summary from git commits.
Default: from last tag to HEAD.
"""

import re
import sys
import os
import subprocess
import argparse
import json
import asyncio
from collections import defaultdict
from typing import Dict, List, Set, Tuple, Optional


def get_last_tag() -> str:
    """Get the most recent git tag."""
    try:
        result = subprocess.run(
            ["git", "describe", "--tags", "--abbrev=0"],
            capture_output=True,
            text=True,
            check=True,
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError:
        return None


def get_commit_list(start: str, end: str) -> List[str]:
    """Get list of commits between start and end."""
    commit_range = f"{start}..{end}" if start else end

    try:
        result = subprocess.run(
            ["git", "log", "--format=%H", "--grep=Translation", commit_range],
            capture_output=True,
            text=True,
            check=True,
        )
        commits = [
            line.strip() for line in result.stdout.strip().split("\n") if line.strip()
        ]
        return commits
    except subprocess.CalledProcessError as e:
        print(f"Error: Failed to get commit list", file=sys.stderr)
        print(f"Git error: {e.stderr}", file=sys.stderr)
        sys.exit(1)


def get_commit_message(commit_hash: str) -> str:
    """Get the full commit message."""
    try:
        result = subprocess.run(
            ["git", "show", "--format=%B", "--no-patch", commit_hash],
            capture_output=True,
            text=True,
            check=True,
        )
        return result.stdout
    except subprocess.CalledProcessError:
        return ""


def extract_github_username_from_email(email: str) -> Optional[str]:
    """Extract GitHub username from email address."""
    # GitHub noreply format: 25982450+VisionR1@users.noreply.github.com
    pattern = r"(\d+\+)?([^@]+)@users\.noreply\.github\.com"
    match = re.match(pattern, email)
    if match:
        return match.group(2)

    # Hosted Weblate noreply format
    pattern = r"([^@]+)@users\.noreply\.hosted\.weblate\.org"
    match = re.match(pattern, email)
    if match:
        return match.group(1)

    return None


# Global cache for API lookups
_email_to_username_cache = {}


async def lookup_github_user_by_email(
    session, semaphore, email: str, verbose: bool = False
) -> Tuple[str, Optional[str]]:
    """
    Look up GitHub username by email using GitHub API with async/await.
    Returns (email, username) tuple.
    """
    # Check cache first
    if email in _email_to_username_cache:
        return (email, _email_to_username_cache[email])

    # Skip obvious non-GitHub emails
    if email.endswith("@weblate.org") or "@" not in email:
        _email_to_username_cache[email] = None
        return (email, None)

    async with semaphore:
        try:
            # Use GitHub search API
            query = email.replace("@", "%40").replace("+", "%2B")
            url = f"https://api.github.com/search/users?q={query}+in:email"

            headers = {
                "Accept": "application/vnd.github.v3+json",
                "User-Agent": "Amarok-Translation-Contrib-Script",
            }

            # Add GitHub token if available from environment
            github_token = os.environ.get("GITHUB_TOKEN")
            if github_token:
                headers["Authorization"] = f"token {github_token}"

            # Use urllib for simplicity (no external dependencies)
            import urllib.request

            req = urllib.request.Request(url, headers=headers)

            # Run in executor to avoid blocking
            loop = asyncio.get_event_loop()
            response = await loop.run_in_executor(
                None, lambda: urllib.request.urlopen(req, timeout=10)
            )

            data = json.loads(response.read().decode())

            if data.get("total_count", 0) > 0:
                items = data.get("items", [])
                if items:
                    username = items[0].get("login")
                    _email_to_username_cache[email] = username
                    if verbose:
                        print(f"  API: {email} -> @{username}", file=sys.stderr)
                    return (email, username)

            _email_to_username_cache[email] = None
            return (email, None)

        except Exception as e:
            if verbose and "403" in str(e):
                print(f"  API: Rate limit or auth issue for {email}", file=sys.stderr)
            _email_to_username_cache[email] = None
            return (email, None)


async def batch_lookup_emails(
    emails: Set[str], verbose: bool = False, max_concurrent: int = 1
) -> Dict[str, Optional[str]]:
    """
    Batch lookup multiple emails concurrently with semaphore.
    Returns dict mapping email to username.
    """
    semaphore = asyncio.Semaphore(max_concurrent)

    # Create a dummy session object (we're using urllib in executor)
    session = None

    tasks = [
        lookup_github_user_by_email(session, semaphore, email, verbose)
        for email in emails
    ]

    results = await asyncio.gather(*tasks, return_exceptions=True)

    # Build result dict
    email_to_username = {}
    for result in results:
        if isinstance(result, tuple):
            email, username = result
            email_to_username[email] = username
        else:
            # Handle exceptions
            if verbose:
                print(f"  API: Error during lookup: {result}", file=sys.stderr)

    return email_to_username


def extract_co_authors(commit_msg: str) -> List[Tuple[str, str]]:
    """Extract co-authors from commit message."""
    pattern = r"Co-authored-by:\s*(.+?)\s*<(.+?)>"
    matches = re.findall(pattern, commit_msg)
    return matches


def extract_language_from_line(line: str) -> Optional[str]:
    """Extract language from a commit message line."""
    pattern = r"translation(?:\s+update)?:\s+([^(]+?)\s*\(by"
    match = re.search(pattern, line)
    if match:
        return match.group(1).strip()
    return None


def parse_translation_commits(
    commit_list: List[str], verbose: bool = False
) -> Dict[str, Set[Tuple[str, Optional[str]]]]:
    """
    Parse translation commits and organize contributors by language.
    Returns: {language: {(name, github_username), ...}}
    """
    contributors_by_lang = defaultdict(set)
    email_to_name = {}
    email_to_languages_map = defaultdict(set)
    emails_to_lookup = set()

    # First pass: collect all emails and their mappings
    for commit_hash in commit_list:
        commit_msg = get_commit_message(commit_hash)
        if not commit_msg:
            continue

        lines = commit_msg.split("\n")
        email_to_languages = defaultdict(set)

        # Map emails to languages
        for line in lines:
            language = extract_language_from_line(line)
            if language:
                email_pattern = r"<(.+?)>"
                email_match = re.search(email_pattern, line)
                if email_match:
                    email = email_match.group(1)
                    email_to_languages[email].add(language)

        # Extract co-authors
        co_authors = extract_co_authors(commit_msg)

        for name, email in co_authors:
            # Skip bot accounts
            if email in ["hosted@weblate.org", "noreply-addon-languages@weblate.org"]:
                continue

            email_to_name[email] = name
            languages = email_to_languages.get(email, set())
            for lang in languages:
                email_to_languages_map[email].add(lang)

            # Check if we need to lookup this email
            github_username = extract_github_username_from_email(email)
            if not github_username and email not in _email_to_username_cache:
                emails_to_lookup.add(email)

    # Batch lookup all emails concurrently
    if emails_to_lookup:
        if verbose:
            print(
                f"Looking up {len(emails_to_lookup)} emails via GitHub API...",
                file=sys.stderr,
            )

        email_to_username_results = asyncio.run(
            batch_lookup_emails(emails_to_lookup, verbose=verbose)
        )

    # Second pass: build contributors by language
    for email, name in email_to_name.items():
        # Try to extract username from email format first
        github_username = extract_github_username_from_email(email)

        # If not found, use cached result from API lookup
        if not github_username:
            github_username = _email_to_username_cache.get(email)

        languages = email_to_languages_map.get(email, set())

        for language in languages:
            contributors_by_lang[language].add((name, github_username))

    return contributors_by_lang


def format_output(
    contributors_by_lang: Dict[str, Set[Tuple[str, Optional[str]]]],
) -> str:
    """Format output as a single line for commit messages."""
    if not contributors_by_lang:
        return ""

    parts = []

    # Sort languages alphabetically
    for language in sorted(contributors_by_lang.keys()):
        contributors = contributors_by_lang[language]

        # Sort contributors and format mentions
        mentions = []
        for name, gh_username in sorted(contributors, key=lambda x: x[0]):
            if gh_username:
                mentions.append(f"@{gh_username}")
            else:
                mentions.append(name)

        if mentions:
            contributor_str = ", ".join(mentions)
            parts.append(f"{language} (by {contributor_str})")

    if not parts:
        return ""

    return "Update community translations: " + ", ".join(parts)


def main():
    parser = argparse.ArgumentParser(
        description="Generate translation contributor summary for commit messages",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # From last tag to HEAD (default)
  %(prog)s
  
  # From specific tag/commit to HEAD
  %(prog)s --start v1.0.0
  
  # Between two tags/commits
  %(prog)s --start v1.0.0 --end v2.0.0
  
  # From specific commit
  %(prog)s --start abc123
  
  # Verbose mode
  %(prog)s --verbose
        """,
    )

    parser.add_argument("--start", help="Start commit/tag (default: last tag)")

    parser.add_argument("--end", default="HEAD", help="End commit/tag (default: HEAD)")

    parser.add_argument(
        "--verbose",
        "-v",
        action="store_true",
        help="Verbose output (shows API lookups and processing details). Set GITHUB_TOKEN environment variable for higher API rate limits.",
    )

    args = parser.parse_args()

    # Determine start point
    start = args.start
    if not start:
        start = get_last_tag()
        if not start:
            print("Error: No tags found and no --start specified", file=sys.stderr)
            sys.exit(1)
        if args.verbose:
            print(f"Using last tag as start: {start}", file=sys.stderr)

    if args.verbose:
        print(f"Processing commits from {start} to {args.end}...", file=sys.stderr)

    # Get commit list
    commit_list = get_commit_list(start, args.end)

    if not commit_list:
        if args.verbose:
            print("No translation commits found in range", file=sys.stderr)
        sys.exit(0)

    if args.verbose:
        print(f"Found {len(commit_list)} translation commit(s)", file=sys.stderr)

    # Parse commits
    contributors_by_lang = parse_translation_commits(commit_list, verbose=args.verbose)

    if not contributors_by_lang:
        if args.verbose:
            print("No contributors found", file=sys.stderr)
        sys.exit(0)

    # Format and print output
    output = format_output(contributors_by_lang)
    if output:
        print(output)
    elif args.verbose:
        print("No output generated", file=sys.stderr)


if __name__ == "__main__":
    main()

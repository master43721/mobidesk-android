#!/usr/bin/env python3
"""
MobiDesk — FreeRDP Native Dependency Fetcher and Checksum Verifier
Ensures 100% reproducible, hash-verified offline builds for native C/C++ dependencies.
Prevents external live-download network flakes (e.g. HTTP 504 Gateway Timeout) during CI.
"""

import argparse
import hashlib
import os
import subprocess
import sys
import time
import urllib.request
import urllib.error

DEPS = [
    {
        "name": "openssl",
        "version": "openssl-4.0.1",
        "hash": "2db3f3a0d6ea4b59e1f094ace2c8cd536dffb87cdc39084c5afa1e6f7f37dd09",
        "filename": "openssl-4.0.1.tar.gz",
        "urls": [
            "https://github.com/openssl/openssl/releases/download/openssl-4.0.1/openssl-4.0.1.tar.gz",
        ],
    },
    {
        "name": "cjson",
        "version": "v1.7.19",
        "hash": "7fa616e3046edfa7a28a32d5f9eacfd23f92900fe1f8ccd988c1662f30454562",
        "filename": "v1.7.19.tar.gz",
        "urls": [
            "https://github.com/DaveGamble/cJSON/archive/refs/tags/v1.7.19.tar.gz",
        ],
    },
    {
        "name": "webp",
        "version": "1.6.0",
        "hash": "e4ab7009bf0629fd11982d4c2aa83964cf244cffba7347ecd39019a9e38c4564",
        "filename": "libwebp-1.6.0.tar.gz",
        "urls": [
            "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-1.6.0.tar.gz",
        ],
    },
    {
        "name": "libpng",
        "version": "v1.6.58",
        "hash": "a9d4df463d36a6e5f9c29bd6f4967312d17e996c1854f3511f833924eb1993cf",
        "filename": "v1.6.58.tar.gz",
        "urls": [
            "https://github.com/pnggroup/libpng/archive/refs/tags/v1.6.58.tar.gz",
        ],
    },
    {
        "name": "jpeg",
        "version": "3.1.4.1",
        "hash": "ecae8008e2cc9ade2f2c1bb9d5e6d4fb73e7c433866a056bd82980741571a022",
        "filename": "libjpeg-turbo-3.1.4.1.tar.gz",
        "urls": [
            "https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/3.1.4.1/libjpeg-turbo-3.1.4.1.tar.gz",
        ],
    },
    {
        "name": "opus",
        "version": "1.5.2",
        "hash": "65c1d2f78b9f2fb20082c38cbe47c951ad5839345876e46941612ee87f9a7ce1",
        "filename": "opus-1.5.2.tar.gz",
        "urls": [
            "https://github.com/xiph/opus/releases/download/v1.5.2/opus-1.5.2.tar.gz",
        ],
    },
    {
        "name": "uriparser",
        "version": "1.0.2",
        "hash": "7b2496f9622b4b201186a3d159d278605f1fb7262402a10cc1c0824889798b00",
        "filename": "uriparser-1.0.2.tar.xz",
        "urls": [
            "https://github.com/uriparser/uriparser/releases/download/uriparser-1.0.2/uriparser-1.0.2.tar.xz",
        ],
    },
    {
        "name": "ffmpeg",
        "version": "n8.1",
        "hash": "dd308201bb1239a1b73185f80c6b4121f4efdfa424a009ce544fd00bf736bb2e",
        "filename": "n8.1.tar.gz",
        "urls": [
            "https://github.com/FFmpeg/FFmpeg/archive/refs/tags/n8.1.tar.gz",
        ],
    },
]

OPENH264_TAG = "v2.6.0"
OPENH264_REPO = "https://github.com/cisco/openh264.git"


def compute_sha256(filepath: str) -> str:
    sha = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest().lower()


def download_with_retry(urls: list, dest_path: str, expected_hash: str, max_retries: int = 5) -> bool:
    tmp_path = dest_path + ".tmp"
    headers = {"User-Agent": "MobiDesk-CI-Fetcher/1.0"}

    for attempt in range(1, max_retries + 1):
        for url in urls:
            try:
                print(f"  [Attempt {attempt}/{max_retries}] Downloading {url} ...")
                req = urllib.request.Request(url, headers=headers)
                with urllib.request.urlopen(req, timeout=60) as resp:
                    with open(tmp_path, "wb") as out_f:
                        while chunk := resp.read(65536):
                            out_f.write(chunk)

                actual_hash = compute_sha256(tmp_path)
                if actual_hash == expected_hash:
                    os.replace(tmp_path, dest_path)
                    print(f"  --> Verification PASSED (sha256: {actual_hash[:16]}...)")
                    return True
                else:
                    print(f"  ❌ Hash mismatch for {url}: expected {expected_hash}, got {actual_hash}")
                    if os.path.exists(tmp_path):
                        os.remove(tmp_path)
            except Exception as ex:
                print(f"  ⚠️ Download error from {url}: {ex}")
                if os.path.exists(tmp_path):
                    os.remove(tmp_path)

        backoff = 2 ** attempt
        print(f"  Waiting {backoff}s before retry ...")
        time.sleep(backoff)

    return False


def main():
    parser = argparse.ArgumentParser(description="Fetch and verify FreeRDP native dependencies.")
    parser.add_argument("--cache-dir", default=".freerdp-deps-cache", help="Path to cache directory")
    parser.add_argument("--verify-only", action="store_true", help="Only verify existing cache without downloading")
    args = parser.parse_args()

    cache_dir = os.path.abspath(args.cache_dir)
    os.makedirs(cache_dir, exist_ok=True)
    print(f"Native Dependency Cache: {cache_dir}")

    all_passed = True

    for dep in DEPS:
        name = dep["name"]
        filename = dep["filename"]
        expected_hash = dep["hash"].lower()
        filepath = os.path.join(cache_dir, filename)

        print(f"\nChecking {name} ({filename}) ...")

        if os.path.isfile(filepath):
            actual_hash = compute_sha256(filepath)
            if actual_hash == expected_hash:
                print(f"  ✅ Cache HIT: {filename} verified (sha256: {actual_hash[:16]}...)")
                continue
            else:
                print(f"  ⚠️ Cache CORRUPT: {filename} hash mismatch ({actual_hash} != {expected_hash}). Removing.")
                os.remove(filepath)

        if args.verify_only:
            print(f"  ❌ MISSING: {filename} not present in cache (--verify-only set)")
            all_passed = False
            continue

        success = download_with_retry(dep["urls"], filepath, expected_hash)
        if not success:
            print(f"  ❌ FAILED to download and verify {name} after multiple retries!")
            all_passed = False

    # Check / Clone openh264 git repository
    h264_dir = os.path.join(cache_dir, "openh264")
    print(f"\nChecking openh264 ({OPENH264_TAG}) ...")
    if os.path.isdir(os.path.join(h264_dir, ".git")):
        print(f"  ✅ Cache HIT: openh264 repo present at {h264_dir}")
    elif not args.verify_only:
        print(f"  Cloning {OPENH264_REPO} ({OPENH264_TAG}) into {h264_dir} ...")
        try:
            subprocess.check_call([
                "git", "clone", "--depth", "1", "--branch", OPENH264_TAG,
                OPENH264_REPO, h264_dir
            ])
            print(f"  ✅ Successfully cloned openh264 {OPENH264_TAG}")
        except subprocess.CalledProcessError as e:
            print(f"  ❌ Failed to clone openh264: {e}")
            all_passed = False
    else:
        print(f"  ❌ MISSING: openh264 repository not present in cache (--verify-only set)")
        all_passed = False

    if not all_passed:
        print("\n❌ One or more native dependencies failed verification.")
        return 1

    print("\n✅ All native dependencies verified and ready for reproducible build.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

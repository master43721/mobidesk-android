#!/usr/bin/env python3
"""
Generate shadowed CMake dependency files in cmake/freerdp/
These files override upstream FreeRDP External*.cmake files to use local
pinned, hash-verified cached archives from FREERDP_EXTERNAL_CACHE_DIR when available,
falling back to upstream URLs if the cache is empty.
"""

import os
import re

CACHE_RULES = {
    'ExternalLibPNG.cmake': {
        'var': 'PNG_ARCHIVE_URL',
        'default': 'https://github.com/pnggroup/libpng/archive/refs/tags/${PNG_VERSION}.tar.gz',
        'files': ['v${PNG_VERSION}.tar.gz', '${PNG_VERSION}.tar.gz'],
        'pattern': r'URL\s+https://github\.com/pnggroup/libpng/archive/refs/tags/\$\{PNG_VERSION\}\.tar\.gz',
    },
    'ExternalJpeg.cmake': {
        'var': 'JPEG_ARCHIVE_URL',
        'default': 'https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/${JPEG_VERSION}/libjpeg-turbo-${JPEG_VERSION}.tar.gz',
        'files': ['libjpeg-turbo-${JPEG_VERSION}.tar.gz'],
        'pattern': r'URL\s+https://github\.com/libjpeg-turbo/libjpeg-turbo/releases/download/\$\{JPEG_VERSION\}/libjpeg-turbo-\$\{JPEG_VERSION\}\.tar\.gz',
    },
    'ExternalOpenSSL.cmake': {
        'var': 'OPENSSL_ARCHIVE_URL',
        'default': 'https://github.com/openssl/openssl/releases/download/${OPENSSL_VERSION}/${OPENSSL_VERSION}.tar.gz',
        'files': ['${OPENSSL_VERSION}.tar.gz'],
        'pattern': r'URL\s+https://github\.com/openssl/openssl/releases/download/\$\{OPENSSL_VERSION\}/\$\{OPENSSL_VERSION\}\.tar\.gz',
    },
    'ExternalFFmpeg.cmake': {
        'var': 'FFMPEG_ARCHIVE_URL',
        'default': 'https://github.com/FFmpeg/FFmpeg/archive/refs/tags/${FFMPEG_VERSION}.tar.gz',
        'files': ['${FFMPEG_VERSION}.tar.gz'],
        'pattern': r'URL\s+https://github\.com/FFmpeg/FFmpeg/archive/refs/tags/\$\{FFMPEG_VERSION\}\.tar\.gz',
    },
    'ExternalOpus.cmake': {
        'var': 'OPUS_ARCHIVE_URL',
        'default': 'https://github.com/xiph/opus/releases/download/v${OPUS_VERSION}/opus-${OPUS_VERSION}.tar.gz',
        'files': ['opus-${OPUS_VERSION}.tar.gz'],
        'pattern': r'URL\s+https://github\.com/xiph/opus/releases/download/v\$\{OPUS_VERSION\}/opus-\$\{OPUS_VERSION\}\.tar\.gz',
    },
    'ExternalCJSON.cmake': {
        'var': 'CJSON_ARCHIVE_URL',
        'default': 'https://github.com/DaveGamble/cJSON/archive/refs/tags/${CJSON_VERSION}.tar.gz',
        'files': ['${CJSON_VERSION}.tar.gz'],
        'pattern': r'URL\s+https://github\.com/DaveGamble/cJSON/archive/refs/tags/\$\{CJSON_VERSION\}\.tar\.gz',
    },
    'ExternalUriparser.cmake': {
        'var': 'URIPARSER_ARCHIVE_URL',
        'default': 'https://github.com/uriparser/uriparser/releases/download/uriparser-${URIPARSER_VERSION}/uriparser-${URIPARSER_VERSION}.tar.xz',
        'files': ['uriparser-${URIPARSER_VERSION}.tar.xz'],
        'pattern': r'URL\s+https://github\.com/uriparser/uriparser/releases/download/uriparser-\$\{URIPARSER_VERSION\}/uriparser-\$\{URIPARSER_VERSION\}\.tar\.xz',
    },
    'ExternalWebP.cmake': {
        'var': 'WEBP_ARCHIVE_URL',
        'default': 'https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-${WEBP_VERSION}.tar.gz',
        'files': ['libwebp-${WEBP_VERSION}.tar.gz'],
        'pattern': r'URL\s+https://storage\.googleapis\.com/downloads\.webmproject\.org/releases/webp/libwebp-\$\{WEBP_VERSION\}\.tar\.gz',
    },
}

def main():
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    out_dir = os.path.join(root_dir, "cmake", "freerdp")
    os.makedirs(out_dir, exist_ok=True)

    for fname, rule in CACHE_RULES.items():
        src_file = os.path.join(root_dir, "third_party", "FreeRDP", "client", "Android", "cmake", fname)
        dst_file = os.path.join(out_dir, fname)
        with open(src_file, "r", encoding="utf-8") as f:
            content = f.read()

        var = rule['var']
        default_url = rule['default']
        files = rule['files']
        is_git = rule.get('is_git', False)

        setup_lines = [f'set({var} "{default_url}")']
        for i, fn in enumerate(files):
            condition = "if" if i == 0 else "elseif"
            check_path = fn.replace("/.git", "") if is_git else fn
            setup_lines.append(f'{condition}(DEFINED FREERDP_EXTERNAL_CACHE_DIR AND EXISTS "${{FREERDP_EXTERNAL_CACHE_DIR}}/{fn}")')
            setup_lines.append(f'  set({var} "${{FREERDP_EXTERNAL_CACHE_DIR}}/{check_path}")')
        setup_lines.append("endif()\n")
        setup_block = "\n".join(setup_lines)

        # Insert setup_block right before ExternalProject_Add
        ep_index = content.find("ExternalProject_Add(")
        if ep_index == -1:
            raise ValueError(f"ExternalProject_Add not found in {src_file}")
        
        new_content = content[:ep_index] + setup_block + content[ep_index:]
        
        # Replace the URL or GIT_REPOSITORY line with variable reference
        repl = f'GIT_REPOSITORY "${{{var}}}"' if is_git else f'URL "${{{var}}}"'
        new_content, count = re.subn(rule['pattern'], repl, new_content)
        if count != 1:
            raise ValueError(f"Pattern '{rule['pattern']}' not matched once in {src_file} (count={count})")

        with open(dst_file, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Generated {dst_file}")

if __name__ == "__main__":
    main()

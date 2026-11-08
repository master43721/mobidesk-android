include(ExternalProject)
include(DepVersions)

set(WEBP_ARCHIVE_URL "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-${WEBP_VERSION}.tar.gz")
if(DEFINED FREERDP_EXTERNAL_CACHE_DIR AND EXISTS "${FREERDP_EXTERNAL_CACHE_DIR}/libwebp-${WEBP_VERSION}.tar.gz")
  set(WEBP_ARCHIVE_URL "${FREERDP_EXTERNAL_CACHE_DIR}/libwebp-${WEBP_VERSION}.tar.gz")
endif()
ExternalProject_Add(
  webp
  DOWNLOAD_EXTRACT_TIMESTAMP OFF
  SOURCE_DIR ${CMAKE_SOURCE_DIR}/external/webp
  URL "${WEBP_ARCHIVE_URL}"
  URL_HASH ${WEBP_HASH}
  LIST_SEPARATOR |
  CMAKE_ARGS ${ANDROID_CMAKE_ARGS}
             -DWEBP_HAVE_TIFF:BOOL=OFF
             -DWEBP_BUILD_CWEBP:BOOL=OFF
             -DWEBP_BUILD_DWEBP:BOOL=OFF
             -DWEBP_BUILD_GIF2WEBP:BOOL=OFF
             -DWEBP_BUILD_IMG2WEBP:BOOL=OFF
             -DWEBP_BUILD_VWEBP:BOOL=OFF
             -DWEBP_BUILD_ANIM_UTILS:BOOL=OFF
             -DWEBP_BUILD_WEBPINFO:BOOL=OFF
             -DWEBP_BUILD_WEBPMUX:BOOL=OFF
             -DWEBP_BUILD_EXTRAS:BOOL=OFF
)

if(WITH_JPEG)
  add_dependencies(webp jpeg)
endif()
if(WITH_PNG)
  add_dependencies(webp libpng)
endif()

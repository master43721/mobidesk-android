include(ExternalProject)
include(DepVersions)

set(PNG_ARCHIVE_URL "https://github.com/pnggroup/libpng/archive/refs/tags/${PNG_VERSION}.tar.gz")
if(DEFINED FREERDP_EXTERNAL_CACHE_DIR AND EXISTS "${FREERDP_EXTERNAL_CACHE_DIR}/v${PNG_VERSION}.tar.gz")
  set(PNG_ARCHIVE_URL "${FREERDP_EXTERNAL_CACHE_DIR}/v${PNG_VERSION}.tar.gz")
elseif(DEFINED FREERDP_EXTERNAL_CACHE_DIR AND EXISTS "${FREERDP_EXTERNAL_CACHE_DIR}/${PNG_VERSION}.tar.gz")
  set(PNG_ARCHIVE_URL "${FREERDP_EXTERNAL_CACHE_DIR}/${PNG_VERSION}.tar.gz")
endif()
ExternalProject_Add(
  libpng SOURCE_DIR ${CMAKE_SOURCE_DIR}/external/libpng
  URL "${PNG_ARCHIVE_URL}" URL_HASH ${PNG_HASH}
  DOWNLOAD_EXTRACT_TIMESTAMP OFF LIST_SEPARATOR | CMAKE_ARGS ${ANDROID_CMAKE_ARGS} -DPNG_TESTS:BOOL=OFF
                                                             -DPNG_TOOLS:BOOL=OFF
)

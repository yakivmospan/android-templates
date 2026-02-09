package com.yakivmospan.templates.core.data

enum class UpdateStrategy {
    ALWAYS_FETCH,
    ALWAYS_CACHED,
    TRY_FETCH_ELSE_CACHED,
    TRY_CACHED_ELSE_FETCH
}
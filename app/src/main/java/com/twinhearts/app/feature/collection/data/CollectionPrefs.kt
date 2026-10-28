package com.twinhearts.app.feature.collection.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Favourites that the web keeps in localStorage (`col_photo_favs`): photos and Library voice items. */
@Singleton
class CollectionPrefs @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("collection_prefs", Context.MODE_PRIVATE)

    fun photoFavorites(): Set<String> = prefs.getStringSet(KEY_PHOTOS, emptySet()).orEmpty().toSet()
    fun libraryFavorites(): Set<String> = prefs.getStringSet(KEY_LIBRARY, emptySet()).orEmpty().toSet()

    fun setPhotoFavorites(ids: Set<String>) = prefs.edit().putStringSet(KEY_PHOTOS, ids).apply()
    fun setLibraryFavorites(ids: Set<String>) = prefs.edit().putStringSet(KEY_LIBRARY, ids).apply()

    private companion object {
        const val KEY_PHOTOS = "photo_favs"
        const val KEY_LIBRARY = "library_favs"
    }
}

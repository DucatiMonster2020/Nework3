package ru.netology.nework.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Post

class PostPagingSource(
    private val apiService: ApiService
) : PagingSource<Long, Post>() {
    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        return try {
            // TODO: реализовать пагинацию
            LoadResult.Page(emptyList(), null, null)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Post>): Long? = null
}
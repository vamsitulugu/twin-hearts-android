package com.twinhearts.app.feature.study.di

import com.twinhearts.app.feature.study.data.StudyRepository
import com.twinhearts.app.feature.study.data.StudyRepositoryImpl
import com.twinhearts.app.feature.study.data.remote.StudyApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StudyModule {

    @Binds
    abstract fun bindStudyRepository(impl: StudyRepositoryImpl): StudyRepository

    companion object {
        @Provides
        @Singleton
        fun provideStudyApi(retrofit: Retrofit): StudyApi = retrofit.create(StudyApi::class.java)
    }
}

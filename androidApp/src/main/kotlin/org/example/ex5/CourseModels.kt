package org.example.ex5

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    val description: String = ""
)

@Serializable
data class Course(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("categoryId") val categoryId: String = "",
    val instructor: String = "",
    val price: Double = 0.0,
    val duration: Int = 0,
    val thumbnail: String = "",
    @SerialName("trailerUrl") val trailerUrl: String = "",
    @SerialName("createdAt") val createdAt: String = ""
)

@Serializable
data class CategoryRequest(
    val name: String,
    val description: String
)

@Serializable
data class CourseRequest(
    val title: String,
    val description: String,
    @SerialName("categoryId") val categoryId: String,
    val instructor: String,
    val price: Double,
    val duration: Int,
    val thumbnail: String,
    @SerialName("trailerUrl") val trailerUrl: String,
    @SerialName("createdAt") val createdAt: String
)

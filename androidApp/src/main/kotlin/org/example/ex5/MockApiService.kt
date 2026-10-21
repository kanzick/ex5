package org.example.ex5

import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object MockApiConfig {
    const val BASE_URL = "https://YOUR_PROJECT_ID.mockapi.io/api/v1"
}

class MockApiService(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
) {
    suspend fun getCategories(): List<Category> {
        val response = makeRequest("GET", "/categories")
        return json.decodeFromString(response)
    }

    suspend fun createCategory(category: CategoryRequest): Category {
        val response = makeRequest("POST", "/categories", json.encodeToString(CategoryRequest.serializer(), category))
        return json.decodeFromString(response)
    }

    suspend fun updateCategory(id: String, category: CategoryRequest): Category {
        val response = makeRequest("PUT", "/categories/$id", json.encodeToString(CategoryRequest.serializer(), category))
        return json.decodeFromString(response)
    }

    suspend fun deleteCategory(id: String) {
        makeRequest("DELETE", "/categories/$id")
    }

    suspend fun getCourses(): List<Course> {
        val response = makeRequest("GET", "/courses")
        return json.decodeFromString(response)
    }

    suspend fun getCourse(id: String): Course {
        val response = makeRequest("GET", "/courses/$id")
        return json.decodeFromString(response)
    }

    suspend fun createCourse(course: CourseRequest): Course {
        val response = makeRequest("POST", "/courses", json.encodeToString(CourseRequest.serializer(), course))
        return json.decodeFromString(response)
    }

    suspend fun updateCourse(id: String, course: CourseRequest): Course {
        val response = makeRequest("PUT", "/courses/$id", json.encodeToString(CourseRequest.serializer(), course))
        return json.decodeFromString(response)
    }

    suspend fun deleteCourse(id: String) {
        makeRequest("DELETE", "/courses/$id")
    }

    private fun makeRequest(method: String, path: String, body: String? = null): String {
        val url = URL("${MockApiConfig.BASE_URL}$path")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 20000
        connection.readTimeout = 30000
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.doInput = true

        if (!body.isNullOrEmpty()) {
            connection.doOutput = true
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: ""
        connection.disconnect()

        if (statusCode !in 200..299) {
            throw IOException("Request failed: $method $path | status=$statusCode | body=$response")
        }

        return response
    }
}

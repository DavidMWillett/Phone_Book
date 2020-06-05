package phonebook

import java.io.File

fun main() {
    val directory = Directory()
    val names = loadNames()

    var found = 0
    println("Start searching...")
    val startTime = System.currentTimeMillis()
    names.forEach {
        val number = directory.linearSearch(it)
        if (number != null) found++
    }
    val endTime = System.currentTimeMillis()
    val duration = endTime - startTime
    print("Found $found / 500 entries. ")
    val minutes = duration / 60_000
    val seconds = duration / 1_000 % 60
    val millis = duration % 1_000
    println("Time taken: $minutes min. $seconds sec. $millis ms.")
}

fun loadNames(): List<String> {
    val filePath = "C:\\Users\\david\\IdeaProjects\\Data\\find.txt"
    val names = mutableListOf<String>()
    File(filePath).forEachLine { names.add(it) }
    return names
}

class Directory {
    private val filePath = "C:\\Users\\david\\IdeaProjects\\Data\\directory.txt"
    private val records = load()

    private fun load(): List<Record> {
        val records = mutableListOf<Record>()
        File(filePath).forEachLine {
            val (number, name) = it.split(" ", limit = 2)
            records.add(Record(number, name))
        }
        return records
    }

    fun linearSearch(name: String): String? {
        for (record in records) {
            if (record.name == name) return record.number
        }
        return null
    }

    data class Record(val number: String, val name: String)
}

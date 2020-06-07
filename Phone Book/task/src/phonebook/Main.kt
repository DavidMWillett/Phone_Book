package phonebook

import java.io.File
import java.util.Collections
import kotlin.math.sqrt

fun main() {
    val directory = loadDirectory()
    val namesToFind = loadNamesToFind()

    linearSearch(directory, namesToFind)
    jumpSearch(directory, namesToFind)
    binarySearch(directory, namesToFind)
}

fun loadDirectory(): MutableList<Record> {
    val filePath = "C:\\Users\\david\\IdeaProjects\\Data\\directory.txt"
    val records = mutableListOf<Record>()
    File(filePath).forEachLine {
        val (number, name) = it.split(" ", limit = 2)
        records.add(Record(number, name))
    }
    return records
}

fun loadNamesToFind(): List<String> {
    val filePath = "C:\\Users\\david\\IdeaProjects\\Data\\find.txt"
    val names = mutableListOf<String>()
    File(filePath).forEachLine { names.add(it) }
    return names
}

fun linearSearch(directory: List<Record>, namesToFind: List<String>): List<Record> {
    println("Start searching (linear search)...")
    val foundRecords = LinearSearcher.search(directory, namesToFind)
    print("Found ${foundRecords.size} / ${namesToFind.size} entries. ")
    println("Time taken: ${LinearSearcher.elapsedTime.toTime()}\n")
    return foundRecords
}

fun jumpSearch(directory: MutableList<Record>, namesToFind: List<String>): List<Record> {
    println("Start searching (bubble sort + jump search)...")
    val foundRecords = JumpSearcher.search(directory, namesToFind, 10 * LinearSearcher.elapsedTime)
    print("Found ${foundRecords.size} / ${namesToFind.size} entries. ")
    println("Time taken: ${JumpSearcher.elapsedTime.toTime()}")
    print("Sorting time: ${JumpSearcher.sortingTime.toTime()}")
    if (JumpSearcher.stopped) print(" - STOPPED, moved to linear search")
    println()
    println("Searching time: ${JumpSearcher.searchingTime.toTime()}\n")
    return foundRecords
}

fun binarySearch(directory: MutableList<Record>, namesToFind: List<String>): List<Record> {
    println("Start searching (quick sort + binary search)...")
    val foundRecords = BinarySearcher.search(directory, namesToFind)
    print("Found ${foundRecords.size} / ${namesToFind.size} entries. ")
    println("Time taken: ${BinarySearcher.elapsedTime.toTime()}")
    println("Sorting time: ${BinarySearcher.sortingTime.toTime()}")
    println("Searching time: ${BinarySearcher.searchingTime.toTime()}\n")
    return foundRecords
}

fun Long.toTime() = "${this / 60_000} min. ${this / 1_000 % 60} sec. ${this % 1_000} ms."

object LinearSearcher {
    var elapsedTime: Long = 0

    fun search(records: List<Record>, namesToFind: List<String>): List<Record> {
        val found = mutableListOf<Record>()
        val startTime = System.currentTimeMillis()
        for (name in namesToFind) {
            for (record in records) {
                if (record.name == name) {
                    found.add(record)
                    break
                }
            }
        }
        elapsedTime = System.currentTimeMillis() - startTime
        return found
    }
}

object JumpSearcher {
    var elapsedTime = 0L
    var sortingTime = 0L
    var searchingTime = 0L
    var stopped = false

    fun search(records: MutableList<Record>, namesToFind: List<String>, timeLimit: Long): List<Record> {
        val startTime = System.currentTimeMillis()
        stopped = bubbleSort(records, timeLimit)
        sortingTime = System.currentTimeMillis() - startTime
        val found = (if (!stopped) {
            searchInSorted(records, namesToFind)
        } else {
            LinearSearcher.search(records, namesToFind)
        })
        elapsedTime = System.currentTimeMillis() - startTime
        searchingTime = elapsedTime - sortingTime
        return found
    }

    private fun searchInSorted(records: List<Record>, namesToFind: List<String>): List<Record> {
        val found = mutableListOf<Record>()
        val stepSize = sqrt(records.size.toDouble()).toInt()
        for (name in namesToFind) {
            var blockStart = 0
            var blockEnd = stepSize
            while (records[minOf(blockEnd, records.size) - 1].name < name) {
                blockStart = blockEnd
                blockEnd += stepSize
                if (blockStart >= records.size) continue
            }
            while (records[blockStart].name < name) {
                blockStart++
                if (blockStart == minOf(blockEnd, records.size)) continue
            }
            if (records[blockStart].name == name)
                found.add(records[blockStart])
        }
        return found
    }

    private fun bubbleSort(records: MutableList<Record>, timeLimit: Long): Boolean {
        val startTime = System.currentTimeMillis()
        for (n in records.lastIndex - 1 downTo 0) {
            for (i in 0..n) {
                if (records[i].name > records[i + 1].name) {
                    Collections.swap(records, i, i + 1)
                }
            }
            if (System.currentTimeMillis() - startTime > timeLimit) return true
        }
        return false
    }
}

object BinarySearcher {
    var sortingTime = 0L
    var searchingTime = 0L
    var elapsedTime = 0L

    fun search(records: MutableList<Record>, namesToFind: List<String>): List<Record> {
        val startTime = System.currentTimeMillis()
        quickSort(records)
        sortingTime = System.currentTimeMillis() - startTime
        val found = searchInSorted(records, namesToFind)
        elapsedTime = System.currentTimeMillis() - startTime
        searchingTime = elapsedTime - sortingTime
        return found
    }

    private fun searchInSorted(records: List<Record>, namesToFind: List<String>): List<Record> {
        val found = mutableListOf<Record>()
        for (name in namesToFind) {
            var left = 0
            var right = records.lastIndex
            loop@ while (left <= right) {
                val middle = (left + right) / 2
                when {
                    records[middle].name < name -> left = middle + 1
                    records[middle].name > name -> right = middle - 1
                    else -> {
                        found.add(records[middle])
                        break@loop
                    }
                }
            }
        }
        return found
    }

    private fun quickSort(records: MutableList<Record>, low: Int = 0, high: Int = records.lastIndex) {
        if (low < high) {
            val p = partition(records, low, high)
            quickSort(records, low, p - 1)
            quickSort(records, p + 1, high)
        }
    }

    private fun partition(records: MutableList<Record>, low: Int, high: Int): Int {
        val pivot = records[high].name
        var i = low
        for (j in low..high) {
            if (records[j].name < pivot) {
                Collections.swap(records, i, j)
                i++
            }
        }
        Collections.swap(records, i, high)
        return i
    }
}

data class Record(val number: String, val name: String)

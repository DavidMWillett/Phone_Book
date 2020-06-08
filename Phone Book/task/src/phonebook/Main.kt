package phonebook

import java.io.File
import java.util.Collections
import kotlin.math.sqrt

const val findFile = "C:\\Users\\david\\IdeaProjects\\Data\\find.txt"
const val directoryFile = "C:\\Users\\david\\IdeaProjects\\Data\\directory.txt"

enum class SearchType(val searcher: Searcher, val notification: String) {
    LINEAR(LinearSearcher,"Start searching (linear search)..."),
    JUMP(JumpSearcher,"Start searching (bubble sort + jump search)..."),
    BINARY(BinarySearcher,"Start searching (quick sort + binary search)..."),
    HASH(HashSearcher, "Start searching (hash table)...")
}

fun main() {
    val namesToFind = File(findFile).readLines()
    val directory = Directory()

    directory.search(namesToFind, SearchType.LINEAR)
    directory.search(namesToFind, SearchType.JUMP, 10 * LinearSearcher.elapsedTime)
    directory.search(namesToFind, SearchType.BINARY)
    directory.search(namesToFind, SearchType.HASH)
}

class Directory {
    private val records = loadDirectory()

    private fun loadDirectory(): MutableList<Record> {
        val records = mutableListOf<Record>()
        File(directoryFile).forEachLine {
            val (number, name) = it.split(" ", limit = 2)
            records.add(Record(number, name))
        }
        return records
    }

    fun search(namesToFind: List<String>, searchType: SearchType, timeLimit: Long = 0): List<Record> {
        println(searchType.notification)
        val foundRecords = searchType.searcher.search(records, namesToFind, timeLimit)
        printStats(searchType.searcher, foundRecords.size, namesToFind.size)
        return foundRecords
    }

    private fun printStats(searcher: Searcher, found: Int, total: Int) {
        print("Found $found / $total entries. ")
        println("Time taken: ${searcher.elapsedTime.toTime()}")
        if (searcher !is LinearSearcher) {
            print(if (searcher is HashSearcher) "Creating time: " else "Sorting time: ")
            print(searcher.setupTime.toTime())
            if (searcher is JumpSearcher && searcher.stopped) {
                print(" - STOPPED, moved to linear search")
            }
            println()
            println("Searching time: ${(searcher.elapsedTime - searcher.setupTime).toTime()}")
        }
        println()
    }

    private fun Long.toTime() = "${this / 60_000} min. ${this / 1_000 % 60} sec. ${this % 1_000} ms."
}

object LinearSearcher : Searcher() {
    override fun search(records: MutableList<Record>, namesToFind: List<String>, timeLimit: Long): List<Record> {
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

object JumpSearcher : Searcher() {
    override fun search(records: MutableList<Record>, namesToFind: List<String>, timeLimit: Long): List<Record> {
        val startTime = System.currentTimeMillis()
        stopped = bubbleSort(records, timeLimit)
        setupTime = System.currentTimeMillis() - startTime
        val found = (if (!stopped) {
            searchInSorted(records, namesToFind)
        } else {
            LinearSearcher.search(records, namesToFind)
        })
        elapsedTime = System.currentTimeMillis() - startTime
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

object BinarySearcher : Searcher() {
    override fun search(records: MutableList<Record>, namesToFind: List<String>, timeLimit: Long): List<Record> {
        val startTime = System.currentTimeMillis()
        quickSort(records)
        setupTime = System.currentTimeMillis() - startTime
        val found = searchInSorted(records, namesToFind)
        elapsedTime = System.currentTimeMillis() - startTime
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

object HashSearcher : Searcher() {
    override fun search(records: MutableList<Record>, namesToFind: List<String>, timeLimit: Long): List<Record> {
        val startTime = System.currentTimeMillis()
        val found = mutableListOf<Record>()
        val hashTable = createHashTable(records)
        setupTime = System.currentTimeMillis() - startTime
        for (name in namesToFind) {
            val number = hashTable[name]
            if (number != null) found.add(number)
        }
        elapsedTime = System.currentTimeMillis() - startTime
        return found
    }

    private fun createHashTable(records: List<Record>): HashMap<String, Record> {
        val hashTable = HashMap<String, Record>()
        for (record in records) {
            hashTable[record.name] = record
        }
        return hashTable
    }
}

abstract class Searcher {
    var elapsedTime = 0L
    var setupTime = 0L
    var stopped = false

    abstract fun search(records: MutableList<Record>, namesToFind: List<String>, timeLimit: Long = 0): List<Record>
}

data class Record(val number: String, val name: String)

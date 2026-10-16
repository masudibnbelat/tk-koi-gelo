// FILE: app/src/main/java/com/masud/moneymanager/data/BackupManager.kt
package com.masud.moneymanager.data

import android.content.Context
import android.net.Uri
import com.masud.moneymanager.ui.TaskAlarmScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    private data class BackupData(
            val transactions: List<TransactionEntity> = emptyList(),
            val loans: List<LoanEntity> = emptyList(),
            val repayments: List<LoanRepaymentEntity> = emptyList(),
            val categories: List<CategoryEntity> = emptyList(),
            val accounts: List<AccountEntity> = emptyList(),
            val entries: List<AccountEntryEntity> = emptyList(),
            val tasks: List<TaskEntity> = emptyList()
    )

    private fun dateFmt() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    // ---------------- collect ----------------

    private suspend fun collectData(db: AppDatabase): BackupData {
        val loans = db.loanDao().observeAllLoans().first()
        val accounts = db.accountDao().observeAccounts().first()
        return BackupData(
                transactions = db.transactionDao().observeAll().first(),
                loans = loans,
                repayments = loans.flatMap { db.loanDao().observeRepayments(it.id).first() },
                categories = db.categoryDao().observeAll().first(),
                accounts = accounts,
                entries = accounts.flatMap { db.accountDao().observeEntries(it.id).first() },
                tasks = db.taskDao().observeAll().first()
        )
    }

    // ---------------- JSON export ----------------

    suspend fun exportJson(context: Context, db: AppDatabase, uri: Uri) =
            withContext(Dispatchers.IO) {
                val data = collectData(db)
                val root = JSONObject()
                root.put("app", "MoneyManager")
                root.put("version", 6)
                root.put("exportedAt", System.currentTimeMillis())

                root.put(
                        "transactions",
                        JSONArray(
                                data.transactions.map { t ->
                                    JSONObject()
                                            .put("id", t.id)
                                            .put("title", t.title)
                                            .put("amount", t.amount)
                                            .put("type", t.type)
                                            .put("category", t.category)
                                            .put("note", t.note)
                                            .put("createdAt", t.createdAt)
                                }
                        )
                )
                root.put(
                        "loans",
                        JSONArray(
                                data.loans.map { l ->
                                    JSONObject()
                                            .put("id", l.id)
                                            .put("personName", l.personName)
                                            .put("type", l.type)
                                            .put("principalAmount", l.principalAmount)
                                            .put("note", l.note)
                                            .put("createdAt", l.createdAt)
                                }
                        )
                )
                root.put(
                        "repayments",
                        JSONArray(
                                data.repayments.map { r ->
                                    JSONObject()
                                            .put("id", r.id)
                                            .put("loanId", r.loanId)
                                            .put("amount", r.amount)
                                            .put("note", r.note)
                                            .put("createdAt", r.createdAt)
                                }
                        )
                )
                root.put(
                        "categories",
                        JSONArray(
                                data.categories.map { c ->
                                    JSONObject()
                                            .put("id", c.id)
                                            .put("name", c.name)
                                            .put("type", c.type)
                                            .put("createdAt", c.createdAt)
                                }
                        )
                )
                root.put(
                        "accounts",
                        JSONArray(
                                data.accounts.map { a ->
                                    JSONObject()
                                            .put("id", a.id)
                                            .put("name", a.name)
                                            .put("createdAt", a.createdAt)
                                }
                        )
                )
                root.put(
                        "accountEntries",
                        JSONArray(
                                data.entries.map { e ->
                                    JSONObject()
                                            .put("id", e.id)
                                            .put("accountId", e.accountId)
                                            .put("amount", e.amount)
                                            .put("note", e.note)
                                            .put("createdAt", e.createdAt)
                                }
                        )
                )
                root.put(
                        "tasks",
                        JSONArray(
                                data.tasks.map { t ->
                                    JSONObject()
                                            .put("id", t.id)
                                            .put("title", t.title)
                                            .put("details", t.details)
                                            .put("amount", t.amount)
                                            .put(
                                                    "remindAt",
                                                    if (t.remindAt != null) t.remindAt
                                                    else JSONObject.NULL
                                            )
                                            .put(
                                                    "remindDate",
                                                    if (t.remindDate != null) t.remindDate
                                                    else JSONObject.NULL
                                            )
                                            .put("isDone", t.isDone)
                                            .put("createdAt", t.createdAt)
                                }
                        )
                )

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(root.toString(2).toByteArray(Charsets.UTF_8))
                }
                        ?: throw IllegalStateException("Cannot open file for writing")
            }

    // ---------------- JSON import ----------------

    suspend fun importJson(context: Context, db: AppDatabase, uri: Uri): Int =
            withContext(Dispatchers.IO) {
                val text =
                        context.contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader(Charsets.UTF_8)
                                ?.readText()
                                ?: throw IllegalStateException("Cannot open file")
                restore(context, db, parseJson(JSONObject(text)))
            }

    private fun parseJson(root: JSONObject): BackupData {
        fun arr(name: String): JSONArray = root.optJSONArray(name) ?: JSONArray()
        val txs = mutableListOf<TransactionEntity>()
        arr("transactions").let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                txs.add(
                        TransactionEntity(
                                id = o.optLong("id"),
                                title = o.optString("title"),
                                amount = o.optDouble("amount"),
                                type = o.optString("type", "EXPENSE"),
                                category = o.optString("category"),
                                note = o.optString("note"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                )
            }
        }
        val loans = mutableListOf<LoanEntity>()
        arr("loans").let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                loans.add(
                        LoanEntity(
                                id = o.optLong("id"),
                                personName = o.optString("personName"),
                                type = o.optString("type", "LENT"),
                                principalAmount = o.optDouble("principalAmount"),
                                note = o.optString("note"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                )
            }
        }
        val repays = mutableListOf<LoanRepaymentEntity>()
        arr("repayments").let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                repays.add(
                        LoanRepaymentEntity(
                                id = o.optLong("id"),
                                loanId = o.optLong("loanId"),
                                amount = o.optDouble("amount"),
                                note = o.optString("note"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                )
            }
        }
        val cats = mutableListOf<CategoryEntity>()
        arr("categories").let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                cats.add(
                        CategoryEntity(
                                id = o.optLong("id"),
                                name = o.optString("name"),
                                type = o.optString("type", "EXPENSE"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                )
            }
        }
        val accs = mutableListOf<AccountEntity>()
        arr("accounts").let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                accs.add(
                        AccountEntity(
                                id = o.optLong("id"),
                                name = o.optString("name"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                )
            }
        }
        val entries = mutableListOf<AccountEntryEntity>()
        arr("accountEntries").let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                entries.add(
                        AccountEntryEntity(
                                id = o.optLong("id"),
                                accountId = o.optLong("accountId"),
                                amount = o.optDouble("amount"),
                                note = o.optString("note"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                )
            }
        }
        val tasks = mutableListOf<TaskEntity>()
        arr("tasks").let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                tasks.add(
                        TaskEntity(
                                id = o.optLong("id"),
                                title = o.optString("title"),
                                details = o.optString("details"),
                                amount = o.optDouble("amount"),
                                remindAt =
                                        if (o.isNull("remindAt")) null else o.optLong("remindAt"),
                                remindDate =
                                        if (o.isNull("remindDate")) null
                                        else o.optLong("remindDate"),
                                isDone = o.optBoolean("isDone"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                )
            }
        }
        return BackupData(txs, loans, repays, cats, accs, entries, tasks)
    }

    // ---------------- Excel export ----------------

    suspend fun exportExcel(context: Context, db: AppDatabase, uri: Uri) =
            withContext(Dispatchers.IO) {
                val fmt = dateFmt()
                val data = collectData(db)
                val wb = XSSFWorkbook()
                try {
                    writeSheet(
                            wb,
                            "Transactions",
                            listOf("ID", "Title", "Amount", "Type", "Category", "Note", "Date"),
                            data.transactions.map {
                                listOf<Any?>(
                                        it.id,
                                        it.title,
                                        it.amount,
                                        it.type,
                                        it.category,
                                        it.note,
                                        fmt.format(Date(it.createdAt))
                                )
                            }
                    )
                    writeSheet(
                            wb,
                            "Loans",
                            listOf("ID", "Person", "Type", "Principal", "Note", "Date"),
                            data.loans.map {
                                listOf<Any?>(
                                        it.id,
                                        it.personName,
                                        it.type,
                                        it.principalAmount,
                                        it.note,
                                        fmt.format(Date(it.createdAt))
                                )
                            }
                    )
                    writeSheet(
                            wb,
                            "Repayments",
                            listOf("ID", "LoanID", "Amount", "Note", "Date"),
                            data.repayments.map {
                                listOf<Any?>(
                                        it.id,
                                        it.loanId,
                                        it.amount,
                                        it.note,
                                        fmt.format(Date(it.createdAt))
                                )
                            }
                    )
                    writeSheet(
                            wb,
                            "Categories",
                            listOf("ID", "Name", "Type", "Date"),
                            data.categories.map {
                                listOf<Any?>(
                                        it.id,
                                        it.name,
                                        it.type,
                                        fmt.format(Date(it.createdAt))
                                )
                            }
                    )
                    writeSheet(
                            wb,
                            "Accounts",
                            listOf("ID", "Name", "Date"),
                            data.accounts.map {
                                listOf<Any?>(it.id, it.name, fmt.format(Date(it.createdAt)))
                            }
                    )
                    writeSheet(
                            wb,
                            "AccountEntries",
                            listOf("ID", "AccountID", "Amount", "Note", "Date"),
                            data.entries.map {
                                listOf<Any?>(
                                        it.id,
                                        it.accountId,
                                        it.amount,
                                        it.note,
                                        fmt.format(Date(it.createdAt))
                                )
                            }
                    )
                    writeSheet(
                            wb,
                            "Tasks",
                            listOf(
                                    "ID",
                                    "Title",
                                    "Details",
                                    "Amount",
                                    "RemindAt",
                                    "RemindDate",
                                    "IsDone",
                                    "CreatedAt"
                            ),
                            data.tasks.map {
                                listOf<Any?>(
                                        it.id,
                                        it.title,
                                        it.details,
                                        it.amount,
                                        it.remindAt?.let { r -> fmt.format(Date(r)) } ?: "",
                                        it.remindDate?.let { r -> fmt.format(Date(r)) } ?: "",
                                        if (it.isDone) 1 else 0,
                                        fmt.format(Date(it.createdAt))
                                )
                            }
                    )
                    context.contentResolver.openOutputStream(uri)?.use { out -> wb.write(out) }
                            ?: throw IllegalStateException("Cannot open file for writing")
                } finally {
                    wb.close()
                }
            }

    private fun writeSheet(
            wb: XSSFWorkbook,
            name: String,
            headers: List<String>,
            rows: List<List<Any?>>
    ) {
        val sheet = wb.createSheet(name)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }
        rows.forEachIndexed { r, row ->
            val excelRow = sheet.createRow(r + 1)
            row.forEachIndexed { c, v ->
                val cell = excelRow.createCell(c)
                when (v) {
                    is Number -> cell.setCellValue(v.toDouble())
                    else -> cell.setCellValue(v?.toString() ?: "")
                }
            }
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }

    // ---------------- Excel import ----------------

    suspend fun importExcel(context: Context, db: AppDatabase, uri: Uri): Int =
            withContext(Dispatchers.IO) {
                val fmt = dateFmt()
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    val wb = XSSFWorkbook(ins)
                    try {
                        val data =
                                BackupData(
                                        transactions =
                                                readSheet(wb, "Transactions").mapNotNull { m ->
                                                    try {
                                                        TransactionEntity(
                                                                id = num(m["ID"]),
                                                                title = m["Title"].orEmpty(),
                                                                amount =
                                                                        m["Amount"]
                                                                                ?.toDoubleOrNull()
                                                                                ?: 0.0,
                                                                type =
                                                                        m["Type"]?.ifBlank {
                                                                            "EXPENSE"
                                                                        }
                                                                                ?: "EXPENSE",
                                                                category = m["Category"].orEmpty(),
                                                                note = m["Note"].orEmpty(),
                                                                createdAt =
                                                                        parseDate(fmt, m["Date"])
                                                        )
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                },
                                        loans =
                                                readSheet(wb, "Loans").mapNotNull { m ->
                                                    try {
                                                        LoanEntity(
                                                                id = num(m["ID"]),
                                                                personName = m["Person"].orEmpty(),
                                                                type = m["Type"]?.ifBlank { "LENT" }
                                                                                ?: "LENT",
                                                                principalAmount =
                                                                        m["Principal"]
                                                                                ?.toDoubleOrNull()
                                                                                ?: 0.0,
                                                                note = m["Note"].orEmpty(),
                                                                createdAt =
                                                                        parseDate(fmt, m["Date"])
                                                        )
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                },
                                        repayments =
                                                readSheet(wb, "Repayments").mapNotNull { m ->
                                                    try {
                                                        LoanRepaymentEntity(
                                                                id = num(m["ID"]),
                                                                loanId = num(m["LoanID"]),
                                                                amount =
                                                                        m["Amount"]
                                                                                ?.toDoubleOrNull()
                                                                                ?: 0.0,
                                                                note = m["Note"].orEmpty(),
                                                                createdAt =
                                                                        parseDate(fmt, m["Date"])
                                                        )
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                },
                                        categories =
                                                readSheet(wb, "Categories").mapNotNull { m ->
                                                    try {
                                                        CategoryEntity(
                                                                id = num(m["ID"]),
                                                                name = m["Name"].orEmpty(),
                                                                type =
                                                                        m["Type"]?.ifBlank {
                                                                            "EXPENSE"
                                                                        }
                                                                                ?: "EXPENSE",
                                                                createdAt =
                                                                        parseDate(fmt, m["Date"])
                                                        )
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                },
                                        accounts =
                                                readSheet(wb, "Accounts").mapNotNull { m ->
                                                    try {
                                                        AccountEntity(
                                                                id = num(m["ID"]),
                                                                name = m["Name"].orEmpty(),
                                                                createdAt =
                                                                        parseDate(fmt, m["Date"])
                                                        )
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                },
                                        entries =
                                                readSheet(wb, "AccountEntries").mapNotNull { m ->
                                                    try {
                                                        AccountEntryEntity(
                                                                id = num(m["ID"]),
                                                                accountId = num(m["AccountID"]),
                                                                amount =
                                                                        m["Amount"]
                                                                                ?.toDoubleOrNull()
                                                                                ?: 0.0,
                                                                note = m["Note"].orEmpty(),
                                                                createdAt =
                                                                        parseDate(fmt, m["Date"])
                                                        )
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                },
                                        tasks =
                                                readSheet(wb, "Tasks").mapNotNull { m ->
                                                    try {
                                                        val ra = m["RemindAt"]
                                                        val rd = m["RemindDate"]
                                                        TaskEntity(
                                                                id = num(m["ID"]),
                                                                title = m["Title"].orEmpty(),
                                                                details = m["Details"].orEmpty(),
                                                                amount =
                                                                        m["Amount"]
                                                                                ?.toDoubleOrNull()
                                                                                ?: 0.0,
                                                                remindAt =
                                                                        if (ra.isNullOrBlank()) null
                                                                        else parseDate(fmt, ra),
                                                                remindDate =
                                                                        if (rd.isNullOrBlank()) null
                                                                        else parseDate(fmt, rd),
                                                                isDone =
                                                                        (m["IsDone"]
                                                                                ?.toDoubleOrNull()
                                                                                ?.toInt()
                                                                                ?: 0) == 1,
                                                                createdAt =
                                                                        parseDate(
                                                                                fmt,
                                                                                m["CreatedAt"]
                                                                        )
                                                        )
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                }
                                )
                        restore(context, db, data)
                    } finally {
                        wb.close()
                    }
                }
                        ?: throw IllegalStateException("Cannot open file")
            }

    private fun num(s: String?): Long = s?.toDoubleOrNull()?.toLong() ?: 0L

    private fun parseDate(fmt: SimpleDateFormat, s: String?): Long {
        if (s.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            fmt.parse(s)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            s.toDoubleOrNull()?.toLong() ?: System.currentTimeMillis()
        }
    }

    private fun readSheet(wb: XSSFWorkbook, name: String): List<Map<String, String>> {
        val sheet = wb.getSheet(name) ?: return emptyList()
        val headerRow = sheet.getRow(0) ?: return emptyList()
        val headers =
                (0 until headerRow.lastCellNum).map { i ->
                    headerRow.getCell(i)?.let { cellStr(it) } ?: ""
                }
        val out = mutableListOf<Map<String, String>>()
        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val map = mutableMapOf<String, String>()
            headers.forEachIndexed { c, h ->
                if (h.isNotBlank()) map[h] = row.getCell(c)?.let { cellStr(it) } ?: ""
            }
            if (map.values.any { it.isNotBlank() }) out.add(map)
        }
        return out
    }

    private fun cellStr(cell: org.apache.poi.ss.usermodel.Cell): String {
        return when (cell.cellType) {
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    dateFmt().format(cell.dateCellValue)
                } else {
                    val d = cell.numericCellValue
                    if (d == floor(d) && !d.isInfinite()) d.toLong().toString() else d.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else ->
                    try {
                        cell.stringCellValue
                    } catch (_: Exception) {
                        ""
                    }
        }
    }

    // ---------------- auto-detect import ----------------

    suspend fun importFile(context: Context, db: AppDatabase, uri: Uri): Int {
        return try {
            importJson(context, db, uri)
        } catch (_: Exception) {
            importExcel(context, db, uri)
        }
    }

    // ---------------- restore (replace semantics) ----------------

    private suspend fun restore(context: Context, db: AppDatabase, data: BackupData): Int {
        // Replace: clear first (FK cascade handles repayments & entries)
        db.transactionDao().deleteAll()
        db.loanDao().observeAllLoans().first().forEach { db.loanDao().deleteLoan(it.id) }
        db.categoryDao().observeAll().first().forEach { db.categoryDao().deleteById(it.id) }
        db.accountDao().observeAccounts().first().forEach { db.accountDao().deleteAccount(it.id) }
        // Purano task alarm cancel kore tasks clear
        db.taskDao().observeAll().first().forEach { TaskAlarmScheduler.cancel(context, it.id) }
        db.taskDao().deleteAll()

        var n = 0
        data.categories.forEach {
            db.categoryDao().insert(it)
            n++
        }
        data.transactions.forEach {
            db.transactionDao().insert(it)
            n++
        }
        data.loans.forEach {
            db.loanDao().insertLoan(it)
            n++
        }
        data.repayments.forEach {
            db.loanDao().insertRepayment(it)
            n++
        }
        data.accounts.forEach {
            db.accountDao().insertAccount(it)
            n++
        }
        data.entries.forEach {
            db.accountDao().insertEntry(it)
            n++
        }
        data.tasks.forEach { task ->
            db.taskDao().insert(task)
            n++
            // Future reminder thakle alarm abar schedule koro
            if (!task.isDone && (task.remindAt != null || task.remindDate != null)) {
                TaskAlarmScheduler.schedule(context, task)
            }
        }
        return n
    }
}

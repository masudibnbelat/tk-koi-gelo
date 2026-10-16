
package com.masud.moneymanager.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val transactions: List<TransactionEntity>,
    val loans: List<LoanEntity>,
    val repayments: List<LoanRepaymentEntity>
)

object JsonBackup {

    fun encode(
        transactions: List<TransactionEntity>,
        loans: List<LoanEntity>,
        repayments: List<LoanRepaymentEntity>
    ): String {
        val root = JSONObject().apply {
            put("app", "Money Manager")
            put("schemaVersion", 2)
            put("exportedAt", System.currentTimeMillis())

            put("transactions", JSONArray().apply {
                transactions.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("amount", item.amount)
                        put("type", item.type)
                        put("category", item.category)
                        put("note", item.note)
                        put("createdAt", item.createdAt)
                    })
                }
            })

            put("loans", JSONArray().apply {
                loans.forEach { loan ->
                    put(JSONObject().apply {
                        put("id", loan.id)
                        put("personName", loan.personName)
                        put("type", loan.type)
                        put("principalAmount", loan.principalAmount)
                        put("note", loan.note)
                        put("createdAt", loan.createdAt)
                    })
                }
            })

            put("repayments", JSONArray().apply {
                repayments.forEach { repayment ->
                    put(JSONObject().apply {
                        put("id", repayment.id)
                        put("loanId", repayment.loanId)
                        put("amount", repayment.amount)
                        put("note", repayment.note)
                        put("createdAt", repayment.createdAt)
                    })
                }
            })
        }

        return root.toString(2)
    }

    fun decode(json: String): BackupData {
        val root = JSONObject(json)
        val version = root.optInt("schemaVersion", 1)

        require(version in 1..2) {
            "Unsupported backup version: $version"
        }

        val transactions = buildList {
            val array = root.getJSONArray("transactions")

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = obj.getString("type")
                val amount = obj.getDouble("amount")

                require(type == "INCOME" || type == "EXPENSE") {
                    "Unknown transaction type: $type"
                }
                require(amount.isFinite() && amount > 0.0) {
                    "Invalid transaction amount"
                }

                add(
                    TransactionEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.getString("title"),
                        amount = amount,
                        type = type,
                        category = obj.optString("category", "General"),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong(
                            "createdAt",
                            System.currentTimeMillis()
                        )
                    )
                )
            }
        }

        val loans = buildList {
            val array = root.optJSONArray("loans") ?: JSONArray()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = obj.getString("type")
                val amount = obj.getDouble("principalAmount")
                val personName = obj.getString("personName").trim()

                require(type == "LENT" || type == "BORROWED") {
                    "Unknown loan type: $type"
                }
                require(amount.isFinite() && amount > 0.0) {
                    "Invalid loan amount"
                }
                require(personName.isNotBlank()) {
                    "Loan person name cannot be empty"
                }

                add(
                    LoanEntity(
                        id = obj.getLong("id"),
                        personName = personName,
                        type = type,
                        principalAmount = amount,
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong(
                            "createdAt",
                            System.currentTimeMillis()
                        )
                    )
                )
            }
        }

        val repayments = buildList {
            val array = root.optJSONArray("repayments") ?: JSONArray()
            val loanIds = loans.map { it.id }.toSet()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val loanId = obj.getLong("loanId")
                val amount = obj.getDouble("amount")

                require(loanId in loanIds) {
                    "Repayment references an unknown loan"
                }
                require(amount.isFinite() && amount > 0.0) {
                    "Invalid repayment amount"
                }

                add(
                    LoanRepaymentEntity(
                        id = obj.optLong("id", 0L),
                        loanId = loanId,
                        amount = amount,
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong(
                            "createdAt",
                            System.currentTimeMillis()
                        )
                    )
                )
            }
        }

        return BackupData(
            transactions = transactions,
            loans = loans,
            repayments = repayments
        )
    }
}

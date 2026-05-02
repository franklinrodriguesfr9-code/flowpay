package com.flowpay.data

data class CategoryTotal(
    val categoryName: String,
    val flowType: FlowType,
    val totalCents: Long,
)

data class ReportLine(
    val title: String,
    val categoryName: String,
    val kind: CommitmentKind? = null,
    val purpose: CommitmentPurpose? = null,
    val flowType: FlowType,
    val amountCents: Long,
    val dateMillis: Long,
    val dueAt: Long = dateMillis,
    val completedAt: Long? = null,
    val status: OccurrenceStatus?,
    val source: String,
    val notes: String,
)

data class ReportSnapshot(
    val year: Int,
    val month: Int,
    val incomeCents: Long,
    val expenseCents: Long,
    val pendingCents: Long,
    val openCents: Long,
    val overdueCents: Long,
    val paidCount: Int,
    val taskCount: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val openCount: Int,
    val overdueCount: Int,
    val categoryTotals: List<CategoryTotal>,
    val lines: List<ReportLine>,
) {
    val balanceCents: Long = incomeCents - expenseCents

    companion object {
        fun empty(year: Int, month: Int) = ReportSnapshot(
            year = year,
            month = month,
            incomeCents = 0,
            expenseCents = 0,
            pendingCents = 0,
            openCents = 0,
            overdueCents = 0,
            paidCount = 0,
            taskCount = 0,
            completedCount = 0,
            pendingCount = 0,
            openCount = 0,
            overdueCount = 0,
            categoryTotals = emptyList(),
            lines = emptyList(),
        )
    }
}

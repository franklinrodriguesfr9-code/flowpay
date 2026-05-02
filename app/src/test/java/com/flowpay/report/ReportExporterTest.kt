package com.flowpay.report

import com.flowpay.data.FlowType
import com.flowpay.data.OccurrenceStatus
import com.flowpay.data.CommitmentPurpose
import com.flowpay.data.ReportLine
import com.flowpay.data.ReportSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExporterTest {
    @Test
    fun csvIncludesSummaryAndLines() {
        val report = ReportSnapshot(
            year = 2026,
            month = 5,
            incomeCents = 500000,
            expenseCents = 12550,
            pendingCents = 9900,
            openCents = 9900,
            overdueCents = 0,
            paidCount = 1,
            taskCount = 0,
            completedCount = 1,
            pendingCount = 1,
            openCount = 1,
            overdueCount = 0,
            categoryTotals = emptyList(),
            lines = listOf(
                ReportLine(
                    title = "Nota fiscal",
                    categoryName = "Nota fiscal",
                    purpose = CommitmentPurpose.PAYMENT,
                    flowType = FlowType.ENTRADA,
                    amountCents = 500000,
                    dateMillis = 0,
                    dueAt = 0,
                    completedAt = 0,
                    status = OccurrenceStatus.FEITO,
                    source = "Compromisso",
                    notes = "",
                ),
            ),
        )

        val csv = ReportExporter.csvContent(report)

        assertTrue(csv.contains("Nota fiscal"))
        assertTrue(csv.contains("data_vencimento"))
        assertTrue(csv.contains("resumo;saldo;4874,50"))
    }
}

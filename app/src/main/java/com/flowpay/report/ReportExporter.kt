package com.flowpay.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.flowpay.data.FlowType
import com.flowpay.data.OccurrenceStatus
import com.flowpay.data.ReportSnapshot
import com.flowpay.util.DateUtils
import com.flowpay.util.MoneyUtils
import java.io.File

object ReportExporter {
    fun shareReport(context: Context, report: ReportSnapshot) {
        val files = writeReportFiles(context, report)
        val uris = files.map { file ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_SUBJECT, "Relatorio FlowPay %02d/%d".format(report.month, report.year))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exportar relatorio FlowPay"))
    }

    fun csvContent(report: ReportSnapshot): String {
        val rows = mutableListOf<List<String>>()
        rows += listOf(
            "origem",
            "data_vencimento",
            "data_conclusao",
            "dias_atraso",
            "descricao",
            "categoria",
            "fluxo",
            "valor",
            "status",
            "observacao",
        )
        report.lines.forEach { line ->
            rows += listOf(
                line.source,
                DateUtils.formatDate(line.dueAt),
                line.completedAt?.let(DateUtils::formatDate).orEmpty(),
                daysLateText(line),
                line.title,
                line.categoryName,
                if (line.flowType == FlowType.ENTRADA) "entrada" else "saida",
                centsForCsv(line.amountCents),
                statusText(line.status, line.dueAt, line.purpose?.name == "TASK"),
                line.notes,
            )
        }
        rows += emptyList<String>()
        rows += listOf("resumo", "entradas", centsForCsv(report.incomeCents))
        rows += listOf("resumo", "saidas", centsForCsv(report.expenseCents))
        rows += listOf("resumo", "saldo", centsForCsv(report.balanceCents))
        rows += listOf("resumo", "pendentes", centsForCsv(report.pendingCents))
        return rows.joinToString("\n") { row -> row.joinToString(";") { csvEscape(it) } }
    }

    fun txtContent(report: ReportSnapshot): String = buildString {
        appendLine("FlowPay - Relatorio %02d/%d".format(report.month, report.year))
        appendLine()
        appendLine("Entradas feitas: ${MoneyUtils.format(report.incomeCents)}")
        appendLine("Saidas feitas: ${MoneyUtils.format(report.expenseCents)}")
        appendLine("Saldo: ${MoneyUtils.format(report.balanceCents)}")
        appendLine("Pendentes previstos: ${MoneyUtils.format(report.pendingCents)}")
        appendLine("Itens pagos: ${report.paidCount}")
        appendLine("Notas fiscais/feitos: ${report.taskCount}")
        appendLine("Itens feitos no total: ${report.completedCount}")
        appendLine("Itens pendentes: ${report.pendingCount}")
        appendLine()
        appendLine("Totais por categoria")
        if (report.categoryTotals.isEmpty()) {
            appendLine("- Nenhum item feito no periodo.")
        } else {
            report.categoryTotals.forEach { total ->
                val flow = if (total.flowType == FlowType.ENTRADA) "entrada" else "saida"
                appendLine("- ${total.categoryName} ($flow): ${MoneyUtils.format(total.totalCents)}")
            }
        }
        appendLine()
        appendLine("Lancamentos")
        if (report.lines.isEmpty()) {
            appendLine("- Nenhum lancamento no periodo.")
        } else {
            report.lines.forEach { line ->
                val status = statusText(line.status, line.dueAt, line.purpose?.name == "TASK")
                val completed = line.completedAt?.let { " | feito em ${DateUtils.formatDate(it)}" }.orEmpty()
                val late = daysLateText(line).takeIf { it.isNotBlank() }?.let { " | atraso: $it dias" }.orEmpty()
                appendLine(
                    "- venc. ${DateUtils.formatDate(line.dueAt)}$completed$late | ${line.title} | ${line.categoryName} | " +
                        "${MoneyUtils.format(line.amountCents)} | $status | ${line.source}",
                )
            }
        }
    }

    private fun writeReportFiles(context: Context, report: ReportSnapshot): List<File> {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val baseName = "flowpay_%04d_%02d".format(report.year, report.month)
        val csv = File(dir, "$baseName.csv")
        val txt = File(dir, "$baseName.txt")
        csv.writeText(csvContent(report), Charsets.UTF_8)
        txt.writeText(txtContent(report), Charsets.UTF_8)
        return listOf(csv, txt)
    }

    private fun centsForCsv(cents: Long): String = "%.2f".format(cents / 100.0).replace(".", ",")

    private fun daysLateText(line: com.flowpay.data.ReportLine): String {
        if (line.status != OccurrenceStatus.PENDENTE) return ""
        val days = DateUtils.daysLate(line.dueAt)
        return if (days > 0) days.toString() else ""
    }

    private fun statusText(status: OccurrenceStatus?, dueAt: Long, isTask: Boolean): String {
        return when (status) {
            OccurrenceStatus.PENDENTE -> if (DateUtils.daysLate(dueAt) > 0) "vencida" else "pendente"
            OccurrenceStatus.FEITO -> if (isTask) "feito" else "pago"
            OccurrenceStatus.CANCELADO -> "cancelado"
            null -> "pago"
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(';') || escaped.contains('"') || escaped.contains('\n')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}

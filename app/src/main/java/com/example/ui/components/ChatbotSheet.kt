package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.ChatMessage
import com.example.ui.theme.FarmaciaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotSheet(
  messages: List<ChatMessage>,
  isThinking: Boolean,
  isPlaceholderMode: Boolean,
  onSend: (String) -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
) {
  var input by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.88f)
        .imePadding()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(8.dp),
            )
          }
          Column {
            Text("FarmaIA · Asistente Clínico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text = if (isPlaceholderMode) "Motor local de farmacia (sin nube)" else "Conectado a Gemini · Datos en vivo",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row {
          IconButton(
            onClick = onClear,
            modifier = Modifier.testTag("chat_clear_button"),
          ) {
            Icon(Icons.Filled.DeleteSweep, contentDescription = "Limpiar chat")
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("chat_close_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar asistente")
          }
        }
      }

      if (isPlaceholderMode) {
        Surface(
          color = FarmaciaColors.extended.warningContainer.copy(alpha = 0.65f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, FarmaciaColors.extended.warning.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Info,
              contentDescription = null,
              tint = FarmaciaColors.extended.warning,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Modo local de respaldo activo.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          "¿Qué vence pronto?",
          "Stock crítico",
          "¿Cuánto vendimos hoy?",
          "Dolor de cabeza",
          "Gastritis",
          "Panadol",
          "Alcohol y fármacos",
        ).forEach { prompt ->
          AssistChip(
            onClick = { onSend(prompt) },
            label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
          )
        }
      }

      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(messages, key = { it.id }) { msg ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
          ) {
            Surface(
              shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp,
              ),
              color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(if (msg.isUser) 0.82f else 0.94f),
            ) {
              Box(modifier = Modifier.padding(12.dp)) {
                if (msg.isUser) {
                  Text(
                    text = msg.text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                } else {
                  FormattedBotMessage(text = msg.text)
                }
              }
            }
          }
        }
        if (isThinking) {
          item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Text("Analizando consulta clínica...", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = input,
          onValueChange = { input = it },
          placeholder = { Text("Pregunta sobre síntomas, dosis o stock...") },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("chat_input"),
        )
        IconButton(
          onClick = {
            if (input.isNotBlank()) {
              onSend(input)
              input = ""
            }
          },
          enabled = input.isNotBlank() && !isThinking,
          modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
            .testTag("chat_send_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Enviar mensaje",
            tint = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }
    }
  }
}

@Composable
fun FormattedBotMessage(text: String) {
  val lines = remember(text) { text.split("\n") }
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    lines.forEach { raw ->
      val line = raw.trim()
      if (line.isBlank()) {
        Spacer(Modifier.height(2.dp))
      } else if (line.startsWith("⚠️") || line.startsWith("🛑") || line.startsWith("💡")) {
        val isRed = line.startsWith("🛑")
        val isTip = line.startsWith("💡")
        val bg = when {
          isRed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
          isTip -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
          else -> FarmaciaColors.extended.warningContainer.copy(alpha = 0.7f)
        }
        Surface(color = bg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = if (isTip) Icons.Filled.Info else Icons.Filled.Warning,
              contentDescription = null,
              tint = when {
                isRed -> MaterialTheme.colorScheme.error
                isTip -> MaterialTheme.colorScheme.primary
                else -> FarmaciaColors.extended.warning
              },
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = parseInlineMarkdown(line.drop(2).trim()),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      } else if (line.startsWith("🩺") || line.startsWith("💊") || line.startsWith("📅") || line.startsWith("📊") || line.startsWith("🏆") || line.startsWith("🤰") || line.startsWith("👋")) {
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = parseInlineMarkdown(line),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      } else {
        Text(
          text = parseInlineMarkdown(line),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

private fun parseInlineMarkdown(input: String) = buildAnnotatedString {
  var idx = 0
  val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
  val matches = boldRegex.findAll(input)
  for (m in matches) {
    if (m.range.first > idx) {
      appendItalicSegments(input.substring(idx, m.range.first))
    }
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
      append(m.groupValues[1])
    }
    idx = m.range.last + 1
  }
  if (idx < input.length) {
    appendItalicSegments(input.substring(idx))
  }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendItalicSegments(segment: String) {
  val italicRegex = Regex("\\*(.*?)\\*")
  var idx = 0
  for (m in italicRegex.findAll(segment)) {
    if (m.range.first > idx) {
      append(segment.substring(idx, m.range.first))
    }
    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
      append(m.groupValues[1])
    }
    idx = m.range.last + 1
  }
  if (idx < segment.length) {
    append(segment.substring(idx))
  }
}

package com.smartcalc.ai.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartcalc.ai.R
import com.smartcalc.ai.ui.components.CalcButton
import com.smartcalc.ai.ui.components.CalcButtonStyle

@Composable
fun CalculatorScreen(
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    viewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Picks up an expression tapped in the history screen.
    LaunchedEffect(Unit) { viewModel.refreshHandoff() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.calculator_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        DisplayCard(state = state)

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onOpenCamera,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_photo_task),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            OutlinedButton(
                onClick = onOpenGallery,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_pick_image),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Keypad(viewModel = viewModel)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DisplayCard(state: CalculatorUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (state.errorRes != null) {
                Text(
                    text = stringResource(state.errorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            } else {
                Text(
                    text = state.display,
                    fontSize = if (state.display.length > 12) 34.sp else 52.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.expression.ifBlank { " " },
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun Keypad(viewModel: CalculatorViewModel) {
    val backspaceLabel = stringResource(R.string.cd_backspace)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            CalcButton("C", style = CalcButtonStyle.FUNCTION) { viewModel.onClear() }
            CalcButton(
                stringResource(R.string.btn_parentheses),
                style = CalcButtonStyle.FUNCTION
            ) { viewModel.onParenthesis() }
            CalcButton(
                stringResource(R.string.btn_toggle_sign),
                style = CalcButtonStyle.FUNCTION
            ) { viewModel.onToggleSign() }
            CalcButton(
                icon = Icons.Outlined.Backspace,
                style = CalcButtonStyle.FUNCTION,
                contentDescription = backspaceLabel
            ) { viewModel.onBackspace() }
        }
        Row(Modifier.fillMaxWidth()) {
            CalcButton("7") { viewModel.onDigit('7') }
            CalcButton("8") { viewModel.onDigit('8') }
            CalcButton("9") { viewModel.onDigit('9') }
            CalcButton("\u00F7", style = CalcButtonStyle.OPERATOR) { viewModel.onOperator('\u00F7') }
        }
        Row(Modifier.fillMaxWidth()) {
            CalcButton("4") { viewModel.onDigit('4') }
            CalcButton("5") { viewModel.onDigit('5') }
            CalcButton("6") { viewModel.onDigit('6') }
            CalcButton("\u00D7", style = CalcButtonStyle.OPERATOR) { viewModel.onOperator('\u00D7') }
        }
        Row(Modifier.fillMaxWidth()) {
            CalcButton("1") { viewModel.onDigit('1') }
            CalcButton("2") { viewModel.onDigit('2') }
            CalcButton("3") { viewModel.onDigit('3') }
            CalcButton("\u2212", style = CalcButtonStyle.OPERATOR) { viewModel.onOperator('\u2212') }
        }
        Row(Modifier.fillMaxWidth()) {
            CalcButton("0") { viewModel.onDigit('0') }
            CalcButton(".") { viewModel.onDecimal() }
            CalcButton("%", style = CalcButtonStyle.FUNCTION) { viewModel.onPercent() }
            CalcButton("+", style = CalcButtonStyle.OPERATOR) { viewModel.onOperator('+') }
        }
        Row(Modifier.fillMaxWidth()) {
            CalcButton(
                label = "=",
                style = CalcButtonStyle.EQUALS,
                weight = 4f
            ) { viewModel.onEquals() }
        }
    }
}

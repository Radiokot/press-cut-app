/* Copyright 2026 Oleg Koretsky

   This file is part of the Press-Cut,
   a digital postage stamp cutter Android app.

   Press-Cut is free software: you can redistribute it
   and/or modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation, either version 3 of the License,
   or (at your option) any later version.

   Press-Cut is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
   See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Press-Cut. If not, see <http://www.gnu.org/licenses/>.
*/

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily

@Composable
fun CaptionInput(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember(::FocusRequester),
    isEnabled: Boolean = true,
    hint: String = "A caption",
    inputState: TextFieldState,
) = Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
) {
    val colors = LocalColors.current
    val hintStyle = remember(colors) {
        TextStyle(
            fontFamily = PodkovaFamily,
            fontSize = 24.sp,
            color = colors.textInputHint,
            textAlign = TextAlign.Center,
        )
    }
    val inputStyle = remember {
        hintStyle.copy(
            color = colors.textPrimary,
        )
    }
    val focusManager = LocalFocusManager.current

    val isCaptionHintVisible by remember(isEnabled) {
        derivedStateOf {
            isEnabled && inputState.text.isEmpty()
        }
    }

    if (isCaptionHintVisible) {
        BasicText(
            text = hint,
            style = hintStyle,
            modifier = Modifier
                .clickable(
                    enabled = isEnabled,
                    onClick = {
                        focusRequester.requestFocus()
                    }
                )
        )
    }

    BasicTextField(
        state = inputState,
        textStyle = inputStyle,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            showKeyboardOnFocus = true,
            imeAction = ImeAction.Done,
        ),
        lineLimits = TextFieldLineLimits.SingleLine,
        onKeyboardAction = {
            focusManager.clearFocus()
        },
        cursorBrush = SolidColor(colors.textInputCursor),
        enabled = isEnabled,
        readOnly = !isEnabled,
        modifier = Modifier
            .focusRequester(focusRequester)
    )
}

@Preview
@Composable
private fun CaptionInputPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        CaptionInput(
            inputState = TextFieldState("")
        )

        CaptionInput(
            inputState = TextFieldState("My stamp")
        )
    }
}

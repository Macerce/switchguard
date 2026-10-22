package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// Masaüstü düğmeleri: Material 3'ün telefon düğmeleriyle aynı imza (çağıran yalnızca içe aktarmayı değiştirir), ama
// Windows 11 / Fluent ölçüsü: 4 dp köşe (hap değil), 32 dp yükseklik, dar iç boşluk, gölgesiz. Ekranlar
// `androidx.compose.material3.Button` yerine bunları kullanır.

private val FluentShape = RoundedCornerShape(4.dp)
private val FluentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp)
private val TextPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
private val MinHeight = 32.dp

/** Vurgulu (birincil) düğme: marka renginde dolu. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = FluentShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = FluentPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.Button(
    onClick, modifier.defaultMinSize(minHeight = MinHeight), enabled, shape, colors,
    ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp), border, contentPadding, content = content,
)

/** Standart (ikincil) düğme: kart renginde, ince kenarlıklı. Fluent'ın varsayılan düğmesi. */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = FluentShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = raisedOnTile(),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    border: BorderStroke? = BorderStroke(1.dp, cardStroke()),
    contentPadding: PaddingValues = FluentPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.FilledTonalButton(
    onClick, modifier.defaultMinSize(minHeight = MinHeight), enabled, shape, colors,
    ButtonDefaults.filledTonalButtonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp), border, contentPadding, content = content,
)

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = FluentShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    contentPadding: PaddingValues = FluentPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(
    onClick, modifier.defaultMinSize(minHeight = MinHeight), enabled, shape, colors, null, border, contentPadding, content = content,
)

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = FluentShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = TextPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.TextButton(
    onClick, modifier.defaultMinSize(minHeight = MinHeight), enabled, shape, colors, null, border, contentPadding, content = content,
)

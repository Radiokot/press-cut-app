package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ua.com.radiokot.camerapp.ui.podkovaFamily

@Composable
fun SelectMoveDestinationCollectionDialog(
    collections: ImmutableList<String>,
    onCollectionSelected: (Int) -> Unit,
    onNewCollectionAction: () -> Unit,
    onCancel: () -> Unit,
) = Dialog(
    onDismissRequest = onCancel,
) {
    val actionTextStyle = remember {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = podkovaFamily,
            fontWeight = FontWeight.Bold,
        )
    }
    val collectionTextStyle = remember {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = podkovaFamily,
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFfff9eb),
                shape = RoundedCornerShape(10.dp),
            )
    ) {
        BasicText(
            text = "Which collection to move the stamps to?",
            style = actionTextStyle.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 48.dp,
                )
        )

        LazyColumn(
            modifier = Modifier
                .weight(
                    weight = 1f,
                    fill = false,
                )
        ) {
            itemsIndexed(
                items = collections,
            ) { i, collection ->
                Spacer(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFcbc4bb))
                )

                BasicText(
                    text = collection,
                    style = collectionTextStyle,
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                onCollectionSelected(i)
                            },
                        )
                        .padding(
                            vertical = 20.dp,
                        )
                        .fillMaxWidth()
                )
            }
        }

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color(0xFFcbc4bb))
        )

        BasicText(
            text = "Create a new one",
            style = actionTextStyle,
            modifier = Modifier
                .clickable(
                    onClick = onNewCollectionAction,
                )
                .padding(
                    vertical = 20.dp,
                )
                .fillMaxWidth()
        )

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color(0xFFcbc4bb))
        )

        BasicText(
            text = "Cancel",
            style = actionTextStyle,
            modifier = Modifier
                .clickable(
                    onClick = onCancel,
                )
                .padding(
                    vertical = 20.dp,
                )
                .fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun SelectMoveDestinationCollectionDialogPreview(

) {
    SelectMoveDestinationCollectionDialog(
        collections = persistentListOf(
            "Red collection",
            "Outside",
            "Patterns",
        ),
        onCollectionSelected = {},
        onNewCollectionAction = {},
        onCancel = {},
    )
}

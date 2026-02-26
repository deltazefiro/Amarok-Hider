package deltazero.amarok.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import deltazero.amarok.Hider
import deltazero.amarok.R

@Composable
fun MainScreen(
    onSetHideFiles: () -> Unit,
    onSetHideApps: () -> Unit,
    onSettings: () -> Unit,
    onChangeStatus: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.hiderState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Spacer(Modifier.height(60.dp))

            // Title
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 40.sp,
                modifier = Modifier.padding(start = 42.dp)
            )
            Text(
                text = stringResource(R.string.moto),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 44.dp)
            )

            Spacer(Modifier.height(45.dp))

            // Status card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 35.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 35.dp, top = 35.dp, bottom = 35.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (state) {
                                    Hider.State.HIDDEN -> stringResource(R.string.hidden_status)
                                    else -> stringResource(R.string.visible_status)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 30.sp
                            )
                            Text(
                                text = when (state) {
                                    Hider.State.HIDDEN -> stringResource(R.string.hidden_moto)
                                    else -> stringResource(R.string.visible_moto)
                                },
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(Modifier.height(35.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = onChangeStatus,
                                    enabled = state != Hider.State.PROCESSING
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (state == Hider.State.HIDDEN) R.drawable.ic_wolf else R.drawable.ic_paw
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(
                                        stringResource(
                                            if (state == Hider.State.HIDDEN) R.string.unhide else R.string.hide
                                        )
                                    )
                                }
                                AnimatedVisibility(visible = state == Hider.State.PROCESSING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        // Status image (partially clipped on right side like original)
                        Image(
                            painter = painterResource(
                                if (state == Hider.State.HIDDEN) R.drawable.img_status_hidden
                                else R.drawable.img_status_visible
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(150.dp)
                                .offset(x = 55.dp),
                            colorFilter = if (state == Hider.State.HIDDEN)
                                androidx.compose.ui.graphics.ColorFilter.tint(
                                    Color(0xFF1F1F1F),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Modulate
                                )
                            else null
                        )
                    }
                }
            }

            Spacer(Modifier.height(35.dp))

            // Action buttons
            TextButton(
                onClick = onSetHideFiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 50.dp)
            ) {
                Icon(painterResource(R.drawable.ic_folder), contentDescription = null)
                Spacer(Modifier.width(15.dp))
                Text(stringResource(R.string.set_hide_files))
                Spacer(Modifier.weight(1f))
            }

            TextButton(
                onClick = onSetHideApps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 50.dp)
            ) {
                Icon(painterResource(R.drawable.ic_app), contentDescription = null)
                Spacer(Modifier.width(15.dp))
                Text(stringResource(R.string.set_hide_apps))
                Spacer(Modifier.weight(1f))
            }

            TextButton(
                onClick = onSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 50.dp)
            ) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = null)
                Spacer(Modifier.width(15.dp))
                Text(stringResource(R.string.more_settings))
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(42.dp))
        }
    }
}

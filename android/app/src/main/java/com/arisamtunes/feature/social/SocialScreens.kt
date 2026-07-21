package com.arisamtunes.feature.social

import com.arisamtunes.core.design.spacing.AriSamDimensions
import com.arisamtunes.core.design.colors.AriSamPalette
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.social.PublicUserDto
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SocialListShape = RoundedCornerShape(AriSamDimensions.dp24)
private val SocialListInk = AriSamPalette.profileInk
private val SocialListCyan = AriSamPalette.cyanAccent
private val SocialListViolet = AriSamPalette.indigoSoft

@Composable
fun SocialUsersRoute(
    onBack: () -> Unit,
    onUserClick: (PublicUserDto) -> Unit,
    viewModel: SocialUsersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val users = viewModel.users.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionFailed = stringResource(R.string.social_action_failed)
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(viewModel, actionFailed) {
        viewModel.effects.collect { snackbarHostState.showSnackbar(actionFailed) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .48f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AriSamDimensions.dp32 + navigationBarPadding),
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
        ) {
            item(key = "social_graph_header", contentType = "header") {
                SocialGraphHeader(
                    kind = state.title,
                    visibleCount = users.itemCount,
                    onBack = onBack,
                )
            }

            when {
                users.loadState.refresh is LoadState.Loading -> {
                    items(6, key = { "social_user_shimmer_$it" }, contentType = { "shimmer" }) {
                        SocialUserShimmer()
                    }
                }
                users.loadState.refresh is LoadState.Error -> item(
                    key = "social_users_refresh_error",
                    contentType = "state",
                ) {
                    SocialUsersErrorState(users::retry)
                }
                users.itemCount == 0 -> item(key = "social_users_empty", contentType = "state") {
                    SocialUsersEmptyState(state.title)
                }
                else -> {
                    items(
                        count = users.itemCount,
                        key = users.itemKey(PublicUserDto::id),
                        contentType = users.itemContentType { "social_user" },
                    ) { index ->
                        val user = users[index] ?: return@items
                        AnimatedSocialUserRow(
                            index = index,
                            user = user,
                            isUpdating = user.id in state.updatingUserIds,
                            onUserClick = { onUserClick(user) },
                            onFollowClick = { viewModel.toggleFollow(user) },
                        )
                    }
                    when (users.loadState.append) {
                        is LoadState.Loading -> item(key = "social_users_append_loading", contentType = "state") {
                            Box(
                                Modifier.fillMaxWidth().padding(AriSamDimensions.dp20),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(Modifier.size(AriSamDimensions.dp24), strokeWidth = AriSamDimensions.dp2)
                            }
                        }
                        is LoadState.Error -> item(key = "social_users_append_error", contentType = "state") {
                            SocialUsersErrorState(users::retry, compact = true)
                        }
                        is LoadState.NotLoading -> Unit
                    }
                }
            }
        }

        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter).padding(bottom = navigationBarPadding),
        )
    }
}

@Composable
private fun SocialGraphHeader(
    kind: SocialListKind,
    visibleCount: Int,
    onBack: () -> Unit,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val transition = rememberInfiniteTransition(label = "socialGraphHeader")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(6_000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "socialGraphPhase",
    )
    var entered by remember(kind) { mutableStateOf(false) }
    LaunchedEffect(kind) { entered = true }

    Box(
        Modifier
            .fillMaxWidth()
            .height(AriSamDimensions.dp210 + statusBarPadding)
            .clip(RoundedCornerShape(bottomStart = AriSamDimensions.dp38, bottomEnd = AriSamDimensions.dp38))
            .background(
                Brush.linearGradient(
                    listOf(SocialListInk, AriSamPalette.cyan900, AriSamPalette.navyViolet),
                ),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = AriSamDimensions.dp54, y = AriSamDimensions.negative48)
                .size(AriSamDimensions.dp190)
                .blur(AriSamDimensions.dp48)
                .background(SocialListCyan.copy(alpha = .3f), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = AriSamDimensions.negative64, y = AriSamDimensions.dp58)
                .size(AriSamDimensions.dp170)
                .blur(AriSamDimensions.dp50)
                .background(SocialListViolet.copy(alpha = .28f), CircleShape),
        )
        SocialConstellation(phase, Modifier.fillMaxSize())

        Surface(
            onClick = onBack,
            modifier = Modifier
                .padding(start = AriSamDimensions.dp14, top = statusBarPadding + AriSamDimensions.dp12)
                .size(AriSamDimensions.dp46),
            shape = CircleShape,
            color = AriSamPalette.black.copy(alpha = .25f),
            contentColor = AriSamPalette.white,
            border = BorderStroke(AriSamDimensions.dp1, AriSamPalette.white.copy(alpha = .15f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
            }
        }

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(420)) +
                slideInVertically(tween(560, easing = FastOutSlowInEasing)) { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = AriSamDimensions.dp22, end = AriSamDimensions.dp22, bottom = AriSamDimensions.dp24),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp14),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp4)) {
                    Text(
                        stringResource(
                            if (kind == SocialListKind.Followers) R.string.social_followers else R.string.social_following,
                        ),
                        color = AriSamPalette.white,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        stringResource(
                            if (kind == SocialListKind.Followers) {
                                R.string.social_followers_list_subtitle
                            } else {
                                R.string.social_following_list_subtitle
                            },
                        ),
                        color = AriSamPalette.white.copy(alpha = .7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = AriSamPalette.white.copy(alpha = .11f),
                    contentColor = SocialListCyan,
                    border = BorderStroke(AriSamDimensions.dp1, AriSamPalette.white.copy(alpha = .12f)),
                ) {
                    Text(
                        visibleCount.toString(),
                        modifier = Modifier.padding(horizontal = AriSamDimensions.dp14, vertical = AriSamDimensions.dp8),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialConstellation(phase: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val phaseValue = phase.value
        val points = List(7) { index ->
            val baseX = size.width * (.12f + index * .13f)
            val baseY = size.height * (.27f + (index % 3) * .11f)
            androidx.compose.ui.geometry.Offset(
                x = baseX + cos(phaseValue + index) * AriSamDimensions.dp7.toPx(),
                y = baseY + sin(phaseValue + index * .8f) * AriSamDimensions.dp6.toPx(),
            )
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = AriSamPalette.white.copy(alpha = .09f),
                start = start,
                end = end,
                strokeWidth = AriSamDimensions.dp1.toPx(),
                cap = StrokeCap.Round,
            )
        }
        points.forEachIndexed { index, point ->
            drawCircle(
                color = if (index % 2 == 0) SocialListCyan else SocialListViolet,
                radius = if (index % 3 == 0) AriSamDimensions.dp3.toPx() else AriSamDimensions.dp2.toPx(),
                center = point,
                alpha = .4f,
            )
        }
    }
}

@Composable
private fun AnimatedSocialUserRow(
    index: Int,
    user: PublicUserDto,
    isUpdating: Boolean,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit,
) {
    var visible by remember(user.id) { mutableStateOf(false) }
    LaunchedEffect(user.id) {
        delay(index.coerceAtMost(8) * 42L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320)) + slideInVertically(
            animationSpec = spring(dampingRatio = .84f, stiffness = 330f),
            initialOffsetY = { it / 3 },
        ),
        exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 4 },
        modifier = Modifier.padding(horizontal = AriSamThemeTokens.spacing.lg),
    ) {
        SocialUserCard(
            user = user,
            isUpdating = isUpdating,
            onUserClick = onUserClick,
            onFollowClick = onFollowClick,
        )
    }
}

@Composable
private fun SocialUserCard(
    user: PublicUserDto,
    isUpdating: Boolean,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit,
) {
    Surface(
        shape = SocialListShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .74f)),
        shadowElevation = AriSamDimensions.dp2,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp12, vertical = AriSamDimensions.dp11),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
        ) {
            PressScaleBox(
                onClick = onUserClick,
                modifier = Modifier.weight(1f).semantics { role = Role.Button },
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12),
                ) {
                    SocialUserAvatar(user)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp3)) {
                        Text(
                            user.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(R.string.social_followers_count, user.followersCount),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
            FollowStateButton(
                isFollowing = user.isFollowing,
                isUpdating = isUpdating,
                onClick = onFollowClick,
            )
        }
    }
}

@Composable
private fun SocialUserAvatar(user: PublicUserDto) {
    Box(Modifier.size(AriSamDimensions.dp58), contentAlignment = Alignment.Center) {
        if (user.isFollowing) {
            Box(
                Modifier
                    .size(AriSamDimensions.dp58)
                    .border(AriSamDimensions.dp1_5, MaterialTheme.colorScheme.primary.copy(alpha = .55f), CircleShape),
            )
        }
        val avatarModifier = Modifier
            .size(AriSamDimensions.dp50)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SocialListViolet, SocialListCyan)))
        if (user.avatarUrl.isNullOrBlank()) {
            Box(avatarModifier, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, tint = AriSamPalette.white, modifier = Modifier.size(AriSamDimensions.dp26))
            }
        } else {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.displayName,
                contentScale = ContentScale.Crop,
                modifier = avatarModifier,
            )
        }
    }
}

@Composable
private fun FollowStateButton(
    isFollowing: Boolean,
    isUpdating: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !isUpdating,
        modifier = Modifier.height(AriSamDimensions.dp42),
        shape = CircleShape,
        color = if (isFollowing) {
            MaterialTheme.colorScheme.primary.copy(alpha = .12f)
        } else {
            MaterialTheme.colorScheme.primary
        },
        contentColor = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
        border = if (isFollowing) BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.primary.copy(alpha = .34f)) else null,
    ) {
        Box(
            Modifier.padding(horizontal = AriSamDimensions.dp14),
            contentAlignment = Alignment.Center,
        ) {
            if (isUpdating) {
                CircularProgressIndicator(
                    Modifier.size(AriSamDimensions.dp18),
                    color = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = AriSamDimensions.dp2,
                )
            } else {
                AnimatedContent(
                    targetState = isFollowing,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(spring(stiffness = 520f), initialScale = .78f)) togetherWith
                            (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = .78f))
                    },
                    label = "socialUserFollowState",
                ) { following ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp5),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (following) Icon(Icons.Rounded.Check, null, Modifier.size(AriSamDimensions.dp16))
                        Text(
                            stringResource(if (following) R.string.social_following_badge else R.string.social_follow),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialUserShimmer() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp16, vertical = AriSamDimensions.dp3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12),
    ) {
        ShimmerBox(Modifier.size(AriSamDimensions.dp54).clip(CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8)) {
            ShimmerBox(Modifier.fillMaxWidth(.56f).height(AriSamDimensions.dp17))
            ShimmerBox(Modifier.fillMaxWidth(.34f).height(AriSamDimensions.dp12))
        }
        ShimmerBox(Modifier.width(AriSamDimensions.dp88).height(AriSamDimensions.dp40))
    }
}

@Composable
private fun SocialUsersErrorState(onRetry: () -> Unit, compact: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp16, vertical = if (compact) AriSamDimensions.dp2 else AriSamDimensions.dp18),
        shape = SocialListShape,
        color = MaterialTheme.colorScheme.error.copy(alpha = .08f),
        contentColor = MaterialTheme.colorScheme.error,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (compact) AriSamDimensions.dp12 else AriSamDimensions.dp20),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12),
        ) {
            Icon(Icons.Rounded.Groups, null, Modifier.size(if (compact) AriSamDimensions.dp24 else AriSamDimensions.dp34))
            Text(
                stringResource(R.string.social_action_failed),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(AriSamDimensions.dp17))
                Spacer(Modifier.width(AriSamDimensions.dp6))
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun SocialUsersEmptyState(kind: SocialListKind) {
    var visible by remember(kind) { mutableStateOf(false) }
    LaunchedEffect(kind) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(360)) + scaleIn(spring(dampingRatio = .76f), initialScale = .84f),
        modifier = Modifier.fillMaxWidth().padding(AriSamDimensions.dp28),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
        ) {
            Surface(
                modifier = Modifier.size(AriSamDimensions.dp76),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .11f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Groups, null, Modifier.size(AriSamDimensions.dp38))
                }
            }
            Text(
                stringResource(
                    if (kind == SocialListKind.Followers) R.string.social_no_followers_title
                    else R.string.social_no_following_title,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.social_users_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

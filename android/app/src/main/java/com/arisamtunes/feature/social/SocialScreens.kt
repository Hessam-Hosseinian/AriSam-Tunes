package com.arisamtunes.feature.social

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

private val SocialListShape = RoundedCornerShape(24.dp)
private val SocialListInk = Color(0xFF06131D)
private val SocialListCyan = Color(0xFF38C6F4)
private val SocialListViolet = Color(0xFF7C6CF2)

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
            contentPadding = PaddingValues(bottom = 32.dp + navigationBarPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
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
            .height(210.dp + statusBarPadding)
            .clip(RoundedCornerShape(bottomStart = 38.dp, bottomEnd = 38.dp))
            .background(
                Brush.linearGradient(
                    listOf(SocialListInk, Color(0xFF0A3345), Color(0xFF171E46)),
                ),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 54.dp, y = (-48).dp)
                .size(190.dp)
                .blur(48.dp)
                .background(SocialListCyan.copy(alpha = .3f), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-64).dp, y = 58.dp)
                .size(170.dp)
                .blur(50.dp)
                .background(SocialListViolet.copy(alpha = .28f), CircleShape),
        )
        SocialConstellation(phase, Modifier.fillMaxSize())

        Surface(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 14.dp, top = statusBarPadding + 12.dp)
                .size(46.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = .25f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .15f)),
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
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(
                            if (kind == SocialListKind.Followers) R.string.social_followers else R.string.social_following,
                        ),
                        color = Color.White,
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
                        color = Color.White.copy(alpha = .7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .11f),
                    contentColor = SocialListCyan,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .12f)),
                ) {
                    Text(
                        visibleCount.toString(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                x = baseX + cos(phaseValue + index) * 7.dp.toPx(),
                y = baseY + sin(phaseValue + index * .8f) * 6.dp.toPx(),
            )
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = Color.White.copy(alpha = .09f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        points.forEachIndexed { index, point ->
            drawCircle(
                color = if (index % 2 == 0) SocialListCyan else SocialListViolet,
                radius = if (index % 3 == 0) 3.dp.toPx() else 2.dp.toPx(),
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .74f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PressScaleBox(
                onClick = onUserClick,
                modifier = Modifier.weight(1f).semantics { role = Role.Button },
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SocialUserAvatar(user)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
        if (user.isFollowing) {
            Box(
                Modifier
                    .size(58.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f), CircleShape),
            )
        }
        val avatarModifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SocialListViolet, SocialListCyan)))
        if (user.avatarUrl.isNullOrBlank()) {
            Box(avatarModifier, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(26.dp))
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
        modifier = Modifier.height(42.dp),
        shape = CircleShape,
        color = if (isFollowing) {
            MaterialTheme.colorScheme.primary.copy(alpha = .12f)
        } else {
            MaterialTheme.colorScheme.primary
        },
        contentColor = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
        border = if (isFollowing) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .34f)) else null,
    ) {
        Box(
            Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isUpdating) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    color = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
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
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (following) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
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
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerBox(Modifier.size(54.dp).clip(CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(Modifier.fillMaxWidth(.56f).height(17.dp))
            ShimmerBox(Modifier.fillMaxWidth(.34f).height(12.dp))
        }
        ShimmerBox(Modifier.width(88.dp).height(40.dp))
    }
}

@Composable
private fun SocialUsersErrorState(onRetry: () -> Unit, compact: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 2.dp else 18.dp),
        shape = SocialListShape,
        color = MaterialTheme.colorScheme.error.copy(alpha = .08f),
        contentColor = MaterialTheme.colorScheme.error,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (compact) 12.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Groups, null, Modifier.size(if (compact) 24.dp else 34.dp))
            Text(
                stringResource(R.string.social_action_failed),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
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
        modifier = Modifier.fillMaxWidth().padding(28.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .11f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Groups, null, Modifier.size(38.dp))
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

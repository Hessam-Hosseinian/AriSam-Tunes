package com.arisamtunes.feature.chat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.arisamtunes.MainActivity
import com.arisamtunes.R
import com.arisamtunes.data.chat.ChatMessageDto
import com.arisamtunes.data.chat.ChatMessageTypeDto
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

private const val MessageChannelId = "chat_messages"
private const val RemoteInputKey = "chat_inline_reply"
private const val ExtraPeerId = "chat_peer_id"
private const val ReplyAction = "com.arisamtunes.action.CHAT_INLINE_REPLY"

@Singleton
class ChatNotificationManager @Inject constructor(@ApplicationContext private val context: Context) {
    init { createChannel() }

    fun show(message: ChatMessageDto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val peerId = message.senderId
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("arisamtunes://chat/$peerId")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, peerId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val replyIntent = Intent(context, ChatReplyReceiver::class.java).apply {
            action = ReplyAction
            data = Uri.parse("arisamtunes://chat-reply/$peerId")
            setPackage(context.packageName)
            putExtra(ExtraPeerId, peerId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context, peerId.hashCode(), replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val remoteInput = RemoteInput.Builder(RemoteInputKey).setLabel(context.getString(R.string.chat_notification_reply_hint)).build()
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification_message,
            context.getString(R.string.chat_reply),
            replyPendingIntent,
        ).addRemoteInput(remoteInput).setAllowGeneratedReplies(true).build()
        val body = if (message.messageType == ChatMessageTypeDto.SONG) context.getString(R.string.chat_shared_song) else message.content.orEmpty()
        val notification = NotificationCompat.Builder(context, MessageChannelId)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setContentTitle(context.getString(R.string.chat_notification_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup("chat:$peerId")
            .setContentIntent(contentIntent)
            .addAction(replyAction)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(message.id.hashCode(), notification) }
    }

    fun showReplyQueueFailure(peerId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, MessageChannelId)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setContentTitle(context.getString(R.string.chat_notification_title))
            .setContentText(context.getString(R.string.chat_inline_reply_failed))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(peerId.hashCode() xor -1, notification) }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            MessageChannelId,
            context.getString(R.string.chat_notification_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.chat_notification_channel_description)
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

@AndroidEntryPoint
class ChatReplyReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: com.arisamtunes.data.chat.ChatRepository
    @Inject lateinit var notificationManager: ChatNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReplyAction) return
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(RemoteInputKey)
            ?.toString()
            ?.trim()
            .orEmpty()
            .take(4_000)
        val peerId = intent.getStringExtra(ExtraPeerId).orEmpty()
        val validPeerId = runCatching { UUID.fromString(peerId).toString() }.getOrNull() ?: return
        if (text.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // sendText writes to the durable local outbox before attempting the
                // socket. The normal app-level realtime owner flushes it on connect.
                repository.sendText(validPeerId, text)
            } catch (error: Throwable) {
                Log.e("AriSamChatReply", "Unable to queue inline reply", error)
                notificationManager.showReplyQueueFailure(validPeerId)
            } finally {
                pending.finish()
            }
        }
    }
}

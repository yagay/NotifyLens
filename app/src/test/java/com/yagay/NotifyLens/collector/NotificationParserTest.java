package com.yagay.NotifyLens.collector;

import android.app.Application;
import android.app.Notification;
import android.app.Person;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;

import com.yagay.NotifyLens.data.EventRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NotificationParserTest {
    private final Application context = ApplicationProvider.getApplicationContext();

    @Test
    public void readsBigText() {
        Notification n = new Notification.Builder(context, "test")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Title")
                .setContentText("Short")
                .setStyle(new Notification.BigTextStyle().bigText("This is the complete body"))
                .build();
        EventRecord r = NotificationParser.parse(context, sbn(n, 1000L));
        assertEquals("Title", r.title);
        assertTrue(r.fullText.contains("This is the complete body"));
    }

    @Test
    public void readsMessagingStyle() {
        Person me = new Person.Builder().setName("Me").build();
        Person alice = new Person.Builder().setName("Alice").build();
        Notification n = new Notification.Builder(context, "chat")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setStyle(new Notification.MessagingStyle(me)
                        .setConversationTitle("Chat")
                        .addMessage("Hello", 1L, alice))
                .build();
        EventRecord r = NotificationParser.parse(context, sbn(n, 2000L));
        assertTrue(r.messagesJson.contains("Hello"));
        assertTrue(r.fullText.contains("Alice"));
    }

    private StatusBarNotification sbn(Notification n, long when) {
        return new StatusBarNotification(
                "com.example.test", "com.example.test", 7, null,
                10001, 1234, 0, n, android.os.Process.myUserHandle(), when);
    }
}

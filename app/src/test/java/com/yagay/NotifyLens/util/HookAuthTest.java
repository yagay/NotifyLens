package com.yagay.NotifyLens.util;

import android.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class HookAuthTest {
    @Test
    public void verifiesAndRejectsTampering() {
        byte[] raw = new byte[32];
        Arrays.fill(raw, (byte) 7);
        String secret = Base64.encodeToString(raw, Base64.NO_WRAP);
        String signature = HookAuth.sign(secret, "com.test", "ui", "toast", "hello", "Toast", "", 123L, "nonce");
        assertTrue(HookAuth.verify(secret, signature, "com.test", "ui", "toast", "hello", "Toast", "", 123L, "nonce"));
        assertFalse(HookAuth.verify(secret, signature, "com.test", "ui", "toast", "changed", "Toast", "", 123L, "nonce"));
    }
}

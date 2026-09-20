package com.yagay.NotifyLens.util;

import com.yagay.NotifyLens.data.EventRecord;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ContentHasherTest {
    @Test
    public void stableForSameContent() {
        EventRecord a = new EventRecord();
        a.title = "A";
        a.fullText = "B";
        EventRecord b = new EventRecord();
        b.title = "A";
        b.fullText = "B";
        assertEquals(ContentHasher.hash(a), ContentHasher.hash(b));
    }

    @Test
    public void changesForProgressRevision() {
        EventRecord a = new EventRecord();
        a.fullText = "Download";
        a.progress = 10;
        a.progressMax = 100;
        EventRecord b = new EventRecord();
        b.fullText = "Download";
        b.progress = 20;
        b.progressMax = 100;
        assertNotEquals(ContentHasher.hash(a), ContentHasher.hash(b));
    }
}

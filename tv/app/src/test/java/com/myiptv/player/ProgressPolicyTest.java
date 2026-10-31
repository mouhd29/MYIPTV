package com.myiptv.player;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProgressPolicyTest {
    @Test public void movieAndEpisodeCanResumeUntilCompletion(){
        assertFalse(ProgressPolicy.resumable(9000,3600000,false));
        assertTrue(ProgressPolicy.resumable(900000,3600000,false));
        assertFalse(ProgressPolicy.completed(3420000,3600000,false)); // 95% is still 3 minutes from the end
        assertTrue(ProgressPolicy.completed(3560000,3600000,false));
        assertFalse(ProgressPolicy.resumable(3560000,3600000,true));
        assertTrue(ProgressPolicy.completed(15000,-1,true));
        assertFalse(ProgressPolicy.completed(15000,-1,false));
    }
    @Test public void identicalStreamIdsDoNotShareProgressAcrossPlaylists(){
        assertNotEquals(ProgressPolicy.key("account-a","series:5"),ProgressPolicy.key("account-b","series:5"));
        assertEquals(ProgressPolicy.key("account-a","https://example.org/movie/1"),ProgressPolicy.key("account-a","https://example.org/movie/1"));
    }
    @Test public void seekingBackAndUnknownDurationsRemainResumable(){
        assertTrue(ProgressPolicy.resumable(360000,-1,false));
        assertFalse(ProgressPolicy.resumable(360000,360000,false));
        assertEquals("1:01:01",ProgressPolicy.time(3661000));
    }
}

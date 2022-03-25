package commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JokerTest {
    @Test
    public void testEmptyConstructor() {
        Joker j = new Joker();
        assertNull(j.username);
        assertEquals("", j.jokerName);
    }

    @Test
    public void testConstructor() {
        Joker j = new Joker("test", "testJoker");
        assertEquals("test", j.username);
        assertEquals("testJoker", j.jokerName);
    }

    @Test
    public void testEquals() {
        Joker j1 = new Joker("test1", "testJoker1");
        Joker j2 = new Joker("test1", "testJoker1");
        Joker j3 = new Joker("test2", "testJoker2");

        assertEquals(j1, j1);
        assertEquals(j1, j2);
        assertNotEquals(j3, j1);
    }

    @Test
    public void testHashCode() {
        Joker j1 = new Joker("test1", "testJoker1");
        Joker j2 = new Joker("test1", "testJoker1");

        assertEquals(j1.hashCode(), j1.hashCode());
        assertEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    public void testToString() {
        Joker j1 = new Joker("test1", "testJoker1");
        String resultString = j1.toString();

        assertTrue(resultString.contains(Joker.class.getSimpleName()));
        assertTrue(resultString.contains("username=test1"));
        assertTrue(resultString.contains("jokerName=testJoker1"));
    }
}
